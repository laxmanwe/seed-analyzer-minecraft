#!/bin/bash
echo "Minecraft Seed Analyzer derleniyor..."

cd "$(dirname "$0")"

# Bin klasorunu olustur
mkdir -p bin

# Java dosyalarini derle
echo "Derleniyor..."
javac -encoding UTF-8 -d bin \
    src/main/*.java \
    src/ui/*.java \
    src/models/*.java \
    src/analyzer/*.java \
    src/minecraft/*.java

if [ $? -ne 0 ]; then
    echo ""
    echo "Derleme hatasi!"
    echo "Java JDK 8+ yuklu oldugundan emin olun."
    exit 1
fi

echo ""
echo "=== DERLEME BASARILI ==="
echo ""
echo "Calistirmak icin: java -cp bin main.MinecraftSeedAnalyzer"
echo "Veya: ./run.sh"
