package analyzer;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Minecraft dunya dizini dogrulama ve bilgi araclari.
 */
public class RealSeedAnalyzer {

    /**
     * Dunya dizininin gecerli bir Minecraft dunya dizini olup olmadigini kontrol eder.
     */
    public static boolean isValidWorldDirectory(File worldDir) {
        if (worldDir == null || !worldDir.exists() || !worldDir.isDirectory()) {
            return false;
        }

        File regionDir = new File(worldDir, "region");
        if (!regionDir.exists() || !regionDir.isDirectory()) {
            return false;
        }

        File[] mcaFiles = regionDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mca");
            }
        });

        return mcaFiles != null && mcaFiles.length > 0;
    }

    /**
     * Dunya dizini hakkinda detayli bilgi dondurur.
     */
    public static String getWorldDirectoryInfo(File worldDir) {
        StringBuilder sb = new StringBuilder();

        if (worldDir == null) {
            return "Dunya dizini secilmedi.\n";
        }

        sb.append("Dunya: ").append(worldDir.getName()).append("\n");
        sb.append("Yol: ").append(worldDir.getAbsolutePath()).append("\n");

        // region klasoru kontrolu
        File regionDir = new File(worldDir, "region");
        if (regionDir.exists() && regionDir.isDirectory()) {
            File[] mcaFiles = regionDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".mca");
                }
            });

            int mcaCount = mcaFiles != null ? mcaFiles.length : 0;
            sb.append("Region dosyalari: ").append(mcaCount).append(" adet\n");

            if (mcaFiles != null && mcaCount > 0) {
                long totalSize = 0;
                for (File f : mcaFiles) {
                    totalSize += f.length();
                }
                sb.append("Toplam boyut: ").append(formatSize(totalSize)).append("\n");

                // Kaplanan alan hesapla
                int minRX = Integer.MAX_VALUE, maxRX = Integer.MIN_VALUE;
                int minRZ = Integer.MAX_VALUE, maxRZ = Integer.MIN_VALUE;
                for (File f : mcaFiles) {
                    String[] parts = f.getName().replace(".mca", "").split("\\.");
                    if (parts.length >= 3) {
                        try {
                            int rx = Integer.parseInt(parts[1]);
                            int rz = Integer.parseInt(parts[2]);
                            minRX = Math.min(minRX, rx);
                            maxRX = Math.max(maxRX, rx);
                            minRZ = Math.min(minRZ, rz);
                            maxRZ = Math.max(maxRZ, rz);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                if (minRX != Integer.MAX_VALUE) {
                    int blockMinX = minRX * 512;
                    int blockMaxX = (maxRX + 1) * 512 - 1;
                    int blockMinZ = minRZ * 512;
                    int blockMaxZ = (maxRZ + 1) * 512 - 1;
                    sb.append("Kaplanan alan: X[").append(blockMinX).append(" ~ ").append(blockMaxX)
                      .append("] Z[").append(blockMinZ).append(" ~ ").append(blockMaxZ).append("]\n");
                }
            }
        } else {
            sb.append("Region klasoru bulunamadi!\n");
        }

        // level.dat kontrolu
        File levelDat = new File(worldDir, "level.dat");
        if (levelDat.exists()) {
            sb.append("level.dat: Mevcut (").append(formatSize(levelDat.length())).append(")\n");
        } else {
            sb.append("level.dat: Bulunamadi\n");
        }

        // DIM-1 (Nether) kontrolu
        File nether = new File(worldDir, "DIM-1/region");
        if (nether.exists()) {
            File[] netherFiles = nether.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".mca");
                }
            });
            sb.append("Nether: ").append(netherFiles != null ? netherFiles.length : 0).append(" region\n");
        }

        // DIM1 (The End) kontrolu
        File end = new File(worldDir, "DIM1/region");
        if (end.exists()) {
            File[] endFiles = end.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".mca");
                }
            });
            sb.append("The End: ").append(endFiles != null ? endFiles.length : 0).append(" region\n");
        }

        return sb.toString();
    }

    /**
     * Region dosyalarini listeler.
     */
    public static File[] getRegionFiles(File worldDir) {
        File regionDir = new File(worldDir, "region");
        if (!regionDir.exists() || !regionDir.isDirectory()) {
            return new File[0];
        }

        File[] files = regionDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mca");
            }
        });

        return files != null ? files : new File[0];
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
