package org.asf.edge.mmoserver.io;

import java.util.ArrayList;

import org.asf.edge.mmoserver.entities.coordinates.Quaternion4D;
import org.asf.edge.mmoserver.entities.coordinates.Vector3D;
import org.asf.edge.mmoserver.networking.sfs.SmartfoxPayload;

/**
 * 
 * Sequence Writer Utility
 * 
 * @author Sky Swimmer
 * 
 */
public class SequenceWriter {

	private ArrayList<Object> data = new ArrayList<Object>();

	public SequenceWriter() {
	}

	public SequenceWriter(Object[] start) {
		for (Object o : start)
			data.add(o);
	}

	/**
	 * Retrieves the sequence array
	 * 
	 * @return Object array
	 */
	public Object[] toArray() {
		return data.toArray(t -> new Object[t]);
	}

	/**
	 * Writes an integer value
	 * 
	 * @param value Value to write
	 */
	public void writeInt(int value) {
		data.add(value);
	}

	/**
	 * Writes an long value
	 * 
	 * @param value Value to write
	 */
	public void writeLong(long value) {
		data.add(value);
	}

	/**
	 * Writes an short value
	 * 
	 * @param value Value to write
	 */
	public void writeShort(short value) {
		data.add(value);
	}

	/**
	 * Writes an boolean value
	 * 
	 * @param value Value to write
	 */
	public void writeBoolean(boolean value) {
		data.add(value);
	}

	/**
	 * Writes an byte value
	 * 
	 * @param value Value to write
	 */
	public void writeByte(byte value) {
		data.add(value);
	}

	/**
	 * Writes an byte array value
	 * 
	 * @param value Value to write
	 */
	public void writeByteArray(byte[] value) {
		data.add(value);
	}

	/**
	 * Writes an float value
	 * 
	 * @param value Value to write
	 */
	public void writeFloat(float value) {
		data.add(value);
	}

	/**
	 * Writes an double value
	 * 
	 * @param value Value to write
	 */
	public void writeDouble(double value) {
		data.add(value);
	}

	/**
	 * Writes an string value
	 * 
	 * @param value Value to write
	 */
	public void writeString(String value) {
		data.add(value);
	}

	/**
	 * Writes an object value
	 * 
	 * @param value Value to write
	 */
	public void writeObject(SmartfoxPayload value) {
		data.add(value);
	}

	/**
	 * Writes an vector3 value
	 * 
	 * @param value Value to write
	 */
	public void writeVector3(Vector3D value) {
		data.add(value.x);
		data.add(value.y);
		data.add(value.z);
	}

	/**
	 * Writes an quaternion value
	 * 
	 * @param value Value to write
	 */
	public void writeQuaternion(Quaternion4D value) {
		data.add(value.x);
		data.add(value.y);
		data.add(value.z);
		data.add(value.w);
	}

	/**
	 * Writes a object array value
	 * 
	 * @param value Value to write
	 */
	public void writeObjectArray(Object[] value) {
		data.add(mapObjects(value));
	}

	private Object mapObjects(Object[] value) {
		ArrayList<Object> ar = new ArrayList<Object>();
		for (int i = 0; i < value.length; i++) {
			Object obj = value[i];
			if (obj instanceof Quaternion4D) {
				Quaternion4D quat = (Quaternion4D) obj;
				ar.add(quat.x);
				ar.add(quat.y);
				ar.add(quat.z);
				ar.add(quat.w);
				continue;
			} else if (obj instanceof Vector3D) {
				Vector3D vec = (Vector3D) obj;
				ar.add(vec.x);
				ar.add(vec.y);
				ar.add(vec.z);
				continue;
			} else if (obj instanceof Object[]) {
				obj = mapObjects((Object[]) obj);
			}
			ar.add(obj);
		}
		return ar.toArray(t -> new Object[t]);
	}

}
