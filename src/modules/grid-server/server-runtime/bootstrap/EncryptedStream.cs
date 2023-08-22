using System.Security.Cryptography;

namespace Phoenix.Server.Bootstrapper
{
    internal class EncryptedStream : Stream
    {
        private CryptoStream inner;
        private Stream dataStream;
        private ICryptoTransform transform;
        private Aes cont;

        public EncryptedStream(Stream source, Aes cont, ICryptoTransform transform, CryptoStreamMode mode)
        {
            dataStream = source;
            this.cont = cont;
            this.transform = transform;
            inner = new CryptoStream(source, transform, mode);
        }

        public override bool CanSeek => false;
        public override bool CanRead => inner.CanRead;
        public override bool CanWrite => inner.CanWrite;
        public override long Length => inner.Length;

        public override long Position
        {
            get
            {
                return inner.Position;
            }

            set
            {
                inner.Position = value;
            }
        }

        public override void Close()
        {
            inner.Close();
            transform.Dispose();
            cont.Dispose();
            dataStream.Close();
        }
        
        public override void Flush()
        {
            inner.Flush();
        }

        public override int Read(byte[] buffer, int offset, int count)
        {
            return inner.Read(buffer, offset, count);
        }

        public override long Seek(long offset, SeekOrigin origin)
        {
            return inner.Seek(offset, origin);
        }

        public override void SetLength(long value)
        {
            inner.SetLength(value);
        }

        public override void Write(byte[] buffer, int offset, int count)
        {
            inner.Write(buffer, offset, count);
        }
    }
}