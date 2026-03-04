package models;

/**
 * 4x4x4 boyutunda 3D blok deseni.
 */
public class BlockPattern {
    public static final int SIZE = 4;
    private final BlockData[][][] blocks;

    public BlockPattern() {
        blocks = new BlockData[SIZE][SIZE][SIZE];
        clear();
    }

    public void clear() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    blocks[x][y][z] = null;
                }
            }
        }
    }

    public void setBlock(int x, int y, int z, BlockData block) {
        if (isInBounds(x, y, z)) {
            blocks[x][y][z] = block;
        }
    }

    public BlockData getBlock(int x, int y, int z) {
        if (isInBounds(x, y, z)) {
            return blocks[x][y][z];
        }
        return null;
    }

    public boolean isEmpty() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (blocks[x][y][z] != null) return false;
                }
            }
        }
        return true;
    }

    /**
     * Desendeki tanımlanmış (null olmayan) blok sayısını döndürür.
     */
    public int getDefinedBlockCount() {
        int count = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (blocks[x][y][z] != null) count++;
                }
            }
        }
        return count;
    }

    private boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE;
    }

    @Override
    public String toString() {
        return "BlockPattern[" + getDefinedBlockCount() + " blocks defined]";
    }
}
