package org.asf.edge.mmoserver.networking.sfs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import org.asf.edge.common.io.IoUtil;

/**
 * 
 * Smartfox Network Object Utility
 * 
 * @author Sky Swimmer
 *
 */
public class SmartfoxNetworkObjectUtil {

	/**
	 * Parses smartfox objects
	 * 
	 * @param data Data to parse
	 * @return Data map
	 */
	public static Map<String, Object> parseSfsObject(byte[] data) throws IOException {
		ByteArrayInputStream strm = new ByteArrayInputStream(data);

		// Read magic
		if (strm.read() != 18)
			throw new IOException("Invalid packet: magic number invalid");

		// Read
		return parseObject(strm);
	}

	/**
	 * Parses smartfox objects (DOES NOT INCLUDE THE MAGIC)
	 * 
	 * @param data Data to parse
	 * @return Data map
	 */
	public static Map<String, Object> parseObject(InputStream data) throws IOException {
		// Create map
		LinkedHashMap<String, Object> obj = new LinkedHashMap<String, Object>();

		// Parse
		short length = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
		if (length < 0)
			throw new IOException("Invalid length: " + length + ": negative values are invalid for object length");
		for (int i = 0; i < length; i++) {
			// Read string key
			short l = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
			String key = new String(IoUtil.readNBytes(data, l), "UTF-8");

			// Read type
			int type = data.read();

			// Read data
			Object val = decodeVal(type, data);
			obj.put(key, val);
		}

		return obj;
	}

	private static Object decodeVal(int type, InputStream data) throws IOException {
		switch (type) {

		// Null
		case 0:
			return null;

		// Boolean
		case 1:
			return data.read() == 1;

		// Byte
		case 2:
			return (byte) data.read();

		// Short
		case 3:
			return ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();

		// Integer
		case 4:
			return ByteBuffer.wrap(IoUtil.readNBytes(data, 4)).getInt();

		// Long
		case 5:
			return ByteBuffer.wrap(IoUtil.readNBytes(data, 8)).getLong();

		// Float
		case 6:
			return ByteBuffer.wrap(IoUtil.readNBytes(data, 4)).getFloat();

		// Double
		case 7:
			return ByteBuffer.wrap(IoUtil.readNBytes(data, 8)).getDouble();

		// String
		case 8: {
			short l = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
			return new String(IoUtil.readNBytes(data, l), "UTF-8");
		}

		// Boolean array
		case 9: {
			short l = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
			boolean[] b = new boolean[l];
			for (int i2 = 0; i2 < b.length; i2++) {
				b[i2] = data.read() == 1;
			}
			return b;
		}

		// Byte array
		case 10: {
			int ln = ByteBuffer.wrap(IoUtil.readNBytes(data, 4)).getInt();
			return IoUtil.readNBytes(data, ln);
		}

		// Short array
		case 11: {
			short l = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
			short[] b = new short[l];
			for (int i2 = 0; i2 < b.length; i2++) {
				b[i2] = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
			}
			return b;
		}

		// Integer array
		case 12: {
			short l = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
			int[] b = new int[l];
			for (int i2 = 0; i2 < b.length; i2++) {
				b[i2] = ByteBuffer.wrap(IoUtil.readNBytes(data, 4)).getInt();
			}
			return b;
		}

		// Long array
		case 13: {
			short l = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
			long[] b = new long[l];
			for (int i2 = 0; i2 < b.length; i2++) {
				b[i2] = ByteBuffer.wrap(IoUtil.readNBytes(data, 8)).getLong();
			}
			return b;
		}

		// Float array
		case 14: {
			short l = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
			float[] b = new float[l];
			for (int i2 = 0; i2 < b.length; i2++) {
				b[i2] = ByteBuffer.wrap(IoUtil.readNBytes(data, 4)).getFloat();
			}
			return b;
		}

		// Double array
		case 15: {
			short l = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
			double[] b = new double[l];
			for (int i2 = 0; i2 < b.length; i2++) {
				b[i2] = ByteBuffer.wrap(IoUtil.readNBytes(data, 8)).getDouble();
			}
			return b;
		}

		// String array
		case 16: {
			short l = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
			String[] b = new String[l];
			for (int i2 = 0; i2 < b.length; i2++) {
				short l2 = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
				b[i2] = new String(IoUtil.readNBytes(data, l2), "UTF-8");
			}
			return b;
		}

		// Object array
		case 17: {
			short l = ByteBuffer.wrap(IoUtil.readNBytes(data, 2)).getShort();
			Object[] b = new Object[l];
			for (int i2 = 0; i2 < b.length; i2++) {
				b[i2] = decodeVal(data.read(), data);
			}
			return b;
		}

		// Object
		case 18:
		case 19:
			return parseObject(data);

		// Error
		default:
			throw new IOException("Invalid data type " + type);

		}
	}

