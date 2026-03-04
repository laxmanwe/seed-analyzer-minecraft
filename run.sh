#!/bin/bash
cd "$(dirname "$0")"

if [ ! -f "bin/main/MinecraftSeedAnalyzer.class" ]; then
    echo "Derlenmis dosyalar bulunamadi! Once derleniyor..."
    ./build.sh
    echo ""
fi

if [ -f "bin/main/MinecraftSeedAnalyzer.class" ]; then
    echo "Uygulama baslatiliyor..."
    java -cp bin main.MinecraftSeedAnalyzer
else
    echo "Derleme basarisiz!"
    exit 1
fi
