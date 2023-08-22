using Phoenix.Common;
using Phoenix.Server;

namespace Phoenix.Debug.DebugServerRunner
{
    public class FileAssetProvider : IAssetProvider
    {
        public string[] GetAssetsIn(string folder)
        {
            Directory.CreateDirectory(Game.AssetsFolder);
            if (Directory.Exists(Game.AssetsFolder + "/" + folder))
            {
                List<string> assets = new List<string>();
                foreach (FileInfo file in new DirectoryInfo(Game.AssetsFolder + "/" + folder).GetFiles())
                {
                    assets.Add(file.Name);
                }
                return assets.ToArray();
            }
            return new string[0];
         }

        public Stream? GetAssetStream(string asset)
        {
            Directory.CreateDirectory(Game.AssetsFolder);
            if (File.Exists(Game.AssetsFolder + "/" + asset))
                return AssetCompiler.Compile(File.OpenRead(Game.AssetsFolder + "/" + asset), asset);
            else
                return null;
        }
    }
}
