using System.Text;

namespace Phoenix.Server.Bootstrapper
{
    /// <summary>
    /// Binary data reader (big-endian)
    /// </summary>
    public class DataReader
    {
        private Stream Input;
        public DataReader(Stream input) {
            Input = input;
        }

        /// <summary>
        /// Retrieves the source input byte stream
        /// </summary>
        /// <returns>Stream instance</returns>
        public Stream GetStream() {
            return Input;
        }

        /// <summary>
        /// Reads a single byte
        /// </summary>
        /// <returns>Byte value</returns>
        public byte ReadRawByte() {
            return (byte)Input.ReadByte();
        }

        /// <summary>
        /// Reads an amount of bytes
        /// </summary>
        /// <param name="count">Byte count to read</param>
        /// <returns></returns>
        public byte[] ReadNBytes(int count) {
            byte[] data = new byte[count];
            for (int i = 0; i < count; i++) {
                int ib = Input.ReadByte();
                if (ib == -1)
                    throw new IOException("Stream closed");
                data[i] = (byte)ib;
            }
            return data;
        }

        /// <summary>
        /// Reads all remaining bytes
        /// </summary>
        /// <returns></returns>
        public byte[] ReadAllBytes() {
            List<byte> data = new List<byte>();
            while (true)            {
                int ib = Input.ReadByte();
                if (ib == -1)
                    break;
                data.Add((byte)ib);
            }
            return data.ToArray();
        }

        /// <summary>
        /// Reads a single integer
        /// </summary>
        /// <returns>Integer value</returns>
        public int ReadInt() {
            byte[] data = ReadNBytes(4);
            if (BitConverter.IsLittleEndian)
                Array.Reverse(data);
            return BitConverter.ToInt32(data, 0);
        }

        /// <summary>
        /// Reads a single short integer
        /// </summary>
        /// <returns>Short value</returns>
        public short ReadShort() {
            byte[] data = ReadNBytes(2);
            if (BitConverter.IsLittleEndian)
                Array.Reverse(data);
            return BitConverter.ToInt16(data, 0);
        }

        /// <summary>
        /// Reads a single long integer
        /// </summary>
        /// <returns>Long value</returns>
        public long ReadLong() {
            byte[] data = ReadNBytes(8);
            if (BitConverter.IsLittleEndian)
                Array.Reverse(data);
            return BitConverter.ToInt64(data, 0);
        }

        /// <summary>
        /// Reads a single floating-point
        /// </summary>
        /// <returns>Float value</returns>
        public float ReadFloat() {
            byte[] data = ReadNBytes(4);
            if (BitConverter.IsLittleEndian)
                Array.Reverse(data);
            return BitConverter.ToSingle(data, 0);
        }

        /// <summary>
        /// Reads a single double-precision floating-point
        /// </summary>
        /// <returns>Double value</returns>
        public double ReadDouble() {
            byte[] data = ReadNBytes(8);
            if (BitConverter.IsLittleEndian)
                Array.Reverse(data);
            return BitConverter.ToDouble(data, 0);
        }

        /// <summary>
        /// Reads a single boolean
        /// </summary>
        /// <returns>Bool value</returns>
        public bool ReadBoolean() {
            if (ReadRawByte() == 1)
                return true;
            else
                return false;
        }

        /// <summary>
        /// Reads a length-prefixed byte array
        /// </summary>
        /// <returns>Array of bytes</returns>
        public byte[] ReadBytes() {
            return ReadNBytes(ReadInt());
        }

        /// <summary>
        /// Reads a single string
        /// </summary>
        /// <returns>String valie</returns>
        public string ReadString() {
            return Encoding.UTF8.GetString(ReadBytes());
        }

    }
}
