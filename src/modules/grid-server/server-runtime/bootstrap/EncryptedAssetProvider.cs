using Phoenix.Common.Logging;
using Phoenix.Server.Bootstrapper.Packages;
using System.Text;

namespace Phoenix.Server.Bootstrapper
{
    public class EncryptedAssetProvider : IAssetProvider
    {
        private BinaryPackage launchPackage;

        public EncryptedAssetProvider(BinaryPackage launchPackage)
        {
            this.launchPackage = launchPackage;
        }

        public string[] GetAssetsIn(string folder)
        {
            // Find assets
            BinaryPackageEntry[] entries = launchPackage.GetEntriesIn("Assets/" + folder);
            return entries.Select(t => t.Key.Substring(t.Key.LastIndexOf("/") + 1)).ToArray();
        }

        public Stream? GetAssetStream(string asset)
        {
            // Find asset ID
            BinaryPackageEntry? idEntry = launchPackage.GetEntry("Assets/" + asset);
            if (idEntry == null)
                return null;
            string assetID = Encoding.UTF8.GetString(Program.ReadBytesFromEntry(idEntry, launchPackage));

            // Verify hash
            Stream data = File.OpenRead("Assets/" + assetID + ".epaf");
            if (!Program.VerifyHash(data, assetID, launchPackage))
            {
                data.Close();
                Console.Error.WriteLine();
                Console.Error.WriteLine("!!!");
                Console.Error.WriteLine("Server files have been tampered with! Shutting down to protect data!");
                Console.Error.WriteLine("!!!");
                Environment.Exit(1);
                return null;
            }

            // Decrypt asset
            data.Position = 0;
            return Program.Decrypt(data, assetID, launchPackage);
        }
    }
}