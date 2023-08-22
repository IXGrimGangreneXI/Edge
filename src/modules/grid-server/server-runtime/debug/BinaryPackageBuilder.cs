using Phoenix.Common.IO;
using System.Text;

namespace Phoenix.Debug
{
    /// <summary>
    /// Binary package writer tool
    /// </summary>
    public class BinaryPackageBuilder
    {
        /// <summary>
        /// Package entry information
        /// </summary>
        public class PackageEntry
        {
            public string Key;
            public bool CloseStream;
            public Stream Data;
        }

        private Dictionary<string, PackageEntry> Entries = new Dictionary<string, PackageEntry>();

        /// <summary>
        /// Retrieves a read-only entry dictionary
        /// </summary>
        /// <returns>Read-only entry dictionary</returns>
        public Dictionary<string, PackageEntry> GetCurrentEntries()
        {
            return new Dictionary<string, PackageEntry>(Entries);
        }

        /// <summary>
        /// Adds a stream to write to the binary package
        /// </summary>
        /// <param name="key">Entry key</param>
        /// <param name="dataStream">Data stream</param>
        /// <param name="closeStreamOnFinish">True to close the stream when the entry is written, false otherwise</param>
        public void AddEntry(string key, Stream dataStream, bool closeStreamOnFinish = true)
        {
            string k = key;
            while (k.Contains("//"))
                k = k.Replace("//", "/");
            while (k.EndsWith("/"))
                k = k.Remove(k.LastIndexOf("/"));
            while (k.StartsWith("/"))
                k = k.Substring(1);
            if (Entries.ContainsKey(k))
                throw new ArgumentException("Duplicate key found");
            PackageEntry ent = new PackageEntry();
            ent.Key = k;
            ent.CloseStream = closeStreamOnFinish;
            ent.Data = dataStream;
            Entries[ent.Key] = ent;
        }

        /// <summary>
        /// Writes the package and clears the builder
        /// </summary>
        /// <param name="output">Output stream</param>
        public void Write(Stream output)
        {
            // Pre-process
            long offset = 4;
            foreach (PackageEntry ent in Entries.Values)
            {
                // Get the offset:
                // 4 bytes: length prefix of key
                // <key>
                // 8 bytes: start
                // 8 bytes: end
                byte[] key = Encoding.UTF8.GetBytes(ent.Key);
                int off = 4 + key.Length + 8 + 8;
                offset += off;
            }

            long pos = offset;

            // Write headers
            DataWriter wr = new DataWriter(output);
            wr.WriteInt(Entries.Count);
            foreach (PackageEntry ent in Entries.Values)
            {
                long length = ent.Data.Length;
                wr.WriteString(ent.Key);
                wr.WriteLong(pos);
                wr.WriteLong(pos + length);
                pos += length;
            }

            // Write payload
            foreach (PackageEntry ent in Entries.Values)
            {
                ent.Data.CopyTo(output);
                if (ent.CloseStream)
                    ent.Data.Close();
            }

            Entries.Clear();
        }
    }
}
