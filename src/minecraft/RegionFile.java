package minecraft;

import java.io.*;

/**
 * Minecraft .mca (Anvil) region dosya okuyucu.
 * Her region dosyasi 32x32 chunk icerir.
 *
 * Format:
 * - Ilk 4096 byte: Konum tablosu (1024 giris x 4 byte)
 *   - Her giris: 3 byte offset (4KB sektor cinsinden), 1 byte sektor sayisi
 * - Sonraki 4096 byte: Zaman damgasi tablosu (1024 giris x 4 byte)
 * - Geri kalan: Chunk veri sektorleri
 *   - Her chunk: 4 byte uzunluk, 1 byte sikistirma tipi (1=gzip, 2=zlib), veri
 */
public class RegionFile {

    private final File file;
    private final int regionX;
    private final int regionZ;

    // Konum tablosu: offset ve sektor sayisi
    private final int[] offsets = new int[1024];
    private final int[] sectorCounts = new int[1024];

    public RegionFile(File file) throws IOException {
        this.file = file;

        // Dosya adinden region koordinatlarini cikart (r.X.Z.mca)
        String name = file.getName();
        String[] parts = name.replace(".mca", "").split("\\.");
        if (parts.length >= 3) {
            this.regionX = Integer.parseInt(parts[1]);
            this.regionZ = Integer.parseInt(parts[2]);
        } else {
            this.regionX = 0;
            this.regionZ = 0;
        }

        readHeader();
    }

    private void readHeader() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (raf.length() < 8192) {
                return; // Dosya cok kucuk, header yok
            }

            // Konum tablosunu oku
            for (int i = 0; i < 1024; i++) {
                int val = raf.readInt();
                offsets[i] = (val >> 8) & 0xFFFFFF;
                sectorCounts[i] = val & 0xFF;
            }
        }
    }

    /**
     * Region koordinatlarini dondurur.
     */
    public int getRegionX() { return regionX; }
    public int getRegionZ() { return regionZ; }

    /**
     * Bu region icindeki chunk'in varolup olmadigini kontrol eder.
     * @param localX Chunk X (0-31)
     * @param localZ Chunk Z (0-31)
     */
    public boolean hasChunk(int localX, int localZ) {
        int index = localX + localZ * 32;
        return index >= 0 && index < 1024 && offsets[index] != 0;
    }

    /**
     * Chunk verisini okur ve NBT olarak parse eder.
     * @param localX Chunk X (0-31)
     * @param localZ Chunk Z (0-31)
     * @return Chunk NBT verisi veya null
     */
    public NBTTag readChunkData(int localX, int localZ) throws IOException {
        int index = localX + localZ * 32;
        if (index < 0 || index >= 1024 || offsets[index] == 0) {
            return null;
        }

        long offset = (long) offsets[index] * 4096;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (offset + 5 > raf.length()) {
                return null;
            }

            raf.seek(offset);

            int length = raf.readInt();
            if (length <= 1) return null;

            byte compressionType = raf.readByte();
            int dataLength = length - 1;

            if (offset + 5 + dataLength > raf.length()) {
                return null;
            }

            byte[] compressedData = new byte[dataLength];
            raf.readFully(compressedData);

            // Sikistirma tipine gore decompress et
            switch (compressionType) {
                case 1: // GZip
                    return NBTReader.readGzip(compressedData);
                case 2: // Zlib
                    return NBTReader.readZlib(compressedData);
                case 3: // Sikistirilmamis
                    return NBTReader.readUncompressed(compressedData);
                default:
                    System.err.println("Bilinmeyen sikistirma tipi: " + compressionType);
                    return null;
            }
        }
    }

    /**
     * Bu region dosyasindaki mevcut chunk sayisini dondurur.
     */
    public int getChunkCount() {
        int count = 0;
        for (int i = 0; i < 1024; i++) {
            if (offsets[i] != 0) count++;
        }
        return count;
    }

    /**
     * Global chunk koordinatlarindan local (region icindeki) koordinat hesaplar.
     */
    public static int toLocal(int chunkCoord) {
        int local = chunkCoord & 31; // mod 32, Java'da negatif sayilar icin
        return local < 0 ? local + 32 : local;
    }

    /**
     * Block koordinatindan chunk koordinati hesaplar.
     */
    public static int blockToChunk(int blockCoord) {
        return blockCoord >> 4; // floor(blockCoord / 16)
    }

    /**
     * Chunk koordinatindan region koordinati hesaplar.
     */
    public static int chunkToRegion(int chunkCoord) {
        return chunkCoord >> 5; // floor(chunkCoord / 32)
    }

    @Override
    public String toString() {
        return String.format("Region(%d, %d) - %d chunks [%s]",
                regionX, regionZ, getChunkCount(), file.getName());
    }
}
