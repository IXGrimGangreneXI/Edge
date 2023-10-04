package org.asf.edge.mmoserver.networking.sfs;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * Smartfox Network Payload Object
 * 
 * @author Sky Swimmer
 *
 */
public class SmartfoxPayload {
	private Map<String, Object> data = new LinkedHashMap<String, Object>();

	/**
	 * Creates a new empty payload object
	 * 
	 * @return SmartfoxPayload instance
	 */
	public static SmartfoxPayload create() {
		return new SmartfoxPayload();
	}

	/**
	 * Creates a payload object from a smartfox object
	 * 
	 * @param data Smartfox object
	 * @return SmartfoxPayload instance
	 */
	public static SmartfoxPayload fromObject(Map<String, Object> data) {
		SmartfoxPayload payload = create();
		payload.data = data;
		return payload;
	}

	/**
	 * Parses smartfox objects
	 * 
	 * @param data Data to parse
	 * @return SmartfoxPayload instance
	 */
	public static SmartfoxPayload parseSfsObject(byte[] data) throws IOException {
		return fromObject(SmartfoxNetworkObjectUtil.parseSfsObject(data));
	}

	/**
	 * Converts this payload object to a smartfox object
	 * 
	 * @return Smartfox object map
	 */
	public Map<String, Object> toSfsObject() {
		return data;
	}

	/**
	 * Encodes the object as a network smartfox object
	 * 
	 * @return Object bytes
	 * @throws IOException If encoding fails
	 */
	public byte[] encodeToSfsObject() throws IOException {
		return SmartfoxNetworkObjectUtil.encodeSfsObject(toSfsObject());
	}

	/**
	 * Checks if a key is present
	 * 
	 * @param key Value key
	 * @return True if present, false otherwise
	 */
	public boolean has(String key) {
		return data.containsKey(key);
	}

	/**
	 * Retrieves all keys in the payload
	 * 
	 * @return Array of all value keys
	 */
	public String[] getKeys() {
		return data.keySet().toArray(t -> new String[t]);
	}

	/**
	 * Clears the object
	 */
	public void clear() {
		data.clear();
	}

	/**
	 * Removes values
	 * 
	 * @param key Value key to remove
	 */
	public void remove(String key) {
		data.remove(key);
	}

	/**
	 * Retrieves a boolean value
	 * 
	 * @param key Value key
	 * @return Byte value
	 */
	public boolean getBoolean(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (boolean) data.get(key);
	}

	/**
	 * Assigns a boolean value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setBoolean(String key, boolean value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a byte value
	 * 
	 * @param key Value key
	 * @return Byte value
	 */
	public byte getByte(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (byte) data.get(key);
	}

	/**
	 * Assigns a byte value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setByte(String key, byte value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a short value
	 * 
	 * @param key Value key
	 * @return Byte value
	 */
	public short getShort(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (short) data.get(key);
	}

	/**
	 * Assigns a short value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setShort(String key, short value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a integer value
	 * 
	 * @param key Value key
	 * @return Integer value
	 */
	public int getInt(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (int) data.get(key);
	}

	/**
	 * Assigns a integer value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setInt(String key, int value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a long value
	 * 
	 * @param key Value key
	 * @return Long value
	 */
	public long getLong(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (long) data.get(key);
	}

	/**
	 * Assigns a long value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setLong(String key, long value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a float value
	 * 
	 * @param key Value key
	 * @return Float value
	 */
	public float getFloat(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (float) data.get(key);
	}

	/**
	 * Assigns a float value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setFloat(String key, float value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a double value
	 * 
	 * @param key Value key
	 * @return Double value
	 */
	public double getDouble(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (double) data.get(key);
	}

	/**
	 * Assigns a double value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setDouble(String key, double value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a string value
	 * 
	 * @param key Value key
	 * @return String value
	 */
	public String getString(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (String) data.get(key);
	}

	/**
	 * Assigns a string value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setString(String key, String value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a boolean array value
	 * 
	 * @param key Value key
	 * @return Boolean array value
	 */
	public boolean[] getBooleanArray(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (boolean[]) data.get(key);
	}

	/**
	 * Assigns a boolean array value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setBooleanArray(String key, boolean[] value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a short array value
	 * 
	 * @param key Value key
	 * @return Short array value
	 */
	public short[] getShortArray(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (short[]) data.get(key);
	}

	/**
	 * Assigns a short array value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setShortArray(String key, short[] value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a integer array value
	 * 
	 * @param key Value key
	 * @return Integer array value
	 */
	public int[] getIntArray(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (int[]) data.get(key);
	}

	/**
	 * Assigns a integer array value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setIntArray(String key, int[] value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a long array value
	 * 
	 * @param key Value key
	 * @return Long array value
	 */
	public long[] getLongArray(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (long[]) data.get(key);
	}

	/**
	 * Assigns a long array value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setLongArray(String key, long[] value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a float array value
	 * 
	 * @param key Value key
	 * @return Float array value
	 */
	public float[] getFloatArray(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (float[]) data.get(key);
	}

	/**
	 * Assigns a float array value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setFloatArray(String key, float[] value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a double array value
	 * 
	 * @param key Value key
	 * @return Float array value
	 */
	public double[] getDoubleArray(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (double[]) data.get(key);
	}

	/**
	 * Assigns a double array value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setDoubleArray(String key, double[] value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a string array value
	 * 
	 * @param key Value key
	 * @return String array value
	 */
	public String[] getStringArray(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return (String[]) data.get(key);
	}

	/**
	 * Assigns a string array value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setStringArray(String key, String[] value) {
		data.put(key, value);
	}

	/**
	 * Retrieves a object array value
	 * 
	 * @param key Value key
	 * @return Object array value
	 */
	public Object[] getObjectArray(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return mapObjectsForRead((Object[]) data.get(key));
	}

	@SuppressWarnings("unchecked")
	private Object[] mapObjectsForRead(Object[] value) {
		Object[] ar = new Object[value.length];
		int i = 0;
		for (Object obj : value) {
			if (obj instanceof SmartfoxPayload) {
				obj = SmartfoxPayload.fromObject((Map<String, Object>) obj);
			} else if (obj instanceof Object[]) {
				obj = mapObjectsForRead((Object[]) obj);
			}
			ar[i++] = obj;
		}
		return ar;
	}

	/**
	 * Assigns a object array value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setObjectArray(String key, Object[] value) {
		data.put(key, mapObjectsForWrite(value));
	}

	private Object[] mapObjectsForWrite(Object[] value) {
		Object[] ar = new Object[value.length];
		int i = 0;
		for (Object obj : value) {
			if (obj instanceof SmartfoxPayload) {
				obj = ((SmartfoxPayload) obj).toSfsObject();
			} else if (obj instanceof Object[]) {
				obj = mapObjectsForWrite((Object[]) obj);
			}
			ar[i++] = obj;
		}
		return ar;
	}

	/**
	 * Retrieves a object value
	 * 
	 * @param key Value key
	 * @return SmartfoxPayload value
	 */
	@SuppressWarnings("unchecked")
	public SmartfoxPayload getObject(String key) {
		if (!data.containsKey(key))
			throw new IllegalArgumentException("Key " + key + " is not present in object");
		return SmartfoxPayload.fromObject((Map<String, Object>) data.get(key));
	}

	/**
	 * Assigns a object value
	 * 
	 * @param key   Value key
	 * @param value Value to assign
	 */
	public void setObject(String key, SmartfoxPayload value) {
		data.put(key, value.toSfsObject());
	}

}
