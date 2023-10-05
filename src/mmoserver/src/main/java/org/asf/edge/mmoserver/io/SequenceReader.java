package org.asf.edge.mmoserver.io;

import org.asf.edge.common.entities.coordinates.Quaternion4D;
import org.asf.edge.common.entities.coordinates.Vector3D;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

/**
 * 
 * Sequence Reader Utility
 * 
 * @author Sky Swimmer
 * 
 */
public class SequenceReader {
	private Object[] arr;
	private int pos;

	public SequenceReader(Object[] array) {
		this.arr = array;
	}

	/**
	 * Moves to the next element
	 * 
	 * @return True if successful, false otherwise
	 */
	public boolean moveNext() {
		if (hasNext()) {
			pos++;
			return true;
		}
		return false;
	}

	/**
	 * Checks if a next element is available
	 * 
	 * @return True if present
	 */
	public boolean hasNext() {
		return pos < arr.length;
	}

	/**
	 * Assigns the position in the array
	 * 
	 * @param pos New position
	 */
	public void setPosition(int pos) {
		this.pos = pos;
	}

	/**
	 * Retrieves the current position
	 * 
	 * @return Position in array
	 */
	public int getPosition() {
		return pos;
	}

	/**
	 * Retrieves the raw sequence array
	 * 
	 * @return Object array
	 */
	public Object[] getRaw() {
		return arr;
	}

	/**
	 * Reads an integer value
	 * 
	 * @return Integer value
	 */
	public int readInt() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		int v = (int) arr[pos];
		moveNext();
		return v;
	}

	/**
	 * Reads an long value
	 * 
	 * @return Long value
	 */
	public long readLong() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		long v = (long) arr[pos];
		moveNext();
		return v;
	}

	/**
	 * Reads an short value
	 * 
	 * @return Short value
	 */
	public short readShort() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		short v = (short) arr[pos];
		moveNext();
		return v;
	}

	/**
	 * Reads an boolean value
	 * 
	 * @return Boolean value
	 */
	public boolean readBoolean() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		boolean v = (boolean) arr[pos];
		moveNext();
		return v;
	}

	/**
	 * Reads an byte value
	 * 
	 * @return Byte value
	 */
	public byte readByte() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		byte v = (byte) arr[pos];
		moveNext();
		return v;
	}

	/**
	 * Reads an byte array value
	 * 
	 * @return Byte array value
	 */
	public byte[] readByteArray() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		byte[] v = (byte[]) arr[pos];
		moveNext();
		return v;
	}

	/**
	 * Reads an float value
	 * 
	 * @return Float value
	 */
	public float readFloat() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		float v = (float) arr[pos];
		moveNext();
		return v;
	}

	/**
	 * Reads an double value
	 * 
	 * @return Double value
	 */
	public double readDouble() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		double v = (double) arr[pos];
		moveNext();
		return v;
	}

	/**
	 * Reads an string value
	 * 
	 * @return String value
	 */
	public String readString() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		String v = (String) arr[pos];
		moveNext();
		return v;
	}

	/**
	 * Reads an quaternion value
	 * 
	 * @return Quaternion4D value
	 */
	public Quaternion4D readQuaternion() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		return new Quaternion4D(readDouble(), readDouble(), readDouble(), readDouble());
	}

	/**
	 * Reads an vector3 value
	 * 
	 * @return Vector3D value
	 */
	public Vector3D readVector3() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		return new Vector3D(readDouble(), readDouble(), readDouble());
	}

	/**
	 * Reads an object value
	 * 
	 * @return SmartfoxPayload value
	 */
	public SmartfoxPayload readObject() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		SmartfoxPayload v = (SmartfoxPayload) arr[pos];
		moveNext();
		return v;
	}

	/**
	 * Reads an object array value
	 * 
	 * @return Array of Object instances
	 */
	public Object[] readObjectArray() {
		if (!hasNext())
			throw new IllegalArgumentException("No further elements");
		Object[] v = (Object[]) arr[pos];
		moveNext();
		return v;
	}

}
