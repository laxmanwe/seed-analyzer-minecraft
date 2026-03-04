package ui;

import models.BlockData;
import models.BlockPattern;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 4x4x4 3D blok deseni editoru.
 * Y katmanlari arasinda gecis yaparak 4x4 grid uzerinde blok yerlestirme imkani sunar.
 */
public class PatternPanel extends JPanel {

    private static final int GRID_SIZE = 4;
    private static final int CELL_SIZE = 60;

    private final BlockPattern pattern;
    private final JButton[][] gridButtons;
    private int currentY = 0;
    private final JLabel yLabel;
    private final JComboBox<String> blockCombo;
    private final JSpinner metadataSpinner;

    // Blok renkleri
    private static final Map<String, Color> BLOCK_COLORS = new LinkedHashMap<>();
    // Blok goruntuleme adlari
    private static final Map<String, String> BLOCK_NAMES = new LinkedHashMap<>();

    static {
        addBlock("minecraft:air", "Hava", new Color(200, 220, 255));
        addBlock("minecraft:stone", "Tas", new Color(128, 128, 128));
        addBlock("minecraft:dirt", "Toprak", new Color(139, 90, 43));
        addBlock("minecraft:grass_block", "Cim Blogu", new Color(86, 153, 53));
        addBlock("minecraft:deepslate", "Kayrak Tasi", new Color(80, 80, 90));
        addBlock("minecraft:tuff", "Tuf", new Color(108, 109, 97));
        addBlock("minecraft:bedrock", "Bedrock", new Color(50, 50, 50));
        addBlock("minecraft:coal_ore", "Komur Cevheri", new Color(55, 55, 55));
        addBlock("minecraft:iron_ore", "Demir Cevheri", new Color(170, 145, 130));
        addBlock("minecraft:gold_ore", "Altin Cevheri", new Color(220, 190, 50));
        addBlock("minecraft:diamond_ore", "Elmas Cevheri", new Color(80, 220, 220));
        addBlock("minecraft:redstone_ore", "Kiziltas Cevheri", new Color(200, 30, 30));
        addBlock("minecraft:lapis_ore", "Lapis Cevheri", new Color(30, 50, 180));
        addBlock("minecraft:emerald_ore", "Zumrut Cevheri", new Color(40, 200, 80));
        addBlock("minecraft:copper_ore", "Bakir Cevheri", new Color(180, 120, 70));
        addBlock("minecraft:deepslate_coal_ore", "Derin Komur", new Color(45, 45, 50));
        addBlock("minecraft:deepslate_iron_ore", "Derin Demir", new Color(130, 115, 110));
        addBlock("minecraft:deepslate_gold_ore", "Derin Altin", new Color(180, 155, 40));
        addBlock("minecraft:deepslate_diamond_ore", "Derin Elmas", new Color(60, 180, 190));
        addBlock("minecraft:deepslate_redstone_ore", "Derin Kiziltas", new Color(160, 25, 30));
        addBlock("minecraft:deepslate_lapis_ore", "Derin Lapis", new Color(25, 40, 150));
        addBlock("minecraft:deepslate_emerald_ore", "Derin Zumrut", new Color(35, 165, 65));
        addBlock("minecraft:deepslate_copper_ore", "Derin Bakir", new Color(145, 100, 60));
        addBlock("minecraft:obsidian", "Obsidyen", new Color(20, 15, 30));
        addBlock("minecraft:gravel", "Cakil", new Color(150, 140, 140));
        addBlock("minecraft:sand", "Kum", new Color(220, 210, 160));
        addBlock("minecraft:water", "Su", new Color(40, 80, 200));
        addBlock("minecraft:lava", "Lav", new Color(220, 100, 20));

        // 1.19+ bloklar
        addBlock("minecraft:mud", "Camur", new Color(60, 55, 55));
        addBlock("minecraft:sculk", "Sculk", new Color(10, 30, 40));
        addBlock("minecraft:sculk_catalyst", "Sculk Katalizor", new Color(15, 45, 55));
        addBlock("minecraft:sculk_shrieker", "Sculk Cigligi", new Color(20, 50, 60));
        addBlock("minecraft:sculk_sensor", "Sculk Sensor", new Color(10, 60, 65));

        // 1.20+ bloklar
        addBlock("minecraft:cherry_log", "Kiraz Kutugu", new Color(180, 100, 120));
        addBlock("minecraft:suspicious_sand", "Suphe Kum", new Color(210, 200, 150));
        addBlock("minecraft:suspicious_gravel", "Suphe Cakil", new Color(140, 130, 130));

        // 1.21+ bloklar
        addBlock("minecraft:trial_spawner", "Trial Spawner", new Color(70, 110, 140));
        addBlock("minecraft:vault", "Vault", new Color(90, 70, 50));
        addBlock("minecraft:heavy_core", "Agir Cekirdek", new Color(55, 50, 60));
        addBlock("minecraft:crafter", "Crafter", new Color(140, 90, 60));
        addBlock("minecraft:tuff_bricks", "Tuf Tugla", new Color(115, 115, 100));
        addBlock("minecraft:chiseled_tuff", "Oyma Tuf", new Color(110, 110, 95));
        addBlock("minecraft:polished_tuff", "Cilali Tuf", new Color(120, 120, 105));
        addBlock("minecraft:chiseled_copper", "Oyma Bakir", new Color(190, 120, 75));
        addBlock("minecraft:copper_grate", "Bakir Izgara", new Color(170, 110, 65));
        addBlock("minecraft:copper_bulb", "Bakir Ampul", new Color(195, 125, 70));

        // 1.21.2+ bloklar (Pale Garden)
        addBlock("minecraft:pale_oak_log", "Soluk Mese", new Color(190, 185, 170));
        addBlock("minecraft:pale_moss_block", "Soluk Yosun", new Color(170, 175, 150));
        addBlock("minecraft:creaking_heart", "Ciritli Kalp", new Color(80, 60, 50));
    }

