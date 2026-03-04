# Minecraft Seed Analyzer

Minecraft dunya dosyalarini dogrudan okuyarak blok deseni arayan, Java Swing tabanli masaustu uygulamasi.

`.minecraft/saves/` altindaki herhangi bir dunyanin `.mca` region dosyalarini parse eder, kullanicinin tanimladigi 3D blok desenlerini cok is parcacikli (multi-threaded) arama motoruyla tarar ve eslesmelerin tam koordinatlarini dondurur.

---

## Desteklenen Minecraft Surumlerl

| Surum Araligi | DataVersion | Format Ozelligi |
|---------------|-------------|-----------------|
| 1.13 - 1.15 | 1519 - 2225 | Palette-based blok depolama, eski bit paketleme (degerler long sinirlarini asabilir) |
| 1.16 - 1.17 | 2529 - 2724 | Yeni bit paketleme (degerler long sinirlarini asmaz) |
| 1.18 - 1.20.x | 2860 - 3700 | Yeni section yapisi (`sections` root altinda), Y=-64 ile Y=320 destek |
| 1.21 - 1.21.4+ | 3837 - 4189+ | 1.18 ile ayni blok depolama formati |

Uygulama DataVersion'i chunk verisinden otomatik algilar ve dogru parse yontemini secer. Yeni surumler icin ek guncelleme gerekmez (format degismedikce).

---

## Ozellikler

### Gercek Dunya Dosyasi Analizi
- Minecraft save klasorundeki `.mca` (Anvil) region dosyalarini dogrudan okur
- Zlib ve GZip sikistirmasini acarak NBT verisini cikarir
- `level.dat` dosyasindan seed, dunya adi, oyun modu ve DataVersion bilgilerini otomatik okur
- Overworld, Nether (`DIM-1`) ve The End (`DIM1`) boyutlarini algilar
- Region dosyalarinin kapsadigi blok alanini hesaplar ve raporlar

### NBT (Named Binary Tag) Parser
Minecraft'in tum veri yapisini tam olarak destekler:
- 13 tag tipi: Byte, Short, Int, Long, Float, Double, ByteArray, String, List, Compound, IntArray, LongArray, End
- Ic ice compound ve list yapilari
- GZip, Zlib ve sikistirilmamis veri okuma

### Chunk Veri Cikarma
- **Palette-based blok depolama**: Her 16x16x16 section kendi blok paleti ve paketlenmis data dizisine sahip
- **Eski paketleme (1.13-1.15)**: Blok indeksleri long sinirlarini asabilir, cross-boundary okuma destegi
- **Yeni paketleme (1.16+)**: Her long `floor(64 / bitsPerBlock)` deger icerir, sinir asmaz
- **Yeni section yapisi (1.18+)**: `sections` root altinda, `block_states > palette + data` formati
- **Eski section yapisi (1.13-1.17)**: `Level > Sections > Palette + BlockStates` formati
- Palette tek elemanli ise (homojen section) data dizisi olmadan dogrudan dondurur

### 4x4x4 3D Blok Deseni Editoru
- Y katmanlari arasinda gecis yaparak (0-3) her katmanda 4x4 grid uzerinde blok yerlestirme
- Sol tik ile blok koyma, sag tik ile kaldirma
- Blok secici dropdown: renkli onizleme ile 46 blok tipi
- Bos birakilan pozisyonlar "joker" olarak davranir (herhangi bir blokla eslesir)
- Hazir ornek desenler: Tas (2x2), Toprak/Cim, Elmas Cevheri (6 blok), Demir Damari (4 blok)

### Cok Is Parcacikli Arama Motoru
- Chunk'lari region dosyalarina gore gruplar
- Her region dosyasini ayri is parcaciginda paralel tarar (`Runtime.availableProcessors()` kadar thread)
- Chunk bazli erken cikis: desendeki ilk eslesmeme aninda o pozisyonu atlar
- Arama sirasinda gercek zamanli ilerleme yuzdeleri ve eslesme sayisi
- Kullanici tarafindan istenildigi an iptal edilebilir
- Chunk sinirlarini asan desenler icin komsuluk yonetimi

### Desteklenen Blok Tipleri (46 adet)

