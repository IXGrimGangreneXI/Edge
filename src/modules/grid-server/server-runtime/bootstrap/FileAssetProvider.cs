namespace Phoenix.Server.Bootstrapper
{
    public class FileAssetProvider : IAssetProvider
    {
        private string assetDirectory;

        public FileAssetProvider(string assetDirectory)
        {
            this.assetDirectory = assetDirectory;
        }

        public string[] GetAssetsIn(string folder)
        {
            Directory.CreateDirectory(assetDirectory);
            if (Directory.Exists(assetDirectory + "/" + folder))
            {
                List<string> assets = new List<string>();
                foreach (FileInfo file in new DirectoryInfo(assetDirectory + "/" + folder).GetFiles())
                {
                    assets.Add(file.Name);
                }
                return assets.ToArray();
            }
            return new string[0];
         }

        public Stream? GetAssetStream(string asset)
        {
            Directory.CreateDirectory(assetDirectory);
            if (File.Exists(assetDirectory + "/" + asset))
                return File.OpenRead(assetDirectory + "/" + asset);
            else
                return null;
        }
    }
}
