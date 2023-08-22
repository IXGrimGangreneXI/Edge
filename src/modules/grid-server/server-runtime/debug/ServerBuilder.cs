using Phoenix.Common.IO;
using Phoenix.Common.Logging;
using Phoenix.Server.Components;
using System.IO.Compression;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;

namespace Phoenix.Debug.DebugServerRunner
{
    public class ServerBuilder
    {
        public static void Run(ProjectManifest project, Logger logger, DebugGameDefLib.DebugGameDef game)
        {
            // Build
            logger.Level = LogLevel.INFO;

            // Get assets folder
            string assets = Path.GetFullPath(project.assetsFolder);

            // Log
            logger.Info("Preparing to build server...");

            // Create output
            logger.Debug("Creating output folder...");
            Directory.CreateDirectory("Build/Release");

            // Create assets output folder
            logger.Info("Compiling and encrypting assets...");
            if (Directory.Exists("Build/Release/Assets"))
                Directory.Delete("Build/Release/Assets", true);
            if (Directory.Exists("Build/Release/Components"))
                Directory.Delete("Build/Release/Components", true);
            if (Directory.Exists("Build/Release/TempAssemblyCache"))
                Directory.Delete("Build/Release/TempAssemblyCache", true);

            // Pull all assets and build
            Dictionary<string, byte[]> keys = new Dictionary<string, byte[]>();
            Dictionary<string, byte[]> ivs = new Dictionary<string, byte[]>();
            Dictionary<string, byte[]> hashes = new Dictionary<string, byte[]>();
            Dictionary<string, string> assetFileNames = new Dictionary<string, string>();
            Dictionary<string, string> componentFileNames = new Dictionary<string, string>();
            Dictionary<string, string> assemblyMap = new Dictionary<string, string>();
            if (project.preserveKeys && File.Exists("Build/keycache.bin"))
            {
                // Load keys
                logger.Info("Loading key cache into memory...");
                logger.Info("Beware that while this enables file diffs this decreases security! Disable preserveKeys in the project manifest to disable this feature!");
                logger.Info("Also note that removed files will still be in memory, delete the keycache file to clear the cache.");
                Stream strm = File.OpenRead("Build/keycache.bin");
                DataReader rd = new DataReader(strm);
                int l = rd.ReadInt();
                for (int i = 0; i < l; i++)
                    keys[rd.ReadString()] = rd.ReadBytes();
                l = rd.ReadInt();
                for (int i = 0; i < l; i++)
                    ivs[rd.ReadString()] = rd.ReadBytes();
                l = rd.ReadInt();
                for (int i = 0; i < l; i++)
                    assetFileNames[rd.ReadString()] = rd.ReadString();
                l = rd.ReadInt();
                for (int i = 0; i < l; i++)
                    componentFileNames[rd.ReadString()] = rd.ReadString();
                l = rd.ReadInt();
                for (int i = 0; i < l; i++)
                    assemblyMap[rd.ReadString()] = rd.ReadString();
                strm.Close();
            }
            logger.Debug("Finding assets...");
            AssetCompiler.LogLevel = LogLevel.INFO;
            BuildAssetsIn(assets, "", logger, keys, hashes, ivs, assetFileNames);

            // Build components
            logger.Info("Building server components...");
            logger.Debug("Creating component output directory...");
            Directory.CreateDirectory("Build/Release/TempAssemblyCache");
            Directory.CreateDirectory("Build/Release/Components");

            // Locate component assemblies
            logger.Debug("Locating component assemblies...");
            if (!File.Exists(project.assembliesDirectory + "/" + project.serverAssembly))
                throw new IOException("No compiled assemblies found!");
            AppDomain.CurrentDomain.AssemblyResolve += (sender, args) => {
                string file = new AssemblyName(args.Name).Name;
                if (File.Exists(project.assembliesDirectory + "/" + file + ".dll"))
                    return Assembly.LoadFile(Path.GetFullPath(project.assembliesDirectory + "/" + file + ".dll"));
                else if (File.Exists(project.assembliesDirectory + "/" + file + ".exe"))
                    return Assembly.LoadFile(Path.GetFullPath(project.assembliesDirectory + "/" + file + ".exe"));
                else if (File.Exists(project.assembliesDirectory + "/" + file))
                    return Assembly.LoadFile(Path.GetFullPath(project.assembliesDirectory + "/" + file));
                return null;
            };
            SHA256 hasher = SHA256.Create();
            List<string> componentAssemblies = new List<string>();
            foreach (FileInfo file in new DirectoryInfo(project.assembliesDirectory).GetFiles("*.dll"))
            {
                // Load assembly
                logger.Debug("Searching assembly: " + file.FullName + " for components...");
                Assembly asm = Assembly.LoadFile(file.FullName);

                // Check types for components
                foreach (Type type in asm.GetTypes())
                {
                    if (typeof(Component).IsAssignableFrom(type) && !type.IsAbstract && !type.IsInterface)
                    {
                        componentAssemblies.Add(file.FullName);
                        logger.Info("Building component package for " + file.Name + "...");

                        // Compile assembly to a assembly package
                        // Find all component types
                        List<string> components = new List<string>();
                        foreach (Type t in asm.GetTypes())
                        {
                            if (typeof(Component).IsAssignableFrom(t))
                            {
                                components.Add(t.FullName);
                            }
                        }

                        // Create info binary
                        MemoryStream data = new MemoryStream();
                        DataWriter writer = new DataWriter(data);
                        writer.WriteString(file.Name);
                        writer.WriteString(asm.FullName);
                        writer.WriteInt(components.Count);
                        foreach (string component in components)
                            writer.WriteString(component);
                        
                        // Create component ID
                        string id = Guid.NewGuid().ToString().ToLower();
                        if (!componentFileNames.ContainsValue(file.Name))
                        {
                            while (componentFileNames.ContainsKey(id))
                                id = Guid.NewGuid().ToString().ToLower();
                            componentFileNames[id] = file.Name;
                        }
                        else
                            id = componentFileNames.First(t => t.Value == file.Name).Key;

                        // Setup encryption
                        logger.Debug("Creating AES encryption Key and IV...");
                        using (Aes aes = Aes.Create())
                        {
                            if (keys.ContainsKey(id))
                                aes.Key = keys[id];
                            if (ivs.ContainsKey(id))
                                aes.IV = ivs[id];
                            byte[] key = aes.Key;
                            byte[] iv = aes.IV;
                            keys[id] = key;
                            ivs[id] = (byte[])iv.Clone();
                            logger.Info("  Component ID: " + id);
                            logger.Info("  Component Key: " + string.Concat(key.Select(x => x.ToString("x2"))));
                            logger.Info("  Component IV: " + string.Concat(iv.Select(x => x.ToString("x2"))));
                            Stream s = File.OpenRead(file.FullName);
                            logger.Info("  Component Hash: " + string.Concat(hasher.ComputeHash(s).Select(x => x.ToString("x2"))));
                            s.Close();

                            // Write component
                            logger.Info("  Encrypting and writing component...");
                            Stream outp = File.OpenWrite("Build/Release/Components/" + id + ".epcb");
                            ICryptoTransform tr = aes.CreateEncryptor(key, iv);
                            CryptoStream cryptStream = new CryptoStream(outp, tr, CryptoStreamMode.Write);
                            GZipStream gzip = new GZipStream(cryptStream, CompressionLevel.Fastest);
                            Stream strm = file.OpenRead();
                            strm.CopyTo(gzip);
                            strm.Close();

                            // Close stuff
                            gzip.Close();
                            cryptStream.Close();
                            outp.Close();
                            s = File.OpenRead("Build/Release/Components/" + id + ".epcb");
                            byte[] hash = hasher.ComputeHash(s);
                            hashes[id] = hash;
                            logger.Info("  BPHash: " + string.Concat(hash.Select(x => x.ToString("x2"))));
                            s.Close();
                            logger.Info("Written to Build/Release/Components/" + id + ".epcb");
                        }

                        break;
                    }
                }
            }

            // Compile other assemblies
            logger.Info("Compiling assemblies.mpbp...");
            logger.Info("Compiling server assemblies...");
            logger.Debug("Finding assemblies...");
            List<string> assemblyNames = new List<string>();
            BinaryPackageBuilder package = new BinaryPackageBuilder();
            foreach (FileInfo file in new DirectoryInfo(project.assembliesDirectory).GetFiles("*.dll"))
            {
                if (!componentAssemblies.Contains(file.FullName))
                {
                    assemblyNames.Add(file.Name);

                    // Create component ID
                    string id = Guid.NewGuid().ToString().ToLower();
                    if (!assemblyMap.ContainsValue(file.Name))
                        {
                            while (assemblyMap.ContainsKey(id))
                                id = Guid.NewGuid().ToString().ToLower();
                            assemblyMap[id] = file.Name;
                        }
                        else
                            id = assemblyMap.First(t => t.Value == file.Name).Key;

                    // Encrypt and write
                    using (Aes aes = Aes.Create())
                    {
                        if (keys.ContainsKey("@ASM-" + id))
                            aes.Key = keys["@ASM-" + id];
                        if (ivs.ContainsKey("@ASM-" + id))
                            aes.IV = ivs["@ASM-" + id];
                        byte[] key = aes.Key;
                        byte[] iv = aes.IV;
                        keys["@ASM-" + id] = key;
                        ivs["@ASM-" + id] = (byte[])iv.Clone();

                        // Log
                        logger.Info("Adding " + file.Name + " to assemblies.mpbp...");
                        logger.Info("  Assembly ID: " + id);
                        logger.Info("  Assembly Key: " + string.Concat(key.Select(x => x.ToString("x2"))));
                        logger.Info("  Assembly IV: " + string.Concat(iv.Select(x => x.ToString("x2"))));
                        Stream s = File.OpenRead(file.FullName);
                        logger.Info("  Assembly Hash: " + string.Concat(hasher.ComputeHash(s).Select(x => x.ToString("x2"))));
                        
                                
                        // Encrypt assembly
                        logger.Info("  Encrypting and writing assembly...");
                        Stream outp = File.OpenWrite("Build/Release/TempAssemblyCache/" + id + ".bin");
                        ICryptoTransform tr = aes.CreateEncryptor(key, iv);
                        CryptoStream cryptStream = new CryptoStream(outp, tr, CryptoStreamMode.Write);
                        GZipStream gzip = new GZipStream(cryptStream, CompressionLevel.Fastest);
                        s.Position = 0;
                        s.CopyTo(gzip);

                        // Close stuff
                        s.Close();
                        gzip.Close();
                        cryptStream.Close();
                        outp.Close();

                        // Add entry
                        package.AddEntry("Assemblies/" + id + ".bin", File.OpenRead("Build/Release/TempAssemblyCache/" + id + ".bin"));
                        logger.Info("Added " + file.Name);
                    }
                }
            }

            // Build package
            logger.Info("Compiling manifest...");
            using (Aes aes = Aes.Create())
            {
                // Gen manifest
                logger.Debug("  Compiling assembly manifest...");
                MemoryStream strm = new MemoryStream();
                DataWriter wr = new DataWriter(strm);
                wr.WriteInt(assemblyMap.Count);
                foreach ((string id, string file) in assemblyMap)
                {
                    wr.WriteString(file);
                    wr.WriteString(id);
                    logger.Debug("    Written ID map: " + id + " -> " + file);
                }

                // Gen keys
                logger.Debug("  Creating AES encryption Key and IV...");
                if (keys.ContainsKey("@ASM-@ROOT"))
                    aes.Key = keys["@ASM-@ROOT"];
                if (ivs.ContainsKey("@ASM-@ROOT"))
                    aes.IV = ivs["@ASM-@ROOT"];
                byte[] key = aes.Key;
                byte[] iv = aes.IV;
                keys["@ASM-@ROOT"] = key;
                ivs["@ASM-@ROOT"] = (byte[])iv.Clone();
                logger.Info("  Assembly Manifest Key: " + string.Concat(key.Select(x => x.ToString("x2"))));
                logger.Info("  Assembly Manifest IV: " + string.Concat(iv.Select(x => x.ToString("x2"))));
                
                // Write manifest
                logger.Info("  Encrypting and writing manifest...");
                Stream outp = File.OpenWrite("Build/Release/TempAssemblyCache/manifest.bin");
                ICryptoTransform tr = aes.CreateEncryptor(key, iv);
                CryptoStream cryptStream = new CryptoStream(outp, tr, CryptoStreamMode.Write);
                GZipStream gzip = new GZipStream(cryptStream, CompressionLevel.Fastest);
                gzip.Write(strm.ToArray());

                // Close stuff
                strm.Close();
                gzip.Close();
                cryptStream.Close();
                outp.Close();

                // Add entry
                package.AddEntry("AssemblyManifest", File.OpenRead("Build/Release/TempAssemblyCache/manifest.bin"));
                logger.Info("Added AssemblyManifest");
            }

            // Write package
            logger.Info("Writing assemblies.mpbp...");
            Stream strO = File.OpenWrite("Build/Release/assemblies.mpbp");
            package.Write(strO);
            strO.Close();
            logger.Info("Hashing assemblies.mpbp...");
            Stream st = File.OpenRead("Build/Release/assemblies.mpbp");
            byte[] hashT = hasher.ComputeHash(st);
            hashes["@ROOT"] = hashT;
            logger.Info("  BPHash: " + string.Concat(hashT.Select(x => x.ToString("x2"))));
            st.Close();
            logger.Info("Cleaning...");
            Directory.Delete("Build/Release/TempAssemblyCache", true);
            logger.Info("Written to Build/Release/assemblies.mpbp");

            // Build game asset
            logger.Info("Creating game info asset...");

            logger.Debug("Creating AES encryption Key and IV...");
            using (Aes aes = Aes.Create())
            {
                if (keys.ContainsKey("@GAMEMANIFEST"))
                    aes.Key = keys["@GAMEMANIFEST"];
                if (ivs.ContainsKey("@GAMEMANIFEST"))
                    aes.IV = ivs["@GAMEMANIFEST"];
                byte[] key = aes.Key;
                byte[] iv = aes.IV;
                keys["@GAMEMANIFEST"] = key;
                ivs["@GAMEMANIFEST"] = (byte[])iv.Clone();

                // Build manifest
                logger.Debug("Creating manifest entry...");
                MemoryStream man = new MemoryStream();
                DataWriter writer = new DataWriter(man);
                writer.WriteString(game.gameID);
                writer.WriteString(game.title);
                writer.WriteString(game.version);
                writer.WriteString(game.developmentStage);
                writer.WriteBoolean(game.hasOfflineSupport);
                writer.WriteLong(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()); // Timestamp of build
                writer.WriteString(project.serverClass); // Server type
                writer.WriteString(project.serverAssembly.Replace(".dll", "").Replace(".exe", "")); // Server assembly
                byte[] data = man.ToArray();
                logger.Info("  Asset Key: " + string.Concat(key.Select(x => x.ToString("x2"))));
                logger.Info("  Asset IV: " + string.Concat(iv.Select(x => x.ToString("x2"))));
                logger.Info("  Asset Hash: " + string.Concat(hasher.ComputeHash(data).Select(x => x.ToString("x2"))));

                // Write asset
                logger.Info("  Encrypting and writing asset...");
                Stream outp = File.OpenWrite("Build/Release/game.epaf");
                ICryptoTransform tr = aes.CreateEncryptor(key, iv);
                CryptoStream cryptStream = new CryptoStream(outp, tr, CryptoStreamMode.Write);
                GZipStream gzip = new GZipStream(cryptStream, CompressionLevel.Fastest);
                gzip.Write(data);

                // Close stuff
                gzip.Close();
                cryptStream.Close();
                outp.Close();
                Stream s = File.OpenRead("Build/Release/game.epaf");
                byte[] hash = hasher.ComputeHash(s);
                hashes["@GAMEMANIFEST"] = hash;
                logger.Info("  BPHash: " + string.Concat(hash.Select(x => x.ToString("x2"))));
                s.Close();
                logger.Info("Written to Build/Release/game.epaf");
            }

            // Create shuffled launch binary
            BinaryPackageBuilder launchInfoBin = new BinaryPackageBuilder();
            Dictionary<string, Stream> streams = new Dictionary<string, Stream>();

            // Add keys and ivs
            foreach ((string file, byte[] key) in keys)
                streams["Filekeys/" + file] = new MemoryStream(key);
            foreach ((string file, byte[] iv) in ivs)
                streams["Fileivs/" + file] = new MemoryStream(iv);
            foreach ((string file, byte[] hash) in hashes)
                streams["Filehashes/" + file] = new MemoryStream(hash);
            
            // Add asset file ids
            foreach ((string id, string file) in assetFileNames)
                streams["Assets/" + file] = new MemoryStream(Encoding.UTF8.GetBytes(id));

            // Add component file ids
            foreach ((string id, string file) in componentFileNames)
                streams["Components/" + file] = new MemoryStream(Encoding.UTF8.GetBytes(id));

            // Add regular assemblies
            using (MemoryStream dInfo = new MemoryStream())
            {
                DataWriter writer = new DataWriter(dInfo);
                writer.WriteInt(assemblyNames.Count);
                foreach (string assembly in assemblyNames)
                    writer.WriteString(Path.GetFileName(assembly));
                streams["Assemblies"] = new MemoryStream(dInfo.ToArray());
            }

            // Add component assemblies
            using (MemoryStream dInfo = new MemoryStream())
            {
                DataWriter writer = new DataWriter(dInfo);
                writer.WriteInt(componentAssemblies.Count);
                foreach (string assembly in componentAssemblies)
                    writer.WriteString(Path.GetFileName(assembly));
                streams["ComponentAssemblies"] = new MemoryStream(dInfo.ToArray());
            }

            // Add streams
            Random rnd = new Random();
            while (streams.Count > 0)
            {
                KeyValuePair<string, Stream> strm = streams.ToArray()[rnd.Next(0, streams.Count)];
                launchInfoBin.AddEntry(strm.Key, strm.Value);
                streams.Remove(strm.Key);
            }

            // Build and encrypt
            logger.Info("Writing launch binary...");
            WriteEncryptedLaunchBinary(launchInfoBin);
            logger.Info("Written to Build/Release/launchinfo.bin");

            // Copy server runner
            logger.Info("Copying server runner...");
            if (File.Exists(AssemblyDirectory + "/Phoenix.Server.Bootstrapper.exe"))
                File.Copy(AssemblyDirectory + "/Phoenix.Server.Bootstrapper.exe", "Build/Release/server.exe", true);
            if (File.Exists(AssemblyDirectory + "/Phoenix.Server.Bootstrapper"))
                File.Copy(AssemblyDirectory + "/Phoenix.Server.Bootstrapper", "Build/Release/server", true);
            File.Copy(AssemblyDirectory + "/Phoenix.Server.Bootstrapper.dll", "Build/Release/Phoenix.Server.Bootstrapper.dll", true);
            File.Copy(AssemblyDirectory + "/Phoenix.Server.Bootstrapper.runtimeconfig.json", "Build/Release/Phoenix.Server.Bootstrapper.runtimeconfig.json", true);
            logger.Info("Done!");

            // Cache
            if (project.preserveKeys)
            {
                logger.Info("Writing key cache...");
                logger.Info("Beware that while this enables file diffs this decreases security! Disable preserveKeys in the project manifest to disable this feature!");
                logger.Info("Also note that removed files will still be in memory, delete the keycache file to clear the cache.");
                Stream strm = File.OpenWrite("Build/keycache.bin");
                DataWriter wr = new DataWriter(strm);
                wr.WriteInt(keys.Count);
                foreach ((string k, byte[] v) in keys)
                {
                    wr.WriteString(k);
                    wr.WriteBytes(v);
                }
                wr.WriteInt(ivs.Count);
                foreach ((string k, byte[] v) in ivs)
                {
                    wr.WriteString(k);
                    wr.WriteBytes(v);
                }
                wr.WriteInt(assetFileNames.Count);
                foreach ((string k, string v) in assetFileNames)
                {
                    wr.WriteString(k);
                    wr.WriteString(v);
                }
                wr.WriteInt(componentFileNames.Count);
                foreach ((string k, string v) in componentFileNames)
                {
                    wr.WriteString(k);
                    wr.WriteString(v);
                }
                wr.WriteInt(assemblyMap.Count);
                foreach ((string k, string v) in assemblyMap)
                {
                    wr.WriteString(k);
                    wr.WriteString(v);
                }
                strm.Close();
            }
        }

