namespace Phoenix.Debug.DebugServerRunner.AssetCompilers
{
    /// <summary>
    /// Asset compiler interface
    /// </summary>
    public interface IAssetCompiler
    {
        /// <summary>
        /// File extension (without the .)
        /// </summary>
        public string FileExtension { get; }

        /// <summary>
        /// Compiles the given asset
        /// </summary>
        /// <param name="input">Asset input stream</param>
        /// <param name="asset">Asset file name</param>
        /// <returns>Compiled asset stream</returns>
        public Stream Compile(Stream input, string asset);
    }
}
