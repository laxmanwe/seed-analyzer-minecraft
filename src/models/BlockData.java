package models;

/**
 * Tek bir bloğun tipini ve metadata bilgisini tutar.
 */
public class BlockData {
    private final String blockId;
    private final int metadata;

    public static final BlockData AIR = new BlockData("minecraft:air", 0);

    public BlockData(String blockId, int metadata) {
        this.blockId = blockId != null ? blockId : "minecraft:air";
        this.metadata = metadata;
    }

    public String getBlockId() {
        return blockId;
    }

    public int getMetadata() {
        return metadata;
    }

    public boolean isAir() {
        return "minecraft:air".equals(blockId) || "minecraft:cave_air".equals(blockId)
                || "minecraft:void_air".equals(blockId);
    }

    public boolean matches(BlockData other) {
        if (other == null) return false;
        if (this.isAir() && other.isAir()) return true;
        return this.blockId.equals(other.blockId);
    }

    @Override
    public String toString() {
        String shortName = blockId.replace("minecraft:", "");
        return shortName + (metadata != 0 ? ":" + metadata : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BlockData)) return false;
        BlockData other = (BlockData) obj;
        return blockId.equals(other.blockId) && metadata == other.metadata;
    }

    @Override
    public int hashCode() {
        return blockId.hashCode() * 31 + metadata;
    }
}
