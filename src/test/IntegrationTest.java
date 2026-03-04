package test;

import minecraft.NBTTag;
import minecraft.NBTReader;
import minecraft.RegionFile;
import minecraft.ChunkReader;
import models.BlockData;
import models.BlockPattern;
import models.SearchResult;
import analyzer.PatternMatcher;

import java.io.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;

/**
 * Tum pipeline'i test eder:
 * NBT yazma -> Region dosyasi olusturma -> NBT okuma -> Chunk parse -> Pattern matching
 */
public class IntegrationTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Minecraft Seed Analyzer - Entegrasyon Testi ===\n");

        testNBTReadWrite();
        testRegionFileReadWrite();
        testChunkReader();
        testPatternMatcher();

        System.out.println("\n=== SONUC ===");
        System.out.println("Basarili: " + passed);
        System.out.println("Basarisiz: " + failed);
        System.out.println(failed == 0 ? "TUM TESTLER GECTI!" : "BAZI TESTLER BASARISIZ!");

        System.exit(failed > 0 ? 1 : 0);
    }

    // ============ NBT Okuma/Yazma Testi ============

    static void testNBTReadWrite() throws Exception {
        System.out.println("--- NBT Okuma/Yazma Testi ---");

        // Bir NBT compound olustur, yaz, geri oku
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Root compound tag
        dos.writeByte(NBTTag.TAG_COMPOUND); // type
        writeString(dos, ""); // root name

        // TAG_Int: "TestInt" = 42
        dos.writeByte(NBTTag.TAG_INT);
        writeString(dos, "TestInt");
        dos.writeInt(42);

        // TAG_String: "TestStr" = "merhaba"
        dos.writeByte(NBTTag.TAG_STRING);
        writeString(dos, "TestStr");
        writeString(dos, "merhaba");

        // TAG_Long: "TestLong" = 123456789L
        dos.writeByte(NBTTag.TAG_LONG);
        writeString(dos, "TestLong");
        dos.writeLong(123456789L);

        // TAG_List of compounds (palette simulasyonu)
        dos.writeByte(NBTTag.TAG_LIST);
        writeString(dos, "Palette");
        dos.writeByte(NBTTag.TAG_COMPOUND); // list element type
        dos.writeInt(2); // list length

        // Palette[0]: {Name: "minecraft:stone"}
        dos.writeByte(NBTTag.TAG_STRING);
        writeString(dos, "Name");
        writeString(dos, "minecraft:stone");
        dos.writeByte(NBTTag.TAG_END);

        // Palette[1]: {Name: "minecraft:dirt"}
        dos.writeByte(NBTTag.TAG_STRING);
        writeString(dos, "Name");
        writeString(dos, "minecraft:dirt");
        dos.writeByte(NBTTag.TAG_END);

        // TAG_Long_Array: "BlockStates"
        dos.writeByte(NBTTag.TAG_LONG_ARRAY);
        writeString(dos, "BlockStates");
        dos.writeInt(2); // array length
        dos.writeLong(0b0101010101010101L);
        dos.writeLong(0b1010101010101010L);

        // Root compound end
        dos.writeByte(NBTTag.TAG_END);

        dos.flush();
        byte[] data = baos.toByteArray();

        // Oku
        NBTTag root = NBTReader.readUncompressed(data);
        assertNotNull("Root tag", root);
        assertEquals("Root type", NBTTag.TAG_COMPOUND, root.getType());

        NBTTag testInt = root.getCompound("TestInt");
        assertNotNull("TestInt tag", testInt);
        assertEquals("TestInt value", 42, testInt.getInt());

        NBTTag testStr = root.getCompound("TestStr");
        assertNotNull("TestStr tag", testStr);
        assertEquals("TestStr value", "merhaba", testStr.getString());

        NBTTag testLong = root.getCompound("TestLong");
        assertNotNull("TestLong tag", testLong);
        assertEquals("TestLong value", 123456789L, testLong.getLong());

        NBTTag palette = root.getCompound("Palette");
        assertNotNull("Palette tag", palette);
        assertEquals("Palette size", 2, palette.getListSize());

        NBTTag entry0 = palette.getListItem(0);
        NBTTag name0 = entry0.getCompound("Name");
        assertEquals("Palette[0] name", "minecraft:stone", name0.getString());

        NBTTag entry1 = palette.getListItem(1);
        NBTTag name1 = entry1.getCompound("Name");
        assertEquals("Palette[1] name", "minecraft:dirt", name1.getString());

        NBTTag blockStates = root.getCompound("BlockStates");
        assertNotNull("BlockStates tag", blockStates);
        long[] longArr = blockStates.getLongArray();
        assertEquals("BlockStates length", 2, longArr.length);

        System.out.println();
    }

    // ============ Region Dosyasi Testi ============

    static void testRegionFileReadWrite() throws Exception {
        System.out.println("--- Region Dosyasi Okuma Testi ---");

        // Minimal bir .mca dosyasi olustur
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "mc_test_" + System.currentTimeMillis());
        File regionDir = new File(tempDir, "region");
        regionDir.mkdirs();

        File mcaFile = new File(regionDir, "r.0.0.mca");

        // Basit bir chunk NBT verisi olustur
        byte[] chunkNBT = createMinimalChunkNBT(0, 0, 3700); // DataVersion 3700 = 1.20.x

        // Zlib ile sikistir
        ByteArrayOutputStream compressedBaos = new ByteArrayOutputStream();
        DeflaterOutputStream deflater = new DeflaterOutputStream(compressedBaos);
        deflater.write(chunkNBT);
        deflater.finish();
        deflater.close();
        byte[] compressed = compressedBaos.toByteArray();

        // .mca dosyasi yaz
        try (RandomAccessFile raf = new RandomAccessFile(mcaFile, "rw")) {
            // Location table: chunk (0,0) at sector 2 (offset 8192)
            // index = 0 + 0 * 32 = 0
            int sectorOffset = 2; // sektorler 4KB
            int sectorCount = (compressed.length + 5 + 4095) / 4096;
            int locationEntry = (sectorOffset << 8) | (sectorCount & 0xFF);

            raf.writeInt(locationEntry);

            // Geri kalan location table (1023 bos giris)
            for (int i = 1; i < 1024; i++) {
                raf.writeInt(0);
            }

            // Timestamp table (1024 giris)
            for (int i = 0; i < 1024; i++) {
                raf.writeInt(0);
            }

            // Chunk verisi (offset 8192 = sector 2)
            raf.seek(sectorOffset * 4096L);
            raf.writeInt(compressed.length + 1); // length (data + compression type)
            raf.writeByte(2); // zlib
            raf.write(compressed);

            // Sektoru doldur
            long remaining = (sectorCount * 4096L) - (compressed.length + 5);
            for (long i = 0; i < remaining; i++) {
                raf.writeByte(0);
            }
        }

        // Oku ve dogrula
        RegionFile region = new RegionFile(mcaFile);
        assertEquals("Region X", 0, region.getRegionX());
        assertEquals("Region Z", 0, region.getRegionZ());
        assertTrue("Has chunk(0,0)", region.hasChunk(0, 0));
        assertFalse("No chunk(1,0)", region.hasChunk(1, 0));
        assertEquals("Chunk count", 1, region.getChunkCount());

        NBTTag chunkTag = region.readChunkData(0, 0);
        assertNotNull("Chunk NBT", chunkTag);

        NBTTag dvTag = chunkTag.getCompound("DataVersion");
        assertNotNull("DataVersion", dvTag);
        assertEquals("DataVersion value", 3700, dvTag.getInt());

        // ChunkReader testi
        ChunkReader reader = new ChunkReader(chunkTag);
        assertEquals("Chunk X", 0, reader.getChunkX());
        assertEquals("Chunk Z", 0, reader.getChunkZ());
        assertEquals("DataVersion", 3700, reader.getDataVersion());

        // Blok okuma testi - section 0'daki bloklar
        String blockAt000 = reader.getBlockAt(0, 0, 0);
        assertNotNull("Block at 0,0,0", blockAt000);
        System.out.println("  Blok (0,0,0) = " + blockAt000);

        String blockAt111 = reader.getBlockAt(1, 1, 1);
        assertNotNull("Block at 1,1,1", blockAt111);
        System.out.println("  Blok (1,1,1) = " + blockAt111);

        // Temizlik
        mcaFile.delete();
        regionDir.delete();
        tempDir.delete();

        System.out.println();
    }

    // ============ ChunkReader Detayli Test ============

    static void testChunkReader() throws Exception {
        System.out.println("--- ChunkReader Detayli Test ---");

        // Bilinen bloklar iceren bir chunk olustur
        // Section 0, tum bloklar stone, (5,0,5) = diamond_ore
        byte[] chunkNBT = createChunkWithKnownBlocks();

        NBTTag tag = NBTReader.readUncompressed(chunkNBT);
        ChunkReader reader = new ChunkReader(tag);

        // Stone kontrolu
        String block = reader.getBlockAt(0, 0, 0);
        assertEquals("Block 0,0,0 should be stone", "minecraft:stone", block);

        // Diamond kontrolu
        String diamond = reader.getBlockAt(5, 0, 5);
        assertEquals("Block 5,0,5 should be diamond_ore", "minecraft:diamond_ore", diamond);

        // Baska bir stone
        String stone2 = reader.getBlockAt(3, 0, 3);
        assertEquals("Block 3,0,3 should be stone", "minecraft:stone", stone2);

        System.out.println();
    }

    // ============ PatternMatcher Testi ============

    static void testPatternMatcher() throws Exception {
        System.out.println("--- PatternMatcher Testi ---");

        // Test dunyasi olustur
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "mc_match_test_" + System.currentTimeMillis());
        File regionDir = new File(tempDir, "region");
        regionDir.mkdirs();

        // Bilinen bloklari iceren chunk olustur
        byte[] chunkNBT = createChunkWithKnownBlocks();

        // Zlib ile sikistir
        ByteArrayOutputStream compressedBaos = new ByteArrayOutputStream();
        DeflaterOutputStream deflater = new DeflaterOutputStream(compressedBaos);
        deflater.write(chunkNBT);
        deflater.finish();
        deflater.close();
        byte[] compressed = compressedBaos.toByteArray();

        // .mca dosyasi yaz
        File mcaFile = new File(regionDir, "r.0.0.mca");
        try (RandomAccessFile raf = new RandomAccessFile(mcaFile, "rw")) {
            int sectorOffset = 2;
            int sectorCount = (compressed.length + 5 + 4095) / 4096;
            int locationEntry = (sectorOffset << 8) | (sectorCount & 0xFF);
            raf.writeInt(locationEntry);
            for (int i = 1; i < 1024; i++) raf.writeInt(0);
            for (int i = 0; i < 1024; i++) raf.writeInt(0);

            raf.seek(sectorOffset * 4096L);
            raf.writeInt(compressed.length + 1);
            raf.writeByte(2);
            raf.write(compressed);
        }

        // Pattern: tek blok stone arama (0,0,0'da stone var)
        BlockPattern pattern = new BlockPattern();
        pattern.setBlock(0, 0, 0, new BlockData("minecraft:stone", 0));

        final List<SearchResult> results = new ArrayList<>();
        final boolean[] completed = {false};

        PatternMatcher matcher = new PatternMatcher(tempDir, pattern,
                0, 15, 0, 15, 0, 3,
                new PatternMatcher.ProgressListener() {
                    @Override
                    public void onProgress(int current, int total, String message) {
                        // sessiz
                    }

                    @Override
                    public void onResult(SearchResult result) {
                        results.add(result);
                    }

                    @Override
                    public void onComplete(int totalResults, long elapsedMs) {
                        completed[0] = true;
                        System.out.println("  Arama tamamlandi: " + totalResults + " sonuc, " + elapsedMs + "ms");
                    }

                    @Override
                    public void onError(String error) {
                        System.err.println("  HATA: " + error);
                    }
                });

        matcher.search();
        assertTrue("Arama tamamlandi", completed[0]);
        assertTrue("En az bir sonuc bulundu", results.size() > 0);

        System.out.println("  Bulunan sonuc sayisi: " + results.size());
        for (SearchResult r : results) {
            System.out.println("    " + r);
        }

        // Diamond pattern arama
        BlockPattern diamondPattern = new BlockPattern();
        diamondPattern.setBlock(0, 0, 0, new BlockData("minecraft:diamond_ore", 0));

        final List<SearchResult> diamondResults = new ArrayList<>();

        PatternMatcher diamondMatcher = new PatternMatcher(tempDir, diamondPattern,
                0, 15, 0, 15, 0, 3,
                new PatternMatcher.ProgressListener() {
                    @Override public void onProgress(int c, int t, String m) {}
                    @Override public void onResult(SearchResult result) { diamondResults.add(result); }
                    @Override public void onComplete(int t, long e) {
                        System.out.println("  Diamond arama: " + t + " sonuc, " + e + "ms");
                    }
                    @Override public void onError(String e) { System.err.println("  HATA: " + e); }
                });

        diamondMatcher.search();
        assertTrue("Diamond bulundu", diamondResults.size() > 0);

        boolean foundAt5_0_5 = false;
        for (SearchResult r : diamondResults) {
            if (r.getX() == 5 && r.getY() == 0 && r.getZ() == 5) {
                foundAt5_0_5 = true;
            }
        }
        assertTrue("Diamond (5,0,5) pozisyonunda bulundu", foundAt5_0_5);

        // Temizlik
        mcaFile.delete();
        regionDir.delete();
        tempDir.delete();

        System.out.println();
    }

    // ============ Yardimci Metodlar ============

    /**
     * Bilinen bloklari olan bir chunk NBT verisi olusturur.
     * Tum bloklar stone, (5,0,5) = diamond_ore
     * Format: 1.18+ (DataVersion 3700)
     */
    static byte[] createChunkWithKnownBlocks() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Root compound
        dos.writeByte(NBTTag.TAG_COMPOUND);
        writeString(dos, "");

        // DataVersion
        dos.writeByte(NBTTag.TAG_INT);
        writeString(dos, "DataVersion");
        dos.writeInt(3700);

        // xPos
        dos.writeByte(NBTTag.TAG_INT);
        writeString(dos, "xPos");
        dos.writeInt(0);

        // zPos
        dos.writeByte(NBTTag.TAG_INT);
        writeString(dos, "zPos");
        dos.writeInt(0);

        // sections (1.18+ format, lowercase)
        dos.writeByte(NBTTag.TAG_LIST);
        writeString(dos, "sections");
        dos.writeByte(NBTTag.TAG_COMPOUND); // list element type
        dos.writeInt(1); // 1 section

        // Section 0
        {
            // Y
            dos.writeByte(NBTTag.TAG_BYTE);
            writeString(dos, "Y");
            dos.writeByte(0);

            // block_states compound
            dos.writeByte(NBTTag.TAG_COMPOUND);
            writeString(dos, "block_states");

            // palette: [stone, diamond_ore]
            dos.writeByte(NBTTag.TAG_LIST);
            writeString(dos, "palette");
            dos.writeByte(NBTTag.TAG_COMPOUND);
            dos.writeInt(2);

            // palette[0] = stone
            dos.writeByte(NBTTag.TAG_STRING);
            writeString(dos, "Name");
            writeString(dos, "minecraft:stone");
            dos.writeByte(NBTTag.TAG_END);

            // palette[1] = diamond_ore
            dos.writeByte(NBTTag.TAG_STRING);
            writeString(dos, "Name");
            writeString(dos, "minecraft:diamond_ore");
            dos.writeByte(NBTTag.TAG_END);

            // data: long array
            // 2 palette entries -> bitsPerBlock = max(4, ceil(log2(2))) = 4
            // 4096 bloklar (16x16x16)
            // 1.16+ paketleme: valuesPerLong = 64/4 = 16
            // longCount = ceil(4096/16) = 256

            int bitsPerBlock = 4;
            int valuesPerLong = 64 / bitsPerBlock;
            int longCount = (4096 + valuesPerLong - 1) / valuesPerLong;

            // Tum bloklar palette[0] = stone (0), sadece (5,0,5) = palette[1] = diamond_ore (1)
            // Blok indexi: Y*256 + Z*16 + X
            // (5,0,5) -> 0*256 + 5*16 + 5 = 85

            long[] data = new long[longCount];
            // Varsayilan olarak hepsi 0 (stone)

            // (5,0,5) icin palette index 1 yaz
            int blockIndex = 0 * 256 + 5 * 16 + 5; // = 85
            int longIdx = blockIndex / valuesPerLong;
            int bitOffset = (blockIndex % valuesPerLong) * bitsPerBlock;
            data[longIdx] |= (1L << bitOffset);

            dos.writeByte(NBTTag.TAG_LONG_ARRAY);
            writeString(dos, "data");
            dos.writeInt(longCount);
            for (long l : data) {
                dos.writeLong(l);
            }

            // block_states end
            dos.writeByte(NBTTag.TAG_END);

            // section end
            dos.writeByte(NBTTag.TAG_END);
        }

        // Root compound end
        dos.writeByte(NBTTag.TAG_END);

        dos.flush();
        return baos.toByteArray();
    }

    /**
     * Minimal chunk NBT verisi olusturur (section yok, sadece metadata).
     */
    static byte[] createMinimalChunkNBT(int chunkX, int chunkZ, int dataVersion) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeByte(NBTTag.TAG_COMPOUND);
        writeString(dos, "");

        dos.writeByte(NBTTag.TAG_INT);
        writeString(dos, "DataVersion");
        dos.writeInt(dataVersion);

        dos.writeByte(NBTTag.TAG_INT);
        writeString(dos, "xPos");
        dos.writeInt(chunkX);

        dos.writeByte(NBTTag.TAG_INT);
        writeString(dos, "zPos");
        dos.writeInt(chunkZ);

        // Bos sections listesi
        dos.writeByte(NBTTag.TAG_LIST);
        writeString(dos, "sections");
        dos.writeByte(NBTTag.TAG_COMPOUND);
        dos.writeInt(0);

        dos.writeByte(NBTTag.TAG_END);

        dos.flush();
        return baos.toByteArray();
    }

    static void writeString(DataOutputStream dos, String s) throws IOException {
        byte[] bytes = s.getBytes("UTF-8");
        dos.writeShort(bytes.length);
        dos.write(bytes);
    }

    // ============ Assert Metodlari ============

    static void assertEquals(String label, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            System.out.println("  OK " + label + " = " + actual);
            passed++;
        } else {
            System.out.println("  FAIL " + label + ": beklenen=" + expected + ", gercek=" + actual);
            failed++;
        }
    }

    static void assertEquals(String label, long expected, long actual) {
        if (expected == actual) {
            System.out.println("  OK " + label + " = " + actual);
            passed++;
        } else {
            System.out.println("  FAIL " + label + ": beklenen=" + expected + ", gercek=" + actual);
            failed++;
        }
    }

    static void assertTrue(String label, boolean condition) {
        if (condition) {
            System.out.println("  OK " + label);
            passed++;
        } else {
            System.out.println("  FAIL " + label);
            failed++;
        }
    }

    static void assertFalse(String label, boolean condition) {
        assertTrue(label, !condition);
    }

    static void assertNotNull(String label, Object obj) {
        if (obj != null) {
            System.out.println("  OK " + label + " != null");
            passed++;
        } else {
            System.out.println("  FAIL " + label + " is null!");
            failed++;
        }
    }
}
