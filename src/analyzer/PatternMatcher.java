package analyzer;

import minecraft.ChunkReader;
import minecraft.RegionFile;
import minecraft.NBTTag;
import models.BlockData;
import models.BlockPattern;
import models.SearchResult;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minecraft dunya dosyalarinda cok-is-parcacikli blok deseni arama motoru.
 */
public class PatternMatcher {

    /**
     * Arama ilerleme ve durum geri bildirimi icin arayuz.
     */
    public interface ProgressListener {
        void onProgress(int current, int total, String message);
        void onResult(SearchResult result);
        void onComplete(int totalResults, long elapsedMs);
        void onError(String error);
    }

    private final File worldDirectory;
    private final BlockPattern pattern;
    private final int startX, endX, startZ, endZ, minY, maxY;
    private final ProgressListener listener;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final List<SearchResult> results = Collections.synchronizedList(new ArrayList<>());

    public PatternMatcher(File worldDirectory, BlockPattern pattern,
                          int startX, int endX, int startZ, int endZ,
                          int minY, int maxY, ProgressListener listener) {
        this.worldDirectory = worldDirectory;
        this.pattern = pattern;
        this.startX = startX;
        this.endX = endX;
        this.startZ = startZ;
        this.endZ = endZ;
        this.minY = minY;
        this.maxY = maxY;
        this.listener = listener;
    }

    /**
     * Aramayi iptal eder.
     */
    public void cancel() {
        cancelled.set(true);
    }

    /**
     * Aramayi baslatir. Chunk bazli paralel arama yapar.
     */
    public List<SearchResult> search() {
        long startTime = System.currentTimeMillis();

        // Hangi chunk'larin taranmasi gerektigini hesapla
        int startChunkX = RegionFile.blockToChunk(startX);
        int endChunkX = RegionFile.blockToChunk(endX);
        int startChunkZ = RegionFile.blockToChunk(startZ);
        int endChunkZ = RegionFile.blockToChunk(endZ);

        // Taranacak chunk'lari topla
        List<int[]> chunksToScan = new ArrayList<>();
        for (int cx = startChunkX; cx <= endChunkX; cx++) {
            for (int cz = startChunkZ; cz <= endChunkZ; cz++) {
                chunksToScan.add(new int[]{cx, cz});
            }
        }

        int totalChunks = chunksToScan.size();
        listener.onProgress(0, totalChunks, "Arama baslatiliyor... " + totalChunks + " chunk taranacak");

        // Chunk'lari region dosyalarina gore grupla
        Map<String, List<int[]>> regionGroups = new LinkedHashMap<>();
        for (int[] chunk : chunksToScan) {
            int rx = RegionFile.chunkToRegion(chunk[0]);
            int rz = RegionFile.chunkToRegion(chunk[1]);
            String key = rx + "," + rz;
            regionGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(chunk);
        }

        listener.onProgress(0, totalChunks, regionGroups.size() + " region dosyasi taranacak");

        // Multi-threaded arama
        int threadCount = Math.min(Runtime.getRuntime().availableProcessors(), regionGroups.size());
        threadCount = Math.max(1, threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger processedChunks = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (Map.Entry<String, List<int[]>> entry : regionGroups.entrySet()) {
            String[] coords = entry.getKey().split(",");
            int rx = Integer.parseInt(coords[0]);
            int rz = Integer.parseInt(coords[1]);
            List<int[]> chunks = entry.getValue();

            futures.add(executor.submit(() -> {
                if (cancelled.get()) return;
                scanRegion(rx, rz, chunks, processedChunks, totalChunks);
            }));
        }

        // Tum islemlerin bitmesini bekle
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                listener.onError("Arama hatasi: " + e.getMessage());
            }
        }

        executor.shutdown();

        long elapsed = System.currentTimeMillis() - startTime;
        listener.onComplete(results.size(), elapsed);

