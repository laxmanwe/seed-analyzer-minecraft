package minecraft;

import models.BlockData;

/**
 * Minecraft chunk verisinden blok bilgilerini cikarir.
 *
 * Desteklenen formatlar:
 * - 1.13-1.15 (DataVersion < 2529): Eski paketleme (degerler long sinirlarini asabilir)
 * - 1.16-1.17 (DataVersion 2529-2859): Yeni paketleme (degerler long sinirlarini asmaz)
 * - 1.18+ (DataVersion >= 2860): Yeni section yapisi, sections root altinda
 */
public class ChunkReader {

    // DataVersion sinir degerleri
    private static final int DV_NEW_PACKING = 2529;    // 1.16+: Yeni long paketleme
    private static final int DV_NEW_SECTIONS = 2860;   // 1.18+: Yeni section yapisi

    private final NBTTag chunkNBT;
    private final int dataVersion;
    private final int chunkX;
    private final int chunkZ;

    public ChunkReader(NBTTag chunkNBT) {
        this.chunkNBT = chunkNBT;

        // DataVersion oku
        NBTTag dvTag = chunkNBT.getCompound("DataVersion");
        this.dataVersion = dvTag != null ? dvTag.getInt() : 0;

        // Chunk koordinatlarini oku
        int cx = 0, cz = 0;
        if (dataVersion >= DV_NEW_SECTIONS) {
            // 1.18+: xPos, zPos root altinda
            NBTTag xTag = chunkNBT.getCompound("xPos");
            NBTTag zTag = chunkNBT.getCompound("zPos");
            if (xTag != null) cx = xTag.getInt();
            if (zTag != null) cz = zTag.getInt();
        } else {
            // Pre-1.18: Level compound altinda
            NBTTag level = chunkNBT.getCompound("Level");
            if (level != null) {
                NBTTag xTag = level.getCompound("xPos");
                NBTTag zTag = level.getCompound("zPos");
                if (xTag != null) cx = xTag.getInt();
                if (zTag != null) cz = zTag.getInt();
            }
        }
        this.chunkX = cx;
        this.chunkZ = cz;
    }

    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public int getDataVersion() { return dataVersion; }

    /**
     * Verilen dunya koordinatindaki blogun ID'sini dondurur.
     * @param worldX Dunya X koordinati
     * @param worldY Dunya Y koordinati
     * @param worldZ Dunya Z koordinati
     * @return Blok ID'si veya null (section yoksa)
     */
    public String getBlockAt(int worldX, int worldY, int worldZ) {
        // Local chunk koordinatlari (0-15)
        int localX = worldX & 15;
        int localZ = worldZ & 15;
        int localY = worldY & 15;

        // Section indeksi
        int sectionY = worldY >> 4; // floor(worldY / 16)

        // Ilgili section'i bul
        NBTTag section = findSection(sectionY);
        if (section == null) {
            // Section yoksa hava kabul et
            return "minecraft:air";
        }

        return getBlockFromSection(section, localX, localY, localZ);
    }

    /**
     * Verilen Y indeksindeki section'i bulur.
     */
    private NBTTag findSection(int sectionY) {
        NBTTag sectionsTag;

        if (dataVersion >= DV_NEW_SECTIONS) {
            // 1.18+: "sections" root altinda (kucuk harf)
            sectionsTag = chunkNBT.getCompound("sections");
        } else {
            // Pre-1.18: "Sections" Level altinda (buyuk harf)
            NBTTag level = chunkNBT.getCompound("Level");
            if (level == null) return null;
            sectionsTag = level.getCompound("Sections");
        }

        if (sectionsTag == null || sectionsTag.getType() != NBTTag.TAG_LIST) {
            return null;
        }

        for (NBTTag section : sectionsTag.getList()) {
            NBTTag yTag = section.getCompound("Y");
            if (yTag != null && yTag.getInt() == sectionY) {
                return section;
            }
        }

        return null;
    }

