namespace Phoenix.Server.Bootstrapper.Packages
{
    public class BinaryPackageEntry
    {
        private string _key;
        private long _start;
        private long _end;

        public BinaryPackageEntry(string key, long start, long end)
        {
            _key = key;
            _start = start;
            _end = end;
        }

        /// <summary>
        /// Retrieves the entry key
        /// </summary>
        public string Key {
            get
            {
                return _key;
            }
        }

        /// <summary>
        /// Retrieves the start index of the entry payload
        /// </summary>
        public long Start
        {
            get
            {
                return _start;
            }
        }

        /// <summary>
        /// Retrieves the end index of the entry payload
        /// </summary>
        public long End
        {
            get
            {
                return _end;
            }
        }

        /// <summary>
        /// Retrieves the size of the entry payload
        /// </summary>
        public long Size
        {
            get
            {
                return _end - _start;
            }
        }
    }
}
