package models;

/**
 * Bulunan bir desen eşleşmesinin koordinat bilgilerini tutar.
 */
public class SearchResult {
    private final int x;
    private final int y;
    private final int z;
    private final int matchCount;
    private final int totalBlocks;

    public SearchResult(int x, int y, int z, int matchCount, int totalBlocks) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.matchCount = matchCount;
        this.totalBlocks = totalBlocks;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public int getMatchCount() { return matchCount; }
    public int getTotalBlocks() { return totalBlocks; }

    public double getMatchPercentage() {
        return totalBlocks > 0 ? (matchCount * 100.0 / totalBlocks) : 0;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d, %d) - %d/%d blok eslesme (%%%.1f)",
                x, y, z, matchCount, totalBlocks, getMatchPercentage());
    }
}
