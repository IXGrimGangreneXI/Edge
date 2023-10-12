namespace Phoenix.Server.Bootstrapper.Packages
{
    /// <summary>
    /// Binary package tool - Seek-capable archives
    /// </summary>
    public class BinaryPackage
    {
        private string _name;
        private StreamProvider _provider;
        private Stream Stream;

        private Dictionary<string, BinaryPackageEntry> Entries = new Dictionary<string, BinaryPackageEntry>();

        /// <summary>
        /// Used to create streams for reading entries
        /// </summary>
        public delegate Stream StreamProvider();

        /// <summary>
        /// Internal constructor for custom handling of binary packages, requires manual init if you do this
        /// </summary>
        protected BinaryPackage()
        {
        }

        protected void InitManually(Stream stream, string name, StreamProvider provider, Dictionary<string, BinaryPackageEntry> entries)
        {
            _provider = provider;
            _name = name;
            Stream = stream;
            Entries = entries;
        }

        /// <summary>
        /// Loads a binary package
        /// </summary>
        /// <param name="stream">Binary package stream</param>
        /// <param name="name">File name</param>
        public BinaryPackage(Stream stream, string name, StreamProvider provider)
        {
            _provider = provider;
            _name = name;
            Stream = stream;
            if (!stream.CanSeek)
                throw new ArgumentException("stream: requires a seek-capable stream");

            // Prepare to read
            DataReader reader = new DataReader(Stream);
            int length = reader.ReadInt();

            // Read headers
            EntryHeader header = null;
            for (int i = 0; i < length; i++)
            {
                String path = reader.ReadString();
                long start = reader.ReadLong();
                if (header != null)
                {
                    header.end = start - 1;
                    Entries[header.path] = new BinaryPackageEntry(header.path, header.start, header.end);
                }
                header = new EntryHeader();
                header.path = path;
                header.start = start;
            }

            // Read last entry's length
            if (header != null)
            {
                header.end = reader.ReadLong();
                Entries[header.path] = new BinaryPackageEntry(header.path, header.start, header.end);
            }
        }

        private class EntryHeader
        {
            public String path;
            public long start;
            public long end;
        }

        /// <summary>
        /// Retrieves the package file name
        /// </summary>
        public string Name
        {
            get
            {
                return _name;
            }
        }

        /// <summary>
        /// Retrieves all entries in the package
        /// </summary>
        /// <returns></returns>
        public BinaryPackageEntry[] GetEntries()
        {
            return Entries.Values.ToArray();
        }

        public BinaryPackageEntry[] GetEntriesIn(string key)
        {
            string k = key;
            while (k.Contains("//"))
                k = k.Replace("//", "/");
            while (k.EndsWith("/"))
                k = k.Remove(k.LastIndexOf("/"));
            while (k.StartsWith("/"))
                k = k.Substring(1);
            return Entries.Values.Where(t =>
            {
                if (t.Key.StartsWith(k + "/"))
                {
                    if (!t.Key.Substring((k + "/").Length).Contains("/"))
                        return true;
                }
                return false;
            }).ToArray();
        }

        public BinaryPackageEntry? GetEntry(string key)
        {
            string k = key;
            while (k.Contains("//"))
                k = k.Replace("//", "/");
            while (k.EndsWith("/"))
                k = k.Remove(k.LastIndexOf("/"));
            while (k.StartsWith("/"))
                k = k.Substring(1);
            if (!Entries.ContainsKey(k))
                return null;
            else
                return Entries[k];
        }
        
        public virtual EntryStream GetStream(BinaryPackageEntry entry)
        {
            Stream dele = _provider();
            dele.Position = entry.Start;
            return new EntryStream(entry.Start, entry.End, dele);
        }

        public void Close()
        {
            Stream.Close();
            Entries.Clear();
        }
    }
}
