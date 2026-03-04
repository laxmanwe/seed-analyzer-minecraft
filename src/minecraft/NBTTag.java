package minecraft;

import java.util.*;

/**
 * Minecraft NBT (Named Binary Tag) veri yapisi.
 * Minecraft dunyalarinin temel veri formatidir.
 */
public class NBTTag {
    public static final byte TAG_END = 0;
    public static final byte TAG_BYTE = 1;
    public static final byte TAG_SHORT = 2;
    public static final byte TAG_INT = 3;
    public static final byte TAG_LONG = 4;
    public static final byte TAG_FLOAT = 5;
    public static final byte TAG_DOUBLE = 6;
    public static final byte TAG_BYTE_ARRAY = 7;
    public static final byte TAG_STRING = 8;
    public static final byte TAG_LIST = 9;
    public static final byte TAG_COMPOUND = 10;
    public static final byte TAG_INT_ARRAY = 11;
    public static final byte TAG_LONG_ARRAY = 12;

    private final byte type;
    private final String name;
    private Object value;

    public NBTTag(byte type, String name, Object value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public byte getType() { return type; }
    public String getName() { return name; }
    public Object getValue() { return value; }

    // Compound tag islemleri
    @SuppressWarnings("unchecked")
    public NBTTag getCompound(String key) {
        if (type != TAG_COMPOUND) return null;
        Map<String, NBTTag> map = (Map<String, NBTTag>) value;
        return map.get(key);
    }

    @SuppressWarnings("unchecked")
    public boolean hasKey(String key) {
        if (type != TAG_COMPOUND) return false;
        Map<String, NBTTag> map = (Map<String, NBTTag>) value;
        return map.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getKeys() {
        if (type != TAG_COMPOUND) return Collections.emptySet();
        Map<String, NBTTag> map = (Map<String, NBTTag>) value;
        return map.keySet();
    }

    // Deger okuma metodlari
    public byte getByte() {
        if (type == TAG_BYTE) return (Byte) value;
        return 0;
    }

    public short getShort() {
        if (type == TAG_SHORT) return (Short) value;
        return 0;
    }

    public int getInt() {
        if (type == TAG_INT) return (Integer) value;
        if (type == TAG_BYTE) return (Byte) value;
        if (type == TAG_SHORT) return (Short) value;
        return 0;
    }

    public long getLong() {
        if (type == TAG_LONG) return (Long) value;
        if (type == TAG_INT) return (Integer) value;
        return 0;
    }

    public float getFloat() {
        if (type == TAG_FLOAT) return (Float) value;
        return 0;
    }

    public double getDouble() {
        if (type == TAG_DOUBLE) return (Double) value;
        return 0;
    }

    public String getString() {
        if (type == TAG_STRING) return (String) value;
        return "";
    }

    public byte[] getByteArray() {
        if (type == TAG_BYTE_ARRAY) return (byte[]) value;
        return new byte[0];
    }

    public int[] getIntArray() {
        if (type == TAG_INT_ARRAY) return (int[]) value;
        return new int[0];
    }

    public long[] getLongArray() {
        if (type == TAG_LONG_ARRAY) return (long[]) value;
        return new long[0];
    }

    // List islemleri
    @SuppressWarnings("unchecked")
    public List<NBTTag> getList() {
        if (type == TAG_LIST) return (List<NBTTag>) value;
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public int getListSize() {
        if (type == TAG_LIST) return ((List<NBTTag>) value).size();
        return 0;
    }

    @SuppressWarnings("unchecked")
    public NBTTag getListItem(int index) {
        if (type != TAG_LIST) return null;
        List<NBTTag> list = (List<NBTTag>) value;
        if (index >= 0 && index < list.size()) return list.get(index);
        return null;
    }

    @Override
    public String toString() {
        return "NBTTag{" + getTypeName(type) + ", name='" + name + "'}";
    }

    public static String getTypeName(byte type) {
        switch (type) {
            case TAG_END: return "TAG_End";
            case TAG_BYTE: return "TAG_Byte";
            case TAG_SHORT: return "TAG_Short";
            case TAG_INT: return "TAG_Int";
            case TAG_LONG: return "TAG_Long";
            case TAG_FLOAT: return "TAG_Float";
            case TAG_DOUBLE: return "TAG_Double";
            case TAG_BYTE_ARRAY: return "TAG_Byte_Array";
            case TAG_STRING: return "TAG_String";
            case TAG_LIST: return "TAG_List";
            case TAG_COMPOUND: return "TAG_Compound";
            case TAG_INT_ARRAY: return "TAG_Int_Array";
            case TAG_LONG_ARRAY: return "TAG_Long_Array";
            default: return "Unknown(" + type + ")";
        }
    }
}
