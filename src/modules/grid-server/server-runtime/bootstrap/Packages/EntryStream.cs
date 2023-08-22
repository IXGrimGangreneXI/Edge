namespace Phoenix.Server.Bootstrapper.Packages
{
    public class EntryStream : Stream
    {
        private long cPos;
        private long _pos;
        private long _max;
        private Stream _delegate;

        public EntryStream(long pos, long max, Stream target)
        {
            _pos = pos;
            cPos = _pos;
            _max = max;
            _delegate = target;
        }

        public override bool CanRead => true;
        public override bool CanSeek => false;
        public override bool CanWrite => false;
        public override long Length => _max - _pos;

        public override long Position { get => cPos - _pos; set => throw new NotImplementedException(); }

        public override void Flush()
        {
        }

        public override int Read(byte[] buffer, int offset, int count)
        {
            int bytesToRead = count;
            if (bytesToRead + cPos >= _max)
                bytesToRead = (int)(_max - cPos);
            int read = _delegate.Read(buffer, offset, bytesToRead);
            cPos += read;
            return read;
        }

        public override int ReadByte()
        {
            if (cPos >= _max)
                return -1;
            int b = _delegate.ReadByte();
            cPos++;
            return b;
        }

        public override void Write(byte[] buffer, int offset, int count)
        {
            throw new NotImplementedException();
        }

        public override long Seek(long offset, SeekOrigin origin)
        {
            throw new NotImplementedException();
        }

        public override void SetLength(long value)
        {
            throw new NotImplementedException();
        }

        public override void Close()
        {
            base.Close();
            _delegate.Close();
        }
    }
}