| Kategori | Bloklar |
|----------|---------|
| **Temel** | Hava, Tas, Toprak, Cim Blogu, Kayrak Tasi, Tuf, Bedrock, Obsidyen, Cakil, Kum |
| **Cevherler** | Komur, Demir, Altin, Elmas, Kiziltas, Lapis Lazuli, Zumrut, Bakir |
| **Derin Cevherler (1.17+)** | Derin Komur, Derin Demir, Derin Altin, Derin Elmas, Derin Kiziltas, Derin Lapis, Derin Zumrut, Derin Bakir |
| **Sivi** | Su, Lav |
| **Sculk (1.19+)** | Sculk, Sculk Katalizor, Sculk Cigligi, Sculk Sensor, Camur |
| **1.20+** | Kiraz Kutugu, Suphe Kum, Suphe Cakil |
| **1.21+** | Trial Spawner, Vault, Agir Cekirdek, Crafter, Tuf Tugla, Oyma Tuf, Cilali Tuf, Oyma Bakir, Bakir Izgara, Bakir Ampul |
| **Pale Garden (1.21.2+)** | Soluk Mese, Soluk Yosun, Creaking Heart |

> Not: Blok ID'leri string tabanli oldugu icin listede olmayan bloklar da dunya dosyasinda dogru sekilde okunur. UI listesi sadece hizli secim icindir.

### Diger Ozellikler
- **Otomatik arama alani algilama**: Region dosyalarindan dunyanin kapsadigi alani hesaplar, performans icin makul bir aralik onerir
- **Ayar dogrulama**: Aramaya baslamadan once dunya dizini, desen ve koordinat araligini kontrol eder
- **Sonuc disa aktarma**: Bulunan tum eslesmeleri tarih, dunya bilgisi ve koordinatlarla birlikte `.txt` dosyasina kaydeder
- **OS algilama**: Windows, Linux ve macOS icin `.minecraft/saves` klasorunu otomatik bulur
- **Islem gunlugu**: Tum islemler ve hatalar detayli sekilde log alaninda gosterilir

---

## Kurulum

