using Phoenix.Common.IO;
using System.Text;

namespace Phoenix.Debug.DebugServerRunner.AssetCompilers
{
    public class ChartCompiler : IAssetCompiler
    {
        public string FileExtension => "ccf";

        public Stream Compile(Stream input, string file)
        {
            // Read asset
            DataReader reader = new DataReader(input);
            byte[] asset = reader.ReadAllBytes();
            input.Close();
            string script = Encoding.UTF8.GetString(asset).Replace("\r", "").Replace("\t", "    ");

            // Compile asset
            MemoryStream buffer = new MemoryStream();
            ChartZ.Compiler.Program.Compile(script.Split('\n'), file, buffer);

            // Return compiled
            return new MemoryStream(buffer.ToArray());
        }
    }
}
