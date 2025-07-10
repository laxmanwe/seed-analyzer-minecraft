# Minecraft Seed Analyzer

Java Swing kullanılarak geliştirilmiş Minecraft seed analiz ve blok arama botu.

## Özellikler

- **Seed Girişi**: Numeric veya string seed desteği
- **Minecraft Sürüm Seçimi**: 1.16.x - 1.20.x arası sürümler
- **4x4x4 Blok Deseni Tanımlama**: 3D grid üzerinde blok yerleştirme
- **Blok Rotasyon Desteği**: Her blok için metadata (0-15) değeri
- **Desteklenen Bloklar**:
  - Temel bloklar: Toprak, Taş, Kayrak Taşı, Tüf, Bedrock
  - Tüm cevherler: Kömür, Demir, Altın, Elmas, Kızıltaş, Lapis Lazuli, Zümrüt, Bakır
  - Derin cevher varyantları (1.17+)
- **Arama Alanı Belirleme**: X, Z ve Y koordinat aralıkları
- **Gerçek Zamanlı İlerleme Takibi**: Arama sırasında anlık durum bildirimi
- **Sonuç Listesi**: Bulunan desenlerin koordinatları

## Derleme ve Çalıştırma

### Gereksinimler
- Java JDK 8 veya üzeri
- Java Swing (JDK ile birlikte gelir)

### Derleme
```bash
cd minecraft-seed-analyzer/src
javac -d ../bin main/MinecraftSeedAnalyzer.java ui/*.java models/*.java analyzer/*.java
```

### Çalıştırma
```bash
cd minecraft-seed-analyzer
java -cp bin main.MinecraftSeedAnalyzer
```

## Kullanım

1. **Seed Girişi**: Üst panelde seed değerini girin (örn: 12345 veya "myworld")
2. **Sürüm Seçimi**: Minecraft sürümünü seçin
3. **Arama Alanı**: Başlangıç/bitiş X,Z koordinatları ve Y aralığını belirleyin
4. **Blok Deseni Oluşturma**:
   - Y katmanını seçin (0-3)
   - 4x4 grid üzerinde "Boş" butonlara tıklayın
   - Blok seçin ve rotasyon değerini girin
   - İstediğiniz deseni oluşturun
5. **Aramayı Başlat**: Alt paneldeki "Aramayı Başlat" butonuna tıklayın

## Notlar

- Bu demo versiyonu gerçek Minecraft world generation API'si kullanmamaktadır
- Gerçek bir uygulama için Minecraft'ın chunk data formatını parse eden bir kütüphane gereklidir
- Arama algoritması şu anda simüle edilmiş sonuçlar üretmektedir

## Geliştirme Önerileri

1. **Minecraft World API Entegrasyonu**: 
   - [Minecraft Wiki](https://minecraft.wiki/w/Chunk_format) chunk formatı
   - NBT (Named Binary Tag) parser eklenmeli

2. **Performans İyileştirmeleri**:
   - Multi-threading ile paralel chunk analizi
   - Chunk cache mekanizması

3. **Ek Özellikler**:
   - Sonuçları dosyaya kaydetme
   - Desen şablonlarını yükleme/kaydetme
   - Harita görünümü entegrasyonu