### Gereksinimler
- Java JDK 8 veya uzeri (JDK 21'e kadar test edilmistir)

### Derleme

**Linux / macOS:**
```bash
./build.sh
```

**Windows:**
```batch
compile.bat
```

**Manuel:**
```bash
mkdir -p bin
javac -encoding UTF-8 -d bin \
    src/main/*.java src/ui/*.java src/models/*.java \
    src/analyzer/*.java src/minecraft/*.java
```

### Calistirma

**Linux / macOS:**
```bash
./run.sh
```

**Windows:**
```batch
run.bat
```

**Manuel:**
```bash
java -cp bin main.MinecraftSeedAnalyzer
```

### Testleri Calistirma
```bash
javac -encoding UTF-8 -d bin src/test/*.java \
    src/main/*.java src/ui/*.java src/models/*.java \
    src/analyzer/*.java src/minecraft/*.java
java -cp bin test.IntegrationTest
```

---

## Kullanim Kilavuzu

### 1. Dunya Secimi
"Gozat..." butonuna tiklayarak Minecraft dunya klasorunuzu secin.
- **Windows**: `%APPDATA%\.minecraft\saves\DunyaAdi`
- **Linux**: `~/.minecraft/saves/DunyaAdi`
- **macOS**: `~/Library/Application Support/minecraft/saves/DunyaAdi`

Secilen klasorde `region/` alt dizini ve en az bir `.mca` dosyasi olmalidir. Uygulama otomatik olarak:
- `level.dat`'tan seed, dunya adi, oyun modu ve DataVersion okur
- Region dosyasi sayisini ve toplam boyutunu gosterir
- Kapsanan blok alanini (X ve Z araligini) hesaplar
- Nether ve The End boyutlarini algilar

### 2. Arama Alani Belirleme
- **X ve Z araligi**: Taranacak blok koordinat araligini girin (varsayilan: -100 ~ 100)
- **Y araligi**: Yukseklik araligini girin (varsayilan: -64 ~ 64)
- **Otomatik Algiyla**: Dunya dosyalarindan mevcut alani hesaplar ve uygun bir aralik onerir. Cok buyuk dunyalar performans icin -1000 ~ 1000 ile sinirlandirilir

### 3. Blok Deseni Tanimlama
Pattern panelinde 4x4x4 boyutunda 3D bir blok deseni olusturun:
- Ust panelden blok tipini secin (renkli onizleme mevcut)
- Grid uzerinde sol tiklayarak blok yerlestirin
- Sag tiklayarak blok kaldirin
- `<< Y` ve `Y >>` butonlariyla katmanlar arasi gecis yapin
- "Temizle" butonu tum deseni sifirlar
- Bos birakilan hucreler herhangi bir blokla eslesir (joker)

**Hazir desenler**: "Ornek Desenler" bolumunden tek tikla yukleyebilirsiniz:
- **Tas**: 2x2 tas blogu
- **Toprak**: Toprak + cim blogu kombinasyonu
- **Elmas**: 6 bloklu elmas cevheri formasyonu
- **Cevher Damari**: 4 bloklu demir cevheri damari

### 4. Arama
"Aramayi Baslat" butonuna tiklayin. Arama sirasinda:
- Progress bar'da yuzdelik ilerleme ve taranan chunk sayisi gosterilir
- Bulunan eslesmeler aninda sonuc listesine eklenir
- Log alaninda detayli islem bilgisi goruntulenir
- "Durdur" butonu ile aramayi istenildigi an iptal edebilirsiniz

### 5. Sonuclar
- Sonuc listesinde her eslesmenin tam koordinatlari (X, Y, Z) ve eslesme yuzdesi gosterilir
- "Disa Aktar" butonu ile tum sonuclari `.txt` dosyasina kaydedebilirsiniz
- "Sonuclari Temizle" butonu listeyi sifirlar

---

## Teknik Mimari

```
src/
├── main/
│   └── MinecraftSeedAnalyzer.java       # Ana uygulama penceresi, Swing GUI, event handling
├── ui/
│   └── PatternPanel.java               # 4x4x4 3D blok deseni editoru, blok secici, renk onizleme
├── models/
│   ├── BlockData.java                  # Blok ID (string) + metadata (int)
│   ├── BlockPattern.java              # 4x4x4 3D blok dizisi, bos pozisyon = joker
│   └── SearchResult.java             # Eslesme koordinatlari (X, Y, Z) + istatistik
├── analyzer/
│   ├── PatternMatcher.java           # Multi-threaded arama motoru, chunk bazli paralel tarama
│   └── RealSeedAnalyzer.java        # Dunya dizini dogrulama, region dosyasi listeleme
├── minecraft/
│   ├── NBTTag.java                  # 13 NBT tag tipi, compound/list erisim metodlari
│   ├── NBTReader.java              # NBT binary format parser (GZip, Zlib, raw)
│   ├── RegionFile.java            # .mca Anvil dosya okuyucu, chunk konum tablosu, decompress
│   └── ChunkReader.java          # Chunk -> blok cikarma, palette-based, 4 farkli format destegi
└── test/
    └── IntegrationTest.java      # Uctan uca entegrasyon testi (NBT, Region, Chunk, PatternMatcher)
```

### Veri Akisi

```
.mca dosyasi
    |
    v
RegionFile: Konum tablosundan chunk offset oku -> Sikistirilmis veriyi oku -> Zlib/GZip decompress
    |
    v
NBTReader: Binary NBT stream -> NBTTag agaci (Compound, List, LongArray vb.)
    |
    v
ChunkReader: NBTTag agaci -> DataVersion'a gore format sec -> Section bul -> Palette + data oku -> Blok ID dondur
    |
    v
PatternMatcher: Her pozisyonda pattern ile karsilastir -> Eslesen koordinatlari raporla
```

### Region Dosyasi Formati (.mca)
```
Offset  Boyut   Icerik
0       4 KB    Konum tablosu (1024 giris x 4 byte: 3 byte offset + 1 byte sektor sayisi)
4096    4 KB    Zaman damgasi tablosu (1024 giris x 4 byte)
8192+   N KB    Chunk veri sektorleri (her biri: 4 byte uzunluk + 1 byte sikistirma + veri)
```
- Her region 32x32 chunk = 512x512 blok alani kapsar
- Chunk indeksi: `(chunkX & 31) + (chunkZ & 31) * 32`
- Sikistirma tipleri: 1 = GZip, 2 = Zlib, 3 = Sikistirilmamis

### Blok Depolama Formati (Palette-based)
```
Section (16x16x16 blok)
├── palette: [{Name: "minecraft:stone"}, {Name: "minecraft:diamond_ore"}, ...]
└── data: long[] (paketlenmis palette indeksleri)
    - bitsPerBlock = max(4, ceil(log2(palette.size)))
    - Blok indeksi: Y*256 + Z*16 + X
    - 1.13-1.15: Degerler long sinirlarini asabilir
    - 1.16+: Her long floor(64/bitsPerBlock) deger icerir, sinir asmaz
```

---

## Lisans

MIT
