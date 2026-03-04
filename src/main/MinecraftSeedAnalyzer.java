package main;

import analyzer.PatternMatcher;
import analyzer.RealSeedAnalyzer;
import models.BlockPattern;
import models.SearchResult;
import ui.PatternPanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

/**
 * Minecraft Seed Analyzer - Ana Uygulama
 *
 * Gercek Minecraft dunya dosyalarini (.mca) okuyarak
 * kullanici tarafindan tanimlanan blok desenlerini arar.
 */
public class MinecraftSeedAnalyzer extends JFrame {

    // UI bilesenleri
    private JTextField seedField;
    private JComboBox<String> versionCombo;
    private JSpinner startXSpinner, endXSpinner, startZSpinner, endZSpinner;
    private JSpinner minYSpinner, maxYSpinner;
    private PatternPanel patternPanel;
    private JTextArea logArea;
    private JButton searchButton, stopButton, browseButton;
    private JLabel worldLabel;
    private JProgressBar progressBar;
    private DefaultListModel<String> resultListModel;
    private JList<String> resultList;

    // Durum
    private File selectedWorldDirectory;
    private PatternMatcher currentMatcher;
    private volatile boolean isSearching = false;

    public MinecraftSeedAnalyzer() {
        setTitle("Minecraft Seed Analyzer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Ust panel: Dunya secimi ve ayarlar
        mainPanel.add(createTopPanel(), BorderLayout.NORTH);

        // Orta: Sol taraf pattern, sag taraf sonuclar
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setResizeWeight(0.4);
        centerSplit.setLeftComponent(createPatternSection());
        centerSplit.setRightComponent(createResultSection());
        mainPanel.add(centerSplit, BorderLayout.CENTER);

        // Alt panel: Log ve kontroller
        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Dunya ve Arama Ayarlari"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Satir 1: Dunya secimi
        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(new JLabel("Minecraft Dunya:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 3;
        worldLabel = new JLabel("Dunya secilmedi");
        worldLabel.setForeground(Color.GRAY);
        topPanel.add(worldLabel, gbc);

        gbc.gridx = 4; gbc.weightx = 0; gbc.gridwidth = 1;
        browseButton = new JButton("Gozat...");
        browseButton.addActionListener(e -> browseWorld());
        topPanel.add(browseButton, gbc);

        // Satir 2: Seed ve surumh
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        topPanel.add(new JLabel("Seed (opsiyonel):"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.5; gbc.gridwidth = 1;
        seedField = new JTextField(15);
        seedField.setToolTipText("Bilgi amacli - gercek arama dunya dosyalarindan yapilir");
        topPanel.add(seedField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        topPanel.add(new JLabel("Surum:"), gbc);

        gbc.gridx = 3; gbc.weightx = 0;
        versionCombo = new JComboBox<>(new String[]{
                "1.20.x", "1.19.x", "1.18.x", "1.17.x", "1.16.x"
        });
        topPanel.add(versionCombo, gbc);

        // Satir 3: Arama alani X/Z
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        topPanel.add(new JLabel("Arama X:"), gbc);

        gbc.gridx = 1;
        JPanel xPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        startXSpinner = new JSpinner(new SpinnerNumberModel(-100, -30000, 30000, 16));
        endXSpinner = new JSpinner(new SpinnerNumberModel(100, -30000, 30000, 16));
        xPanel.add(startXSpinner);
        xPanel.add(new JLabel("~"));
        xPanel.add(endXSpinner);
        topPanel.add(xPanel, gbc);

        gbc.gridx = 2;
        topPanel.add(new JLabel("Arama Z:"), gbc);

        gbc.gridx = 3;
        JPanel zPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        startZSpinner = new JSpinner(new SpinnerNumberModel(-100, -30000, 30000, 16));
        endZSpinner = new JSpinner(new SpinnerNumberModel(100, -30000, 30000, 16));
        zPanel.add(startZSpinner);
        zPanel.add(new JLabel("~"));
        zPanel.add(endZSpinner);
        topPanel.add(zPanel, gbc);

        // Satir 4: Y araligi
        gbc.gridx = 0; gbc.gridy = 3;
        topPanel.add(new JLabel("Y Araligi:"), gbc);

        gbc.gridx = 1;
        JPanel yPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        minYSpinner = new JSpinner(new SpinnerNumberModel(-64, -64, 320, 1));
        maxYSpinner = new JSpinner(new SpinnerNumberModel(64, -64, 320, 1));
        yPanel.add(minYSpinner);
        yPanel.add(new JLabel("~"));
        yPanel.add(maxYSpinner);
        topPanel.add(yPanel, gbc);

        gbc.gridx = 2; gbc.gridwidth = 2;
        JButton autoDetectBtn = new JButton("Otomatik Algiyla");
        autoDetectBtn.setToolTipText("Dunya dosyasindan mevcut alani otomatik algila");
        autoDetectBtn.addActionListener(e -> autoDetectSearchArea());
        topPanel.add(autoDetectBtn, gbc);

        return topPanel;
    }

    private JPanel createPatternSection() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        patternPanel = new PatternPanel();
        panel.add(patternPanel, BorderLayout.CENTER);

        // Ornek pattern butonlari
        JPanel samplePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
        samplePanel.setBorder(BorderFactory.createTitledBorder("Ornek Desenler"));

        JButton stoneBtn = new JButton("Tas");
        stoneBtn.addActionListener(e -> loadSamplePattern("stone"));
        samplePanel.add(stoneBtn);

        JButton dirtBtn = new JButton("Toprak");
        dirtBtn.addActionListener(e -> loadSamplePattern("dirt"));
        samplePanel.add(dirtBtn);

        JButton diamondBtn = new JButton("Elmas");
        diamondBtn.addActionListener(e -> loadSamplePattern("diamond"));
        samplePanel.add(diamondBtn);

        JButton oreVeinBtn = new JButton("Cevher Damari");
        oreVeinBtn.addActionListener(e -> loadSamplePattern("ore_vein"));
        samplePanel.add(oreVeinBtn);

        panel.add(samplePanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createResultSection() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Arama Sonuclari"));

        resultListModel = new DefaultListModel<>();
        resultList = new JList<>(resultListModel);
        resultList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Sonuc bilgisi
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearResults = new JButton("Sonuclari Temizle");
        clearResults.addActionListener(e -> resultListModel.clear());
        infoPanel.add(clearResults);

        JButton exportBtn = new JButton("Disa Aktar");
        exportBtn.addActionListener(e -> exportResults());
        infoPanel.add(exportBtn);

        panel.add(infoPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        // Log alani
        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setText("Minecraft Seed Analyzer - Hazir\n");
        logArea.append("Bir Minecraft dunya klasoru secerek aramaya baslayabilirsiniz.\n");
        logArea.append("Dunya klasoru: .minecraft/saves/ altindaki dunya klasorleridir.\n\n");
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Islem Gunlugu"));
        bottomPanel.add(logScroll, BorderLayout.CENTER);

        // Kontrol paneli
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        searchButton = new JButton("Aramayi Baslat");
        searchButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        searchButton.setPreferredSize(new Dimension(180, 40));
        searchButton.setBackground(new Color(50, 150, 50));
        searchButton.setForeground(Color.WHITE);
        searchButton.setOpaque(true);
        searchButton.addActionListener(e -> startSearch());
        controlPanel.add(searchButton);

        stopButton = new JButton("Durdur");
        stopButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        stopButton.setPreferredSize(new Dimension(120, 40));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopSearch());
        controlPanel.add(stopButton);

        JButton validateBtn = new JButton("Ayarlari Dogrula");
        validateBtn.addActionListener(e -> validateSettings());
        controlPanel.add(validateBtn);

        bottomPanel.add(controlPanel, BorderLayout.NORTH);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Hazir");
        bottomPanel.add(progressBar, BorderLayout.SOUTH);

        return bottomPanel;
    }

    // === Islevsel Metodlar ===

    private void browseWorld() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Minecraft Dunya Klasoru Secin");

        // Varsayilan Minecraft saves klasorunu dene
        String home = System.getProperty("user.home");
        File defaultDir = null;

        // Windows
        File winDir = new File(home, "AppData/Roaming/.minecraft/saves");
        // Linux
        File linuxDir = new File(home, ".minecraft/saves");
        // macOS
        File macDir = new File(home, "Library/Application Support/minecraft/saves");

        if (winDir.exists()) defaultDir = winDir;
        else if (linuxDir.exists()) defaultDir = linuxDir;
        else if (macDir.exists()) defaultDir = macDir;

        if (defaultDir != null) {
            chooser.setCurrentDirectory(defaultDir);
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();

            if (RealSeedAnalyzer.isValidWorldDirectory(selected)) {
                selectedWorldDirectory = selected;
                worldLabel.setText(selected.getName() + " (" + selected.getAbsolutePath() + ")");
                worldLabel.setForeground(new Color(0, 120, 0));

                logArea.append("\n=== DUNYA YUKLENDI ===\n");
                logArea.append(RealSeedAnalyzer.getWorldDirectoryInfo(selected));
                logArea.append("========================\n\n");

                // Seed bilgisini level.dat'dan okumaya calis
                readSeedFromWorld(selected);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Gecersiz Minecraft dunya klasoru!\n\n" +
                                "Secilen klasorde 'region' alt klasoru ve .mca dosyalari bulunamadi.\n" +
                                "Lutfen .minecraft/saves/ altindaki bir dunya klasoru secin.",
                        "Gecersiz Dunya",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void readSeedFromWorld(File worldDir) {
        File levelDat = new File(worldDir, "level.dat");
        if (!levelDat.exists()) return;

        try {
            minecraft.NBTTag root = minecraft.NBTReader.readGzipFile(levelDat);
            if (root != null) {
                minecraft.NBTTag data = root.getCompound("Data");
                if (data != null) {
                    // Seed oku
                    minecraft.NBTTag seedTag = data.getCompound("RandomSeed");
                    if (seedTag != null) {
                        long seed = seedTag.getLong();
                        seedField.setText(String.valueOf(seed));
                        logArea.append("Seed level.dat'dan okundu: " + seed + "\n");
                    }

                    // Dunya adini oku
                    minecraft.NBTTag nameTag = data.getCompound("LevelName");
                    if (nameTag != null) {
                        logArea.append("Dunya adi: " + nameTag.getString() + "\n");
                    }

                    // DataVersion oku
                    minecraft.NBTTag dvTag = data.getCompound("DataVersion");
                    if (dvTag != null) {
                        logArea.append("DataVersion: " + dvTag.getInt() + "\n");
                    }

                    // Oyun modu
                    minecraft.NBTTag gmTag = data.getCompound("GameType");
                    if (gmTag != null) {
                        String[] modes = {"Survival", "Creative", "Adventure", "Spectator"};
                        int gm = gmTag.getInt();
                        String modeName = gm >= 0 && gm < modes.length ? modes[gm] : "Bilinmeyen";
                        logArea.append("Oyun modu: " + modeName + "\n");
                    }
                }
            }
        } catch (Exception e) {
            logArea.append("level.dat okunamadi: " + e.getMessage() + "\n");
        }
    }

    private void autoDetectSearchArea() {
        if (selectedWorldDirectory == null) {
            JOptionPane.showMessageDialog(this,
                    "Once bir Minecraft dunya klasoru secin!",
                    "Uyari", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File[] regionFiles = RealSeedAnalyzer.getRegionFiles(selectedWorldDirectory);
        if (regionFiles.length == 0) {
            logArea.append("Region dosyasi bulunamadi!\n");
            return;
        }

        int minRX = Integer.MAX_VALUE, maxRX = Integer.MIN_VALUE;
        int minRZ = Integer.MAX_VALUE, maxRZ = Integer.MIN_VALUE;

        for (File f : regionFiles) {
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

            // Cok buyuk alanlari sinirla
            int limitedMinX = Math.max(blockMinX, -1000);
            int limitedMaxX = Math.min(blockMaxX, 1000);
            int limitedMinZ = Math.max(blockMinZ, -1000);
            int limitedMaxZ = Math.min(blockMaxZ, 1000);

            startXSpinner.setValue(limitedMinX);
            endXSpinner.setValue(limitedMaxX);
            startZSpinner.setValue(limitedMinZ);
            endZSpinner.setValue(limitedMaxZ);

            logArea.append(String.format("Arama alani ayarlandi: X[%d ~ %d] Z[%d ~ %d]\n",
                    limitedMinX, limitedMaxX, limitedMinZ, limitedMaxZ));

            if (blockMinX < limitedMinX || blockMaxX > limitedMaxX) {
                logArea.append(String.format("Not: Dunya daha buyuk (X[%d ~ %d] Z[%d ~ %d]), " +
                                "performans icin sinirlandirildi.\n",
                        blockMinX, blockMaxX, blockMinZ, blockMaxZ));
            }
        }
    }

    private void validateSettings() {
        logArea.append("\n=== AYAR DOGRULAMA ===\n");

        // Dunya kontrolu
        if (selectedWorldDirectory == null) {
            logArea.append("X Minecraft dunya klasoru secilmedi!\n");
            logArea.append("  'Gozat...' butonuna tiklayarak bir dunya secin.\n");
        } else {
            boolean isValid = RealSeedAnalyzer.isValidWorldDirectory(selectedWorldDirectory);
            if (isValid) {
                logArea.append("OK Dunya dosyasi analiz icin uygun\n");

                String worldInfo = RealSeedAnalyzer.getWorldDirectoryInfo(selectedWorldDirectory);
                logArea.append(worldInfo);
            } else {
                logArea.append("X Dunya dosyasi gecersiz!\n");
            }
        }

        // Seed bilgisi
        String seedText = seedField.getText().trim();
        if (!seedText.isEmpty()) {
            try {
                long seed = Long.parseLong(seedText);
                logArea.append("OK Seed: " + seed + "\n");
            } catch (NumberFormatException e) {
                long hash = seedText.hashCode();
                logArea.append("OK String seed -> hash: " + hash + "\n");
            }
        } else {
            logArea.append("- Seed belirtilmedi (opsiyonel)\n");
        }

        // Arama alani kontrolu
        int sX = (int) startXSpinner.getValue();
        int eX = (int) endXSpinner.getValue();
        int sZ = (int) startZSpinner.getValue();
        int eZ = (int) endZSpinner.getValue();
        int mY = (int) minYSpinner.getValue();
        int xY = (int) maxYSpinner.getValue();

        if (eX <= sX || eZ <= sZ || xY <= mY) {
            logArea.append("X Gecersiz arama alani! Bitis degerleri baslangicltan buyuk olmali.\n");
        } else {
            int area = (eX - sX + 1) * (eZ - sZ + 1);
            int layers = xY - mY + 1;
            int chunkCount = ((eX - sX) / 16 + 1) * ((eZ - sZ) / 16 + 1);
            logArea.append(String.format("OK Arama alani: %d blok (%dx%d), %d Y katman\n",
                    area, eX - sX + 1, eZ - sZ + 1, layers));
            logArea.append(String.format("OK Taranacak chunk: ~%d adet\n", chunkCount));

            if (area > 1000000) {
                logArea.append("! Buyuk arama alani - islem uzun surebilir\n");
            }
        }

        // Pattern kontrolu
        BlockPattern p = patternPanel.getPattern();
        if (p.isEmpty()) {
            logArea.append("X Blok deseni tanimlanmamis! Pattern panelinden blok yerlestirin.\n");
        } else {
            logArea.append("OK Blok deseni: " + p.getDefinedBlockCount() + " blok tanimli\n");
        }

        logArea.append("===================\n\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void startSearch() {
        // Validasyon
        if (selectedWorldDirectory == null) {
            JOptionPane.showMessageDialog(this,
                    "Lutfen once bir Minecraft dunya klasoru secin!",
                    "Dunya Secilmedi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!RealSeedAnalyzer.isValidWorldDirectory(selectedWorldDirectory)) {
            JOptionPane.showMessageDialog(this,
                    "Secilen dunya klasoru gecersiz!",
                    "Gecersiz Dunya", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BlockPattern p = patternPanel.getPattern();
        if (p.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Lutfen bir blok deseni tanimlayin!\n" +
                            "Pattern panelinden blok yerlestirin veya ornek desen yukleyin.",
                    "Desen Tanimlanmamis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int sX = (int) startXSpinner.getValue();
        int eX = (int) endXSpinner.getValue();
        int sZ = (int) startZSpinner.getValue();
        int eZ = (int) endZSpinner.getValue();
        int mY = (int) minYSpinner.getValue();
        int xY = (int) maxYSpinner.getValue();

        if (eX <= sX || eZ <= sZ || xY <= mY) {
            JOptionPane.showMessageDialog(this,
                    "Gecersiz arama alani! Bitis degerleri baslangicltan buyuk olmali.",
                    "Gecersiz Alan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Arama basla
        isSearching = true;
        searchButton.setEnabled(false);
        stopButton.setEnabled(true);
        browseButton.setEnabled(false);
        resultListModel.clear();

        logArea.append("\n>>> ARAMA BASLADI <<<\n");
        logArea.append(String.format("Alan: X[%d ~ %d] Z[%d ~ %d] Y[%d ~ %d]\n", sX, eX, sZ, eZ, mY, xY));
        logArea.append("Desen: " + p.getDefinedBlockCount() + " blok\n");

        // Arka plan is parcaciginda arama yap
        currentMatcher = new PatternMatcher(selectedWorldDirectory, p,
                sX, eX, sZ, eZ, mY, xY,
                new PatternMatcher.ProgressListener() {
                    @Override
                    public void onProgress(int current, int total, String message) {
                        SwingUtilities.invokeLater(() -> {
                            int percent = total > 0 ? (current * 100 / total) : 0;
                            progressBar.setValue(percent);
                            progressBar.setString(message);
                        });
                    }

                    @Override
                    public void onResult(SearchResult result) {
                        SwingUtilities.invokeLater(() -> {
                            resultListModel.addElement(result.toString());
                            logArea.append("BULUNDU: " + result + "\n");
                            logArea.setCaretPosition(logArea.getDocument().getLength());
                        });
                    }

                    @Override
                    public void onComplete(int totalResults, long elapsedMs) {
                        SwingUtilities.invokeLater(() -> {
                            isSearching = false;
                            searchButton.setEnabled(true);
                            stopButton.setEnabled(false);
                            browseButton.setEnabled(true);

                            double seconds = elapsedMs / 1000.0;
                            String msg = String.format(
                                    "\n>>> ARAMA TAMAMLANDI <<<\nSonuc: %d eslesme bulundu\nSure: %.1f saniye\n\n",
                                    totalResults, seconds);
                            logArea.append(msg);
                            logArea.setCaretPosition(logArea.getDocument().getLength());

                            progressBar.setValue(100);
                            progressBar.setString(String.format("Tamamlandi - %d sonuc (%.1fs)", totalResults, seconds));
                        });
                    }

                    @Override
                    public void onError(String error) {
                        SwingUtilities.invokeLater(() -> {
                            logArea.append("HATA: " + error + "\n");
                        });
                    }
                });

        new Thread(() -> {
            try {
                currentMatcher.search();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    isSearching = false;
                    searchButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    browseButton.setEnabled(true);
                    logArea.append("HATA: " + e.getMessage() + "\n");
                    progressBar.setString("Hata olustu");
                });
            }
        }, "SearchThread").start();
    }

    private void stopSearch() {
        if (currentMatcher != null) {
            currentMatcher.cancel();
            logArea.append("\n--- Arama kullanici tarafindan durduruldu ---\n");
        }
    }

    private void loadSamplePattern(String type) {
        patternPanel.clearPattern();

        switch (type) {
            case "stone":
                patternPanel.setBlockAt(0, 0, 0, "minecraft:stone", 0);
                patternPanel.setBlockAt(1, 0, 0, "minecraft:stone", 0);
                patternPanel.setBlockAt(0, 0, 1, "minecraft:stone", 0);
                patternPanel.setBlockAt(1, 0, 1, "minecraft:stone", 0);
                logArea.append("OK Tas deseni yuklendi (2x2)\n");
                break;

            case "dirt":
                patternPanel.setBlockAt(0, 0, 0, "minecraft:dirt", 0);
                patternPanel.setBlockAt(1, 0, 0, "minecraft:dirt", 0);
                patternPanel.setBlockAt(0, 0, 1, "minecraft:grass_block", 0);
                patternPanel.setBlockAt(1, 0, 1, "minecraft:grass_block", 0);
                logArea.append("OK Toprak/cim deseni yuklendi\n");
                break;

            case "diamond":
                patternPanel.setBlockAt(1, 0, 1, "minecraft:diamond_ore", 0);
                patternPanel.setBlockAt(2, 0, 1, "minecraft:diamond_ore", 0);
                patternPanel.setBlockAt(1, 0, 2, "minecraft:diamond_ore", 0);
                patternPanel.setBlockAt(2, 0, 2, "minecraft:diamond_ore", 0);
                patternPanel.setBlockAt(1, 1, 1, "minecraft:diamond_ore", 0);
                patternPanel.setBlockAt(2, 1, 2, "minecraft:diamond_ore", 0);
                logArea.append("OK Elmas cevheri deseni yuklendi (6 blok, nadir)\n");
                break;

            case "ore_vein":
                patternPanel.setBlockAt(0, 0, 0, "minecraft:iron_ore", 0);
                patternPanel.setBlockAt(1, 0, 0, "minecraft:iron_ore", 0);
                patternPanel.setBlockAt(0, 0, 1, "minecraft:iron_ore", 0);
                patternPanel.setBlockAt(0, 1, 0, "minecraft:iron_ore", 0);
                logArea.append("OK Demir cevheri damari deseni yuklendi (4 blok)\n");
                break;
        }

        patternPanel.setCurrentY(0);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void exportResults() {
        if (resultListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Disa aktarilacak sonuc yok!",
                    "Bos Sonuc", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Sonuclari Kaydet");
        chooser.setSelectedFile(new File("arama_sonuclari.txt"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(chooser.getSelectedFile())) {
                writer.println("Minecraft Seed Analyzer - Arama Sonuclari");
                writer.println("==========================================");
                writer.println("Dunya: " + (selectedWorldDirectory != null ? selectedWorldDirectory.getAbsolutePath() : "N/A"));
                writer.println("Seed: " + seedField.getText());
                writer.println("Tarih: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
                writer.println();

                for (int i = 0; i < resultListModel.size(); i++) {
                    writer.println((i + 1) + ". " + resultListModel.get(i));
                }

                writer.println();
                writer.println("Toplam: " + resultListModel.size() + " sonuc");

                logArea.append("Sonuclar kaydedildi: " + chooser.getSelectedFile().getAbsolutePath() + "\n");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Dosya yazma hatasi: " + e.getMessage(),
                        "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void generateRandomSeed() {
        long seed = (long) (Math.random() * Long.MAX_VALUE);
        seedField.setText(String.valueOf(seed));
        logArea.append("Rastgele seed olusturuldu: " + seed + "\n");
    }

    // === Main ===

    public static void main(String[] args) {
        // Look and Feel ayarla
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Varsayilan L&F kullan
        }

        SwingUtilities.invokeLater(() -> {
            MinecraftSeedAnalyzer app = new MinecraftSeedAnalyzer();
            app.setVisible(true);
        });
    }
}