        // From stackoverflow: https://stackoverflow.com/a/283917
        public static string AssemblyDirectory
        {
            get
            {
                string codeBase = Assembly.GetExecutingAssembly().CodeBase;
                UriBuilder uri = new UriBuilder(codeBase);
                string path = Uri.UnescapeDataString(uri.Path);
                return Path.GetDirectoryName(path);
            }
        }

        private static void BuildAssetsIn(string folder, string pref, Logger logger, Dictionary<string, byte[]> keys, Dictionary<string, byte[]> hashes, Dictionary<string, byte[]> ivs, Dictionary<string, string> assetFileNames)
        {
            // Find assets
            foreach (FileInfo file in new DirectoryInfo(folder).GetFiles())
            {
                if (!Directory.Exists("Build/Release/Assets"))
                {
                    logger.Debug("Creating asset output directory...");
                    Directory.CreateDirectory("Build/Release/Assets");
                }
                logger.Info("Building asset: " + pref + file.Name);

                // Create asset ID
                string id = Guid.NewGuid().ToString().ToLower();
                if (!assetFileNames.ContainsValue(pref + file.Name))
                {
                    while (assetFileNames.ContainsKey(id))
                        id = Guid.NewGuid().ToString().ToLower();
                    assetFileNames[id] = pref + file.Name;
                }
                else
                    id = assetFileNames.First(t => t.Value == pref + file.Name).Key;

                // Setup encryption
                logger.Debug("Creating AES encryption Key and IV...");
                using (Aes aes = Aes.Create())
                {
                    if (keys.ContainsKey(id))
                        aes.Key = keys[id];
                    if (ivs.ContainsKey(id))
                        aes.IV = ivs[id];
                    byte[] key = aes.Key;
                    byte[] iv = aes.IV;
                    keys[id] = key;
                    ivs[id] = (byte[])iv.Clone();
                    SHA256 hasher = SHA256.Create();
                    logger.Info("  Asset ID: " + id);
                    logger.Info("  Asset Key: " + string.Concat(key.Select(x => x.ToString("x2"))));
                    logger.Info("  Asset IV: " + string.Concat(iv.Select(x => x.ToString("x2"))));
                    Stream s = File.OpenRead(file.FullName);
                    logger.Info("  Asset Hash: " + string.Concat(hasher.ComputeHash(s).Select(x => x.ToString("x2"))));
                    s.Close();

                    // Compile asset
                    Logger.GlobalMessagePrefix += "  ";
                    Stream strm = AssetCompiler.Compile(File.OpenRead(file.FullName), pref + file.Name);
                    Logger.GlobalMessagePrefix = Logger.GlobalMessagePrefix.Remove(Logger.GlobalMessagePrefix.Length - 2);

                    // Write asset
                    logger.Info("  Encrypting and writing asset...");
                    Stream outp = File.OpenWrite("Build/Release/Assets/" + id + ".epaf");
                    ICryptoTransform tr = aes.CreateEncryptor(key, iv);
                    CryptoStream cryptStream = new CryptoStream(outp, tr, CryptoStreamMode.Write);
                    GZipStream gzip = new GZipStream(cryptStream, CompressionLevel.Optimal);
                    strm.CopyTo(gzip);

                    // Close stuff
                    gzip.Close();
                    cryptStream.Close();
                    outp.Close();
                    strm.Close();

                    // Hash
                    s = File.OpenRead("Build/Release/Assets/" + id + ".epaf");
                    byte[] hash = hasher.ComputeHash(s);
                    hashes[id] = hash;
                    logger.Info("  BPHash: " + string.Concat(hash.Select(x => x.ToString("x2"))));
                    s.Close();
                    logger.Info("Written to Build/Release/Assets/" + id + ".epaf");
                }
            }
            foreach (DirectoryInfo dir in new DirectoryInfo(folder).GetDirectories())
            {
                // Recurse into subdirectories
                logger.Debug("Finding assets in " + pref + dir.Name + "...");
                BuildAssetsIn(dir.FullName, pref + dir.Name + "/", logger, keys, hashes, ivs, assetFileNames);
            }
        }

