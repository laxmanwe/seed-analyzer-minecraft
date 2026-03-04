package minecraft;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Minecraft NBT (Named Binary Tag) format parser.
 * Hem GZIP hem de zlib sIkIstIrma destegi.
 */
public class NBTReader {

    /**
     * GZIP ile sIkIstIrIlmIs NBT dosyasInI okur (level.dat vb.)
     */
    public static NBTTag readGzipFile(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(
                        new GZIPInputStream(new FileInputStream(file))))) {
            return readNamedTag(dis);
        }
    }

    /**
     * Zlib ile sIkIstIrIlmIs veriyi okur (chunk data)
     */
    public static NBTTag readZlib(byte[] data) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(
                        new InflaterInputStream(new ByteArrayInputStream(data))))) {
            return readNamedTag(dis);
        }
    }

    /**
     * GZIP ile sIkIstIrIlmIs veriyi okur
     */
    public static NBTTag readGzip(byte[] data) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(
                        new GZIPInputStream(new ByteArrayInputStream(data))))) {
            return readNamedTag(dis);
        }
    }

    /**
     * SIkIstIrIlmamIs NBT verisini okur
     */
    public static NBTTag readUncompressed(byte[] data) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(data))) {
            return readNamedTag(dis);
        }
    }

    /**
     * Isimli bir NBT etiketini okur.
     */
    private static NBTTag readNamedTag(DataInputStream dis) throws IOException {
        byte type = dis.readByte();
        if (type == NBTTag.TAG_END) {
            return new NBTTag(NBTTag.TAG_END, "", null);
        }

        String name = readString(dis);
        Object value = readPayload(dis, type);
        return new NBTTag(type, name, value);
    }

    /**
     * Verilen tipe gore NBT degerini okur.
     */
    private static Object readPayload(DataInputStream dis, byte type) throws IOException {
        switch (type) {
            case NBTTag.TAG_BYTE:
                return dis.readByte();

            case NBTTag.TAG_SHORT:
                return dis.readShort();

            case NBTTag.TAG_INT:
                return dis.readInt();

            case NBTTag.TAG_LONG:
                return dis.readLong();

            case NBTTag.TAG_FLOAT:
                return dis.readFloat();

            case NBTTag.TAG_DOUBLE:
                return dis.readDouble();

            case NBTTag.TAG_BYTE_ARRAY: {
                int length = dis.readInt();
                byte[] data = new byte[length];
                dis.readFully(data);
                return data;
            }

            case NBTTag.TAG_STRING:
                return readString(dis);

            case NBTTag.TAG_LIST: {
                byte listType = dis.readByte();
                int length = dis.readInt();
                List<NBTTag> list = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    Object val = readPayload(dis, listType);
                    list.add(new NBTTag(listType, "", val));
                }
                return list;
            }

            case NBTTag.TAG_COMPOUND: {
                Map<String, NBTTag> map = new LinkedHashMap<>();
                while (true) {
                    NBTTag tag = readNamedTag(dis);
                    if (tag.getType() == NBTTag.TAG_END) break;
                    map.put(tag.getName(), tag);
                }
                return map;
            }

            case NBTTag.TAG_INT_ARRAY: {
                int length = dis.readInt();
                int[] data = new int[length];
                for (int i = 0; i < length; i++) {
                    data[i] = dis.readInt();
                }
                return data;
            }

            case NBTTag.TAG_LONG_ARRAY: {
                int length = dis.readInt();
                long[] data = new long[length];
                for (int i = 0; i < length; i++) {
                    data[i] = dis.readLong();
                }
                return data;
            }

            default:
                throw new IOException("Bilinmeyen NBT tag tipi: " + type);
        }
    }

    /**
     * UTF-8 string okur (2 byte uzunluk + veri).
     */
    private static String readString(DataInputStream dis) throws IOException {
        short length = dis.readShort();
        if (length <= 0) return "";
        byte[] data = new byte[length];
        dis.readFully(data);
        return new String(data, "UTF-8");
    }
}