    private static void addBlock(String id, String name, Color color) {
        BLOCK_COLORS.put(id, color);
        BLOCK_NAMES.put(id, name);
    }

    public PatternPanel() {
        this.pattern = new BlockPattern();
        this.gridButtons = new JButton[GRID_SIZE][GRID_SIZE];

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Blok Deseni (4x4x4)",
                TitledBorder.CENTER,
                TitledBorder.TOP
        ));

        // Ust panel: Blok secici
        JPanel blockSelectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        blockSelectPanel.add(new JLabel("Blok:"));

        String[] blockOptions = new String[BLOCK_NAMES.size()];
        String[] blockIds = new String[BLOCK_NAMES.size()];
        int idx = 0;
        for (Map.Entry<String, String> entry : BLOCK_NAMES.entrySet()) {
            blockIds[idx] = entry.getKey();
            blockOptions[idx] = entry.getValue();
            idx++;
        }

        blockCombo = new JComboBox<>(blockOptions);
        blockCombo.setSelectedIndex(1); // Tas varsayilan
        blockCombo.setRenderer(new BlockListRenderer(blockIds));
        blockSelectPanel.add(blockCombo);

        blockSelectPanel.add(new JLabel("Meta:"));
        metadataSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 15, 1));
        metadataSpinner.setPreferredSize(new Dimension(50, 25));
        blockSelectPanel.add(metadataSpinner);

        add(blockSelectPanel, BorderLayout.NORTH);

        // Orta: 4x4 grid
        JPanel gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE, 2, 2));
        gridPanel.setPreferredSize(new Dimension(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE));

        String[] blockIdArray = BLOCK_NAMES.keySet().toArray(new String[0]);

        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                final int fx = x, fz = z;
                JButton btn = new JButton("Bos");
                btn.setFont(new Font("SansSerif", Font.PLAIN, 10));
                btn.setMargin(new Insets(1, 1, 1, 1));
                btn.setBackground(Color.WHITE);
                btn.setOpaque(true);

                btn.addActionListener(e -> {
                    int selectedIdx = blockCombo.getSelectedIndex();
                    String blockId = blockIdArray[selectedIdx];
                    int meta = (int) metadataSpinner.getValue();

                    BlockData current = pattern.getBlock(fx, currentY, fz);
                    if (current != null && current.getBlockId().equals(blockId)) {
                        // Ayni blok tekrar tiklanirsa temizle
                        pattern.setBlock(fx, currentY, fz, null);
                        btn.setText("Bos");
                        btn.setBackground(Color.WHITE);
                        btn.setForeground(Color.BLACK);
                    } else {
                        pattern.setBlock(fx, currentY, fz, new BlockData(blockId, meta));
                        updateButton(btn, blockId);
                    }
                });

                // Sag tik ile temizle
                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            pattern.setBlock(fx, currentY, fz, null);
                            btn.setText("Bos");
                            btn.setBackground(Color.WHITE);
                            btn.setForeground(Color.BLACK);
                        }
                    }
                });

                gridButtons[x][z] = btn;
                gridPanel.add(btn);
            }
        }

        add(gridPanel, BorderLayout.CENTER);

        // Alt panel: Y katman navigasyonu
        JPanel yPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 2));
        JButton prevY = new JButton("<< Y");
        yLabel = new JLabel("Y Katman: 0");
        yLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        JButton nextY = new JButton("Y >>");

        prevY.addActionListener(e -> {
            if (currentY > 0) {
                currentY--;
                yLabel.setText("Y Katman: " + currentY);
                refreshGrid();
            }
        });

        nextY.addActionListener(e -> {
            if (currentY < GRID_SIZE - 1) {
                currentY++;
                yLabel.setText("Y Katman: " + currentY);
                refreshGrid();
            }
        });

        JButton clearBtn = new JButton("Temizle");
        clearBtn.addActionListener(e -> {
            pattern.clear();
            refreshGrid();
        });

        yPanel.add(prevY);
        yPanel.add(yLabel);
        yPanel.add(nextY);
        yPanel.add(Box.createHorizontalStrut(20));
        yPanel.add(clearBtn);

        add(yPanel, BorderLayout.SOUTH);
    }

    private void updateButton(JButton btn, String blockId) {
        String displayName = BLOCK_NAMES.getOrDefault(blockId, blockId.replace("minecraft:", ""));
        btn.setText(displayName);
        Color color = BLOCK_COLORS.getOrDefault(blockId, Color.LIGHT_GRAY);
        btn.setBackground(color);

        // Koyu arka plan icin beyaz yazi
        double brightness = (color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114) / 1000.0;
        btn.setForeground(brightness < 128 ? Color.WHITE : Color.BLACK);
    }

    private void refreshGrid() {
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                BlockData block = pattern.getBlock(x, currentY, z);
                if (block != null) {
                    updateButton(gridButtons[x][z], block.getBlockId());
                } else {
                    gridButtons[x][z].setText("Bos");
                    gridButtons[x][z].setBackground(Color.WHITE);
                    gridButtons[x][z].setForeground(Color.BLACK);
                }
            }
        }
    }

    public BlockPattern getPattern() {
        return pattern;
    }

    public void setBlockAt(int x, int y, int z, String blockId, int metadata) {
        pattern.setBlock(x, y, z, new BlockData(blockId, metadata));
        if (y == currentY) {
            refreshGrid();
        }
    }

    public void clearPattern() {
        pattern.clear();
        refreshGrid();
    }

    public void setCurrentY(int y) {
        if (y >= 0 && y < GRID_SIZE) {
            currentY = y;
            yLabel.setText("Y Katman: " + currentY);
            refreshGrid();
        }
    }

    /**
     * Blok listesi icin ozel renderer - renk onizleme ile.
     */
    private static class BlockListRenderer extends DefaultListCellRenderer {
        private final String[] blockIds;

        BlockListRenderer(String[] blockIds) {
            this.blockIds = blockIds;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                       int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (index >= 0 && index < blockIds.length) {
                Color color = BLOCK_COLORS.get(blockIds[index]);
                if (color != null && !isSelected) {
                    setBackground(color);
                    double brightness = (color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114) / 1000.0;
                    setForeground(brightness < 128 ? Color.WHITE : Color.BLACK);
                }
            }

            return this;
        }
    }
}