        private static Stream LaunchBinaryEncryptionStream(Stream target)
        {
            //
            // Implement this method for better security
            //

            //
            // WE HIGHLY RECOMMEND TO CREATE CUSTOM PROPRIETARY METHODS FOR BEST SECURITY
            //

            // The parameter 'target' contains the output file to which you want to write
            // You want to return some form of encryption stream that encrypts data as it writes

            // Please note that the format MUST be seekable as the server reads the file multiple times and seeks through it to find payload
            // Phoenix has hash checks for asset and component loading however you will need to do signature checks on the launch binary as else the hashses can be modified

            Console.Error.WriteLine();
            Console.Error.WriteLine();
            Console.Error.WriteLine();
            Console.Error.WriteLine("WARNING! Unencrypted launch binary file! Please fork the server builder (debug server runner) and implement this method for security, otherwise your assets will be easy to decrypt!");
            Console.Error.WriteLine("This code is kept in ServerBuilder.cs at the end of the file!");
            Console.Error.WriteLine();
            Console.Error.WriteLine();
            Console.Error.WriteLine();
            return target;
        }

        private static void WriteEncryptedLaunchBinary(BinaryPackageBuilder launchInfoBin)
        {
            //
            // Alternatively to the above, you can replace this writing code to use your own launch binary format with inner encryption
            // launchInfoBin.GetCurrentEntries() will return the collection of entries and their streams as well as if the streams should be closed on build
            //

            //
            // Ordinarily the format is as following:
            //
            // Base format:
            // 4 bytes - count of entries (little-endian signed integer)
            // headers - each entry header below
            // all entry data is added one-by-one after this, no length prefixes, no delimiters, nothing, the headers control how much should be read
            //
            // Entry header format:
            // 4 bytes - length prefix of key
            // string  - key string bytes (UTF-8)
            // 8 bytes - start of payload
            // 8 bytes - end of payload
            //

            //
            // Suggested replacement format:
            //
            // For security we recommend constructing some form of global key that is used to encrypt individual entries in
            // the launch binary, file-wide encryption can break as the file needs to be opened and seeked through multiple times to read keys and file ids
            //

            FileStream output = File.OpenWrite("Build/Release/launchinfo.bin");
            Stream dest = LaunchBinaryEncryptionStream(output);
            launchInfoBin.Write(dest);
            dest.Close();
            output.Close();
        }
    }
}