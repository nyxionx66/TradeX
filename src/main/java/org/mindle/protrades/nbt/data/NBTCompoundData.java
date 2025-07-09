package org.mindle.protrades.nbt.data;

import java.io.Serializable;
import java.util.*;

/**
 * A comprehensive NBT compound data structure that can store all types of NBT data
 * including ProItems custom data and metadata. This class is serializable for 
 * storage and transmission.
 */
public class NBTCompoundData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final Map<String, Object> data;
    
    public NBTCompoundData() {
        this.data = new HashMap<>();
    }
    
    public NBTCompoundData(Map<String, Object> data) {
        this.data = new HashMap<>(data);
    }
    
    // String operations
    public void setString(String key, String value) {
        data.put(key, value);
    }
    
    public String getString(String key) {
        Object value = data.get(key);
        return value instanceof String ? (String) value : null;
    }
    
    // Integer operations
    public void setInteger(String key, Integer value) {
        data.put(key, value);
    }
    
    public Integer getInteger(String key) {
        Object value = data.get(key);
        return value instanceof Integer ? (Integer) value : null;
    }
    
    // Double operations
    public void setDouble(String key, Double value) {
        data.put(key, value);
    }
    
    public Double getDouble(String key) {
        Object value = data.get(key);
        return value instanceof Double ? (Double) value : null;
    }
    
    // Byte operations
    public void setByte(String key, Byte value) {
        data.put(key, value);
    }
    
    public Byte getByte(String key) {
        Object value = data.get(key);
        return value instanceof Byte ? (Byte) value : null;
    }
    
    // Boolean operations (stored as byte)
    public void setBoolean(String key, Boolean value) {
        data.put(key, value);
    }
    
    public Boolean getBoolean(String key) {
        Object value = data.get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }
    
    // Long operations
    public void setLong(String key, Long value) {
        data.put(key, value);
    }
    
    public Long getLong(String key) {
        Object value = data.get(key);
        return value instanceof Long ? (Long) value : null;
    }
    
    // Float operations
    public void setFloat(String key, Float value) {
        data.put(key, value);
    }
    
    public Float getFloat(String key) {
        Object value = data.get(key);
        return value instanceof Float ? (Float) value : null;
    }
    
    // Short operations
    public void setShort(String key, Short value) {
        data.put(key, value);
    }
    
    public Short getShort(String key) {
        Object value = data.get(key);
        return value instanceof Short ? (Short) value : null;
    }
    
    // Byte array operations
    public void setByteArray(String key, byte[] value) {
        data.put(key, value);
    }
    
    public byte[] getByteArray(String key) {
        Object value = data.get(key);
        return value instanceof byte[] ? (byte[]) value : null;
    }
    
    // Integer array operations
    public void setIntegerArray(String key, int[] value) {
        data.put(key, value);
    }
    
    public int[] getIntegerArray(String key) {
        Object value = data.get(key);
        return value instanceof int[] ? (int[]) value : null;
    }
    
    // Long array operations
    public void setLongArray(String key, long[] value) {
        data.put(key, value);
    }
    
    public long[] getLongArray(String key) {
        Object value = data.get(key);
        return value instanceof long[] ? (long[]) value : null;
    }
    
    // String list operations
    public void setStringList(String key, List<String> value) {
        data.put(key, new ArrayList<>(value));
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object value = data.get(key);
        return value instanceof List ? (List<String>) value : null;
    }
    
    // String set operations
    public void setStringSet(String key, Set<String> value) {
        data.put(key, new HashSet<>(value));
    }
    
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key) {
        Object value = data.get(key);
        return value instanceof Set ? (Set<String>) value : null;
    }
    
    // String-Integer map operations (for enchantments)
    public void setStringIntegerMap(String key, Map<String, Integer> value) {
        data.put(key, new HashMap<>(value));
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Integer> getStringIntegerMap(String key) {
        Object value = data.get(key);
        return value instanceof Map ? (Map<String, Integer>) value : null;
    }
    
    // Compound operations
    public void setCompound(String key, NBTCompoundData value) {
        data.put(key, value);
    }
    
    public NBTCompoundData getCompound(String key) {
        Object value = data.get(key);
        return value instanceof NBTCompoundData ? (NBTCompoundData) value : null;
    }
    
    // General operations
    public boolean hasKey(String key) {
        return data.containsKey(key);
    }
    
    public void removeKey(String key) {
        data.remove(key);
    }
    
    public Set<String> getKeys() {
        return new HashSet<>(data.keySet());
    }
    
    public Map<String, Object> getAllData() {
        return new HashMap<>(data);
    }
    
    public boolean isEmpty() {
        return data.isEmpty();
    }
    
    public int size() {
        return data.size();
    }
    
    public void clear() {
        data.clear();
    }
    
    // Utility methods
    public NBTCompoundData copy() {
        return new NBTCompoundData(data);
    }
    
    public void merge(NBTCompoundData other) {
        if (other != null) {
            data.putAll(other.data);
        }
    }
    
    public void putAll(Map<String, Object> values) {
        if (values != null) {
            data.putAll(values);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        NBTCompoundData that = (NBTCompoundData) obj;
        return Objects.equals(data, that.data);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NBTCompoundData{");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            
            sb.append(entry.getKey()).append("=");
            
            Object value = entry.getValue();
            if (value instanceof byte[]) {
                sb.append("byte[").append(((byte[]) value).length).append("]");
            } else if (value instanceof int[]) {
                sb.append("int[").append(((int[]) value).length).append("]");
            } else if (value instanceof long[]) {
                sb.append("long[").append(((long[]) value).length).append("]");
            } else if (value instanceof List) {
                sb.append("List[").append(((List<?>) value).size()).append("]");
            } else if (value instanceof Set) {
                sb.append("Set[").append(((Set<?>) value).size()).append("]");
            } else if (value instanceof Map) {
                sb.append("Map[").append(((Map<?, ?>) value).size()).append("]");
            } else {
                sb.append(value);
            }
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Creates a summary string for logging purposes.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("NBTData[").append(data.size()).append(" entries: ");
        
        List<String> keyTypes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value != null) {
                keyTypes.add(key + "(" + value.getClass().getSimpleName() + ")");
            } else {
                keyTypes.add(key + "(null)");
            }
        }
        
        sb.append(String.join(", ", keyTypes));
        sb.append("]");
        
        return sb.toString();
    }
}