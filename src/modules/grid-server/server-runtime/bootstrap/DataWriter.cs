
using System.Text;

namespace Phoenix.Server.Bootstrapper
{
    /// <summary>
    /// Binary data writer (big-endian)
    /// </summary>
    public class DataWriter
    {
        private Stream Output;

        public DataWriter(Stream output)
        {
            Output = output;
        }

        /// <summary>
        /// Writes a single byte
        /// </summary>
        /// <param name="value">Byte to write</param>
        public void WriteRawByte(byte value)
        {
            Output.WriteByte(value);
        }

        /// <summary>
        /// Writes an array of bytes
        /// </summary>
        /// <param name="value">Bytes to write</param>
        public void WriteRawBytes(byte[] value)
        {
            foreach (byte b in value)
                Output.WriteByte(b);
        }

        /// <summary>
        /// Writes a single integer
        /// </summary>
        /// <param name="value">Integer to write</param>
        public void WriteInt(int value)
        {
            byte[] data = BitConverter.GetBytes(value);
            if (BitConverter.IsLittleEndian)
                Array.Reverse(data);
            WriteRawBytes(data);
        }

        /// <summary>
        /// Writes a single short integer
        /// </summary>
        /// <param name="value">Short to write</param>
        public void WriteShort(short value)
        {
            byte[] data = BitConverter.GetBytes(value);
            if (BitConverter.IsLittleEndian)
                Array.Reverse(data);
            WriteRawBytes(data);
        }

        /// <summary>
        /// Writes a single long integer
        /// </summary>
        /// <param name="value">Long to write</param>
        public void WriteLong(long value)
        {
            byte[] data = BitConverter.GetBytes(value);
            if (BitConverter.IsLittleEndian)
                Array.Reverse(data);
            WriteRawBytes(data);
        }

        /// <summary>
        /// Writes a single float
        /// </summary>
        /// <param name="value">Float to write</param>
        public void WriteFloat(float value)
        {
            byte[] data = BitConverter.GetBytes(value);
            if (BitConverter.IsLittleEndian)
                Array.Reverse(data);
            WriteRawBytes(data);
        }

        /// <summary>
        /// Writes a single double-precision floating-point
        /// </summary>
        /// <param name="value">Double to write</param>
        public void WriteDouble(double value)
        {
            byte[] data = BitConverter.GetBytes(value);
            if (BitConverter.IsLittleEndian)
                Array.Reverse(data);
            WriteRawBytes(data);
        }

        /// <summary>
        /// Writes a single boolean
        /// </summary>
        /// <param name="value">Boolean to write</param>
        public void WriteBoolean(bool value)
        {
            if (value)
                WriteRawByte(1);
            else
                WriteRawByte(0);
        }

        /// <summary>
        /// Writes an array of bytes (length-prefixed)
        /// </summary>
        /// <param name="value">Bytes to write</param>
        public void WriteBytes(byte[] value)
        {
            WriteInt(value.Length);
            WriteRawBytes(value);
        }

        /// <summary>
        /// Writes a single string
        /// </summary>
        /// <param name="value">String to write</param>
        public void WriteString(string value)
        {
            WriteBytes(Encoding.UTF8.GetBytes(value));
        }
    }
}
