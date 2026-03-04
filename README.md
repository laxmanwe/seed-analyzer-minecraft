# Minecraft Seed Analyzer

Java Swing kullanilarak gelistirilmis Minecraft dunya analiz ve blok arama araci.

Gercek Minecraft dunya dosyalarini (.mca region dosyalari) okuyarak kullanici tarafindan tanimlanan blok desenlerini arar.

## Ozellikler

- **Gercek Dunya Analizi**: Minecraft save dosyalarini dogrudan okur (.mca Anvil formati)
- **NBT Parser**: Minecraft'in Named Binary Tag formatini tam destekler
- **Coklu Format Destegi**: 1.13-1.15 (eski paketleme), 1.16-1.17, 1.18+ (yeni section yapisi)
- **4x4x4 Blok Deseni Tanimlama**: 3D grid uzerinde gorsel blok yerlestirme
- **Cok Is Parcacikli Arama**: Paralel chunk analizi ile hizli tarama
- **Gercek Zamanli Ilerleme**: Arama sirasinda anlik durum bildirimi
- **Otomatik Seed Okuma**: level.dat dosyasindan seed otomatik algilama
- **Otomatik Alan Algilama**: Dunya boyutuna gore arama alani onerisi
- **Sonuc Disa Aktarma**: Bulunan desenleri dosyaya kaydetme
- **Desteklenen Bloklar**:
  - Temel bloklar: Tas, Toprak, Cim, Kayrak Tasi, Tuf, Bedrock, Obsidyen, Cakil, Kum
  - Tum cevherler: Komur, Demir, Altin, Elmas, Kiziltas, Lapis Lazuli, Zumrut, Bakir
  - Derin cevher varyantlari (1.17+)
  - Su, Lav, Hava

## Derleme ve Calistirma

### Gereksinimler
- Java JDK 8 veya uzeri

### Derleme

**Linux / macOS:**
```bash
cd minecraft-seed-analyzer
mkdir -p bin
javac -encoding UTF-8 -d bin src/main/*.java src/ui/*.java src/models/*.java src/analyzer/*.java src/minecraft/*.java
```

**Windows:**
```bash
compile.bat
```

### Calistirma

**Linux / macOS:**
```bash
java -cp bin main.MinecraftSeedAnalyzer
```

**Windows:**
```bash
run.bat
```

## Kullanim

1. **Dunya Secimi**: "Gozat..." butonuna tiklayarak Minecraft dunya klasorunuzu secin
   - Genellikle `.minecraft/saves/` altindaki klasorlerdir
   - Secilen dunya icinde `region/` klasoru ve `.mca` dosyalari olmalidir
2. **Otomatik Bilgiler**: Seed, dunya adi ve diger bilgiler `level.dat`'tan okunur
3. **Arama Alani**: X, Z koordinat araligini ve Y (yukseklik) araligini belirleyin
   - "Otomatik Algiyla" butonu dunya boyutuna gore uygun bir aralik onerir
4. **Blok Deseni Olusturma**:
   - Y katmanini secin (0-3)
   - Blok tipini ust panelden secin
   - 4x4 grid uzerinde tikla yarak blok yerlestirin
   - Sag tiklayarak blok kaldirin
   - Hazir desenleri "Ornek Desenler" butonlarindan yukleyebilirsiniz
5. **Arama**: "Aramayi Baslat" butonuna tiklayin
   - Ilerleme durumu gercek zamanli olarak guncellenir
   - Bulunan eslesmeler aninda sonuc listesine eklenir
   - "Durdur" butonu ile aramayi iptal edebilirsiniz
6. **Sonuclari Kaydetme**: "Disa Aktar" butonu ile sonuclari dosyaya kaydedin

## Teknik Detaylar

### Mimari

```
src/
├── main/
│   └── MinecraftSeedAnalyzer.java    # Ana uygulama ve Swing GUI
├── ui/
│   └── PatternPanel.java            # 4x4x4 blok deseni editoru
├── models/
│   ├── BlockData.java               # Blok tipi + metadata
│   ├── BlockPattern.java            # 4x4x4 desen yapisi
│   └── SearchResult.java            # Arama sonucu koordinatlari
├── analyzer/
│   ├── PatternMatcher.java          # Cok is parcacikli arama motoru
│   └── RealSeedAnalyzer.java        # Dunya dizini dogrulama araclari
└── minecraft/
    ├── NBTTag.java                  # NBT veri yapisi
    ├── NBTReader.java               # NBT format parser
    ├── RegionFile.java              # .mca (Anvil) dosya okuyucu
    └── ChunkReader.java            # Chunk veri cikarici
```

### Region Dosyasi Formati (.mca)

- Her region dosyasi 32x32 chunk icerir (512x512 blok alani)
- Dosya adi: `r.{regionX}.{regionZ}.mca`
- Ilk 8KB: Konum ve zaman damgasi tablolari
- Chunk verisi: Zlib/GZip ile sikistirilmis NBT verisi

### Chunk Veri Formati

- **1.13+**: Palette-based blok depolama
  - Her section (16x16x16) kendi palette ve data dizisine sahip
  - Blok indeksleri long dizisinde paketlenmis (bit-packed)
- **1.16+**: Yeni paketleme - degerler long sinirlarini asmaz
- **1.18+**: Yeni section yapisi, Y=-64 ile Y=320 arasi destek