	/**
	 * Encodes smartfox objects
	 * 
	 * @param obj Smartfox object to encode
	 * @return Object bytes
	 * @throws IOException If encoding fails
	 */
	public static byte[] encodeSfsObject(Map<String, Object> obj) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		// Write magic
		output.write(18);

		// Write
		encodeObject(obj, output);
		return output.toByteArray();
	}

	/**
	 * Encodes smartfox objects (DOES NOT INCLUDE THE MAGIC)
	 * 
	 * @param obj    Smartfox object to encode
	 * @param output Output stream
	 * @throws IOException If encoding fails
	 */
	@SuppressWarnings("unchecked")
	public static void encodeObject(Map<String, Object> obj, OutputStream output) throws IOException {
		int[] types = new int[obj.size()];

		// Compute types
		int i = 0;
		if (obj.size() > Short.MAX_VALUE)
			throw new IOException("Too many values in object, max size is " + Short.MAX_VALUE);
		for (String key : obj.keySet()) {
			if (key.length() > Short.MAX_VALUE)
				throw new IOException("String '" + key + "' too long, max length is " + Short.MAX_VALUE);
			Object value = obj.get(key);

			// Find type
			if (value == null)
				types[i++] = 0;
			else if (value instanceof Boolean)
				types[i++] = 1;
			else if (value instanceof Byte)
				types[i++] = 2;
			else if (value instanceof Short)
				types[i++] = 3;
			else if (value instanceof Integer)
				types[i++] = 4;
			else if (value instanceof Long)
				types[i++] = 5;
			else if (value instanceof Float)
				types[i++] = 6;
			else if (value instanceof Double)
				types[i++] = 7;
			else if (value instanceof String) {
				if (value.toString().length() > Short.MAX_VALUE)
					throw new IOException(
							"String '" + value.toString() + "' too long, max length is " + Short.MAX_VALUE);
				types[i++] = 8;
			} else if (value instanceof boolean[])
				types[i++] = 9;
			else if (value instanceof byte[])
				types[i++] = 10;
			else if (value instanceof short[])
				types[i++] = 11;
			else if (value instanceof int[])
				types[i++] = 12;
			else if (value instanceof long[])
				types[i++] = 13;
			else if (value instanceof float[])
				types[i++] = 14;
			else if (value instanceof double[])
				types[i++] = 15;
			else if (value instanceof String[]) {
				for (String str : (String[]) value)
					if (str.length() > Short.MAX_VALUE)
						throw new IOException("String '" + str + "' too long, max length is " + Short.MAX_VALUE);
				types[i++] = 16;
			} else if (value instanceof Object[])
				types[i++] = 17;
			else if (value instanceof Map) {
				Map<String, Object> mp = (Map<String, Object>) value;
				if (!mp.containsKey("$C") || !mp.containsKey("$F"))
					types[i++] = 18;
				else
					types[i++] = 19;
			} else
				throw new IOException("Unsupported type: " + value.getClass().getTypeName());
		}

		// Encode
		i = 0;
		output.write(ByteBuffer.allocate(2).putShort((short) obj.size()).array());
		for (String key : obj.keySet()) {
			Object value = obj.get(key);

			// Write key
			output.write(ByteBuffer.allocate(2).putShort((short) key.length()).array());
			output.write(key.getBytes("UTF-8"));

			// Write
			writeVal(value, types[i], output);

			// Increase
			i++;
		}
	}

	@SuppressWarnings("unchecked")
	private static void writeVal(Object value, int type, OutputStream output) throws IOException {
		// Write type
		output.write(type);

		// Write object
		switch (type) {

		// Boolean
		case 1: {
			output.write((boolean) value == true ? 1 : 0);
			break;
		}

		// Byte
		case 2: {
			output.write(((byte) value) & 0xff);
			break;
		}

		// Short
		case 3: {
			output.write(ByteBuffer.allocate(2).putShort((short) value).array());
			break;
		}

		// Integer
		case 4: {
			output.write(ByteBuffer.allocate(4).putInt((int) value).array());
			break;
		}

		// Long
		case 5: {
			output.write(ByteBuffer.allocate(8).putLong((long) value).array());
			break;
		}

		// Float
		case 6: {
			output.write(ByteBuffer.allocate(4).putFloat((float) value).array());
			break;
		}

		// Double
		case 7: {
			output.write(ByteBuffer.allocate(8).putDouble((double) value).array());
			break;
		}

		// String
		case 8: {
			// Write length
			output.write(ByteBuffer.allocate(2).putShort((short) value.toString().length()).array());

			// Write data
			output.write(value.toString().getBytes("UTF-8"));
			break;
		}

		// Boolean array
		case 9: {
			// Write length
			boolean[] b = (boolean[]) value;
			output.write(ByteBuffer.allocate(2).putShort((short) b.length).array());

			// Write data
			for (boolean bl : b) {
				output.write((boolean) bl == true ? 1 : 0);
			}
			break;
		}

		// Byte array
		case 10: {
			// Write length
			byte[] b = (byte[]) value;
			output.write(ByteBuffer.allocate(4).putInt(b.length).array());

			// Write data
			output.write(b);
			break;
		}

		// Short array
		case 11: {
			// Write length
			short[] va = (short[]) value;
			output.write(ByteBuffer.allocate(2).putShort((short) va.length).array());

			// Write data
			for (short v : va) {
				output.write(ByteBuffer.allocate(2).putShort(v).array());
			}
			break;
		}

		// Integer array
		case 12: {
			// Write length
			int[] va = (int[]) value;
			output.write(ByteBuffer.allocate(2).putShort((short) va.length).array());

			// Write data
			for (int v : va) {
				output.write(ByteBuffer.allocate(4).putInt(v).array());
			}
			break;
		}

		// Long array
		case 13: {
			// Write length
			long[] va = (long[]) value;
			output.write(ByteBuffer.allocate(2).putShort((short) va.length).array());

			// Write data
			for (long v : va) {
				output.write(ByteBuffer.allocate(8).putLong(v).array());
			}
			break;
		}

		// Float array
		case 14: {
			// Write length
			float[] va = (float[]) value;
			output.write(ByteBuffer.allocate(2).putShort((short) va.length).array());

			// Write data
			for (float v : va) {
				output.write(ByteBuffer.allocate(4).putFloat(v).array());
			}
			break;
		}

		// Double array
		case 15: {
			// Write length
			double[] va = (double[]) value;
			output.write(ByteBuffer.allocate(2).putShort((short) va.length).array());

			// Write data
			for (double v : va) {
				output.write(ByteBuffer.allocate(8).putDouble(v).array());
			}
			break;
		}

		// String array
		case 16: {
			// Write length
			String[] va = (String[]) value;
			output.write(ByteBuffer.allocate(2).putShort((short) va.length).array());

			// Write data
			for (String v : va) {
				// Write length
				output.write(ByteBuffer.allocate(2).putShort((short) v.toString().length()).array());

				// Write data
				output.write(v.toString().getBytes("UTF-8"));
			}
			break;
		}

		// Object array
		case 17: {
			// Write length
			Object[] va = (Object[]) value;
			output.write(ByteBuffer.allocate(2).putShort((short) va.length).array());

			// Write data
			for (Object v : va) {
				// Find type
				int t = 0;
				if (v == null)
					t = 0;
				else if (v instanceof Boolean)
					t = 1;
				else if (v instanceof Byte)
					t = 2;
				else if (v instanceof Short)
					t = 3;
				else if (v instanceof Integer)
					t = 4;
				else if (v instanceof Long)
					t = 5;
				else if (v instanceof Float)
					t = 6;
				else if (v instanceof Double)
					t = 7;
				else if (v instanceof String) {
					if (v.toString().length() > Short.MAX_VALUE)
						throw new IOException(
								"String '" + value.toString() + "' too long, max length is " + Short.MAX_VALUE);
					t = 8;
				} else if (v instanceof boolean[])
					t = 9;
				else if (v instanceof byte[])
					t = 10;
				else if (v instanceof short[])
					t = 11;
				else if (v instanceof int[])
					t = 12;
				else if (v instanceof long[])
					t = 13;
				else if (v instanceof float[])
					t = 14;
				else if (v instanceof double[])
					t = 15;
				else if (v instanceof String[]) {
					for (String str : (String[]) v)
						if (str.length() > Short.MAX_VALUE)
							throw new IOException("String '" + str + "' too long, max length is " + Short.MAX_VALUE);
					t = 16;
				} else if (v instanceof Object[])
					t = 17;
				else if (v instanceof Map) {
					Map<String, Object> mp = (Map<String, Object>) v;
					if (!mp.containsKey("$C") || !mp.containsKey("$F"))
						t = 18;
					else
						t = 19;
				} else
					throw new IOException("Unsupported type: " + value.getClass().getTypeName());
				writeVal(v, t, output);
			}
			break;
		}

		// Object
		case 18:
		case 19: {
			encodeObject((Map<String, Object>) value, output);
			break;
		}

		}
	}

}