    /**
     * Section'dan belirli koordinattaki blok ID'sini okur.
     * Palette-based blok depolama formatini kullanir.
     */
    private String getBlockFromSection(NBTTag section, int localX, int localY, int localZ) {
        // block_states compound'unu bul
        NBTTag blockStates;
        if (dataVersion >= DV_NEW_SECTIONS) {
            blockStates = section.getCompound("block_states");
        } else {
            // Pre-1.18'de palette ve data dogrudan section altinda
            blockStates = section;
        }

        if (blockStates == null) return "minecraft:air";

        // Palette'i oku
        NBTTag paletteTag;
        if (dataVersion >= DV_NEW_SECTIONS) {
            paletteTag = blockStates.getCompound("palette");
        } else {
            paletteTag = blockStates.getCompound("Palette");
        }

        if (paletteTag == null || paletteTag.getType() != NBTTag.TAG_LIST) {
            return "minecraft:air";
        }

        java.util.List<NBTTag> palette = paletteTag.getList();
        if (palette.isEmpty()) return "minecraft:air";

        // Palette tek eleman ise, tum section o bloktur
        if (palette.size() == 1) {
            NBTTag entry = palette.get(0);
            NBTTag nameTag = entry.getCompound("Name");
            return nameTag != null ? nameTag.getString() : "minecraft:air";
        }

        // Data long dizisini oku
        NBTTag dataTag;
        if (dataVersion >= DV_NEW_SECTIONS) {
            dataTag = blockStates.getCompound("data");
        } else {
            dataTag = blockStates.getCompound("BlockStates");
        }

        if (dataTag == null || dataTag.getType() != NBTTag.TAG_LONG_ARRAY) {
            return "minecraft:air";
        }

        long[] data = dataTag.getLongArray();
        if (data.length == 0) return "minecraft:air";

        // Blok indeksi: Y * 16 * 16 + Z * 16 + X
        int blockIndex = localY * 256 + localZ * 16 + localX;

        // Palette boyutuna gore bit sayisi
        int bitsPerBlock = Math.max(4, ceilLog2(palette.size()));

        // Paketleme yontemine gore palette indeksini oku
        int paletteIndex;
        if (dataVersion >= DV_NEW_PACKING) {
            paletteIndex = readPackedNew(data, blockIndex, bitsPerBlock);
        } else {
            paletteIndex = readPackedOld(data, blockIndex, bitsPerBlock);
        }

        if (paletteIndex < 0 || paletteIndex >= palette.size()) {
            return "minecraft:air";
        }

        // Palette'den blok adini al
        NBTTag entry = palette.get(paletteIndex);
        NBTTag nameTag = entry.getCompound("Name");
        return nameTag != null ? nameTag.getString() : "minecraft:air";
    }

    /**
     * Yeni paketleme formati (1.16+): Degerler long sinirlarini asmaz.
     * Her long floor(64 / bitsPerBlock) deger icerir.
     */
    private int readPackedNew(long[] data, int blockIndex, int bitsPerBlock) {
        int valuesPerLong = 64 / bitsPerBlock;
        int longIndex = blockIndex / valuesPerLong;
        int bitOffset = (blockIndex % valuesPerLong) * bitsPerBlock;

        if (longIndex >= data.length) return 0;

        long mask = (1L << bitsPerBlock) - 1;
        return (int) ((data[longIndex] >>> bitOffset) & mask);
    }

    /**
     * Eski paketleme formati (1.13-1.15): Degerler long sinirlarini asabilir.
     */
    private int readPackedOld(long[] data, int blockIndex, int bitsPerBlock) {
        long totalBitOffset = (long) blockIndex * bitsPerBlock;
        int longIndex = (int) (totalBitOffset / 64);
        int bitOffset = (int) (totalBitOffset % 64);

        if (longIndex >= data.length) return 0;

        long mask = (1L << bitsPerBlock) - 1;
        int value = (int) ((data[longIndex] >>> bitOffset) & mask);

        // Deger long sinirini asiyorsa, sonraki long'dan devamini oku
        if (bitOffset + bitsPerBlock > 64 && longIndex + 1 < data.length) {
            int remainingBits = bitsPerBlock - (64 - bitOffset);
            long upperMask = (1L << remainingBits) - 1;
            value |= (int) ((data[longIndex + 1] & upperMask) << (64 - bitOffset));
        }

        return value;
    }

    /**
     * ceil(log2(n)) hesaplar.
     */
    private int ceilLog2(int n) {
        if (n <= 1) return 0;
        return 32 - Integer.numberOfLeadingZeros(n - 1);
    }
}