        return new ArrayList<>(results);
    }

    /**
     * Tek bir region dosyasindaki chunk'lari tarar.
     */
    private void scanRegion(int regionX, int regionZ, List<int[]> chunks,
                            AtomicInteger processedChunks, int totalChunks) {
        // Region dosyasini bul
        File regionDir = new File(worldDirectory, "region");
        File regionFile = new File(regionDir, String.format("r.%d.%d.mca", regionX, regionZ));

        if (!regionFile.exists()) {
            // Region dosyasi yok, bu chunk'lar mevcut degil
            int done = processedChunks.addAndGet(chunks.size());
            listener.onProgress(done, totalChunks,
                    String.format("Region r.%d.%d.mca bulunamadi, atlaniyor...", regionX, regionZ));
            return;
        }

        RegionFile region;
        try {
            region = new RegionFile(regionFile);
        } catch (Exception e) {
            int done = processedChunks.addAndGet(chunks.size());
            listener.onError(String.format("Region r.%d.%d.mca okunamadi: %s", regionX, regionZ, e.getMessage()));
            return;
        }

        // Bu region'daki her chunk'i tara
        for (int[] chunk : chunks) {
            if (cancelled.get()) return;

            int cx = chunk[0];
            int cz = chunk[1];
            int localX = RegionFile.toLocal(cx);
            int localZ = RegionFile.toLocal(cz);

            if (region.hasChunk(localX, localZ)) {
                try {
                    NBTTag chunkNBT = region.readChunkData(localX, localZ);
                    if (chunkNBT != null) {
                        scanChunk(new ChunkReader(chunkNBT), cx, cz);
                    }
                } catch (Exception e) {
                    // Chunk okuma hatasi - atla ve devam et
                    listener.onError(String.format("Chunk (%d, %d) okunamadi: %s", cx, cz, e.getMessage()));
                }
            }

            int done = processedChunks.incrementAndGet();
            if (done % 10 == 0 || done == totalChunks) {
                listener.onProgress(done, totalChunks,
                        String.format("Taraniyor... %d/%d chunk (%%%.0f) - %d eslesme",
                                done, totalChunks, done * 100.0 / totalChunks, results.size()));
            }
        }
    }

    /**
     * Tek bir chunk'ta pattern arar.
     */
    private void scanChunk(ChunkReader reader, int chunkX, int chunkZ) {
        // Bu chunk'in blok koordinat araligi
        int blockStartX = chunkX * 16;
        int blockStartZ = chunkZ * 16;
        int blockEndX = blockStartX + 15;
        int blockEndZ = blockStartZ + 15;

        // Arama alanini chunk sinirlarina kisitla
        int scanStartX = Math.max(startX, blockStartX);
        int scanEndX = Math.min(endX, blockEndX);
        int scanStartZ = Math.max(startZ, blockStartZ);
        int scanEndZ = Math.min(endZ, blockEndZ);

        // Pattern boyutu kadar yer birakmak lazim
        int patternSize = BlockPattern.SIZE;

        for (int x = scanStartX; x <= scanEndX - patternSize + 1; x++) {
            for (int z = scanStartZ; z <= scanEndZ - patternSize + 1; z++) {
                for (int y = minY; y <= maxY - patternSize + 1; y++) {
                    if (cancelled.get()) return;

                    MatchResult match = matchPatternAt(reader, x, y, z);
                    if (match.matched) {
                        SearchResult result = new SearchResult(x, y, z, match.matchCount, match.totalBlocks);
                        results.add(result);
                        listener.onResult(result);
                    }
                }
            }
        }
    }

    /**
     * Belirli bir konumda pattern'in eslesip eslesmedigini kontrol eder.
     */
    private MatchResult matchPatternAt(ChunkReader reader, int worldX, int worldY, int worldZ) {
        int matchCount = 0;
        int totalDefinedBlocks = 0;

        for (int px = 0; px < BlockPattern.SIZE; px++) {
            for (int py = 0; py < BlockPattern.SIZE; py++) {
                for (int pz = 0; pz < BlockPattern.SIZE; pz++) {
                    BlockData expected = pattern.getBlock(px, py, pz);
                    if (expected == null) continue; // Tanimlanmamis pozisyon, herhangi bir blok olabilir

                    totalDefinedBlocks++;
                    String actual = reader.getBlockAt(worldX + px, worldY + py, worldZ + pz);

                    if (actual != null && actual.equals(expected.getBlockId())) {
                        matchCount++;
                    } else if (expected.isAir() && (actual == null
                            || "minecraft:air".equals(actual)
                            || "minecraft:cave_air".equals(actual)
                            || "minecraft:void_air".equals(actual))) {
                        matchCount++;
                    } else {
                        // Eslesmedi - erken cikis
                        return new MatchResult(false, matchCount, totalDefinedBlocks);
                    }
                }
            }
        }

        // Tum tanimli bloklar eslesti
        return new MatchResult(totalDefinedBlocks > 0, matchCount, totalDefinedBlocks);
    }

    private static class MatchResult {
        final boolean matched;
        final int matchCount;
        final int totalBlocks;

        MatchResult(boolean matched, int matchCount, int totalBlocks) {
            this.matched = matched;
            this.matchCount = matchCount;
            this.totalBlocks = totalBlocks;
        }
    }
}
