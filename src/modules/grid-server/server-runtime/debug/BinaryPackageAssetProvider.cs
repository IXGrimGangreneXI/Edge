using Phoenix.Server;
using Phoenix.Server.Bootstrapper.Packages;
using System.IO.Compression;

namespace Phoenix.Debug.DebugServerRunner
{
    public class BinaryPackageAssetProvider : IAssetProvider
    {
        private string prefix;
        private BinaryPackage package;

        public BinaryPackageAssetProvider(BinaryPackage package, string prefix)
        {
            this.prefix = prefix;
            this.package = package;
        }

        public string[] GetAssetsIn(string folder)
        {
            BinaryPackageEntry[] entries = package.GetEntriesIn(prefix + "/" + folder);
            return entries.Select(t => t.Key.Substring(t.Key.LastIndexOf("/") + 1)).ToArray();
        }

        public Stream? GetAssetStream(string asset)
        {
            BinaryPackageEntry? ent = package.GetEntry(prefix + "/" + asset);
            if (ent != null)
                return new GZipStream(package.GetStream(ent), CompressionMode.Decompress);
            return null;
        }
    }
}
