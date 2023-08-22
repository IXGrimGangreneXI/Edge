using Phoenix.Common.Logging;
using Phoenix.Server.Bootstrapper;
using Phoenix.Server.Bootstrapper.Packages;
using System.IO.Compression;
using System.Reflection;

namespace Phoenix.Server
{
    /// <summary>
    /// Phoenix Mod Manager - Exposed only to mods and to the server only at runtime, not to be used for regular game development
    /// </summary>
    public class ModManager
    {
        private bool locked;
        private List<ModInfo> mods = new List<ModInfo>();
        private static Dictionary<string, Assembly> assemblyCache = new Dictionary<string, Assembly>();
        private Logger logger = Logger.GetLogger("Mod Manager");

        /// <summary>
        /// Locks the mod manager (internal)
        /// </summary>
        public void Lock()
        {
            locked = true;
        }

        /// <summary>
        /// Loads a mod
        /// </summary>
        /// <param name="mod">Mod file stream</param>
        public ModInfo Load(FileStream mod)
        {
            if (locked)
                throw new ArgumentException("Locked");
            BinaryPackage package = new BinaryPackage(mod, Path.GetFileName(mod.Name), () => File.OpenRead(mod.Name));

            // Read mod information
            BinaryPackageEntry? ent = package.GetEntry("manifest.bin");
            if (ent == null)
            {
                logger.Fatal("Invalid mod package: " + package.Name + ": missing mod manifest file!");
                Environment.Exit(1);
                return null;
            }
            Stream binStrm = package.GetStream(ent);
            DataReader rd = new DataReader(binStrm);
            string id = rd.ReadString();
            string version = rd.ReadString();
            binStrm.Close();
            logger.Info("Loading mod: " + id + ", version: " + version);
            ModInfo? i = GetMod(id);
            if (i != null)
            {
                logger.Fatal("Duplicate mod found!\n" +
                    "Mod id: " + id + "\n\n" +
                    "Mod file: " + i.Package.Name + "\n" +
                    "Version: " + i.Version + "\n\n" +
                    "Mod file: " + mod.Name + "\n"
                    + "Version: " + version);
                Environment.Exit(1);
                return null;
            }

            // Read mod assembly into memory
            logger.Debug("Loading mod assembly...");
            ent = package.GetEntry("assembly.bin");
            if (ent == null)
            {
                logger.Fatal("Invalid mod package: " + package.Name + ": missing mod assembly file!");
                Environment.Exit(1);
                return null;
            }
            binStrm = new GZipStream(package.GetStream(ent), CompressionMode.Decompress);
            MemoryStream data = new MemoryStream();
            binStrm.CopyTo(data);
            Assembly modAsm = Assembly.Load(data.ToArray());
            AppDomain.CurrentDomain.AssemblyResolve += (sender, args) =>
            {
                AssemblyName nm = new AssemblyName(args.Name);
                if (nm.FullName == modAsm.GetName().FullName)
                    return modAsm;
                else
                {
                    // Check dependencies
                    lock (assemblyCache)
                    {
                        if (assemblyCache.ContainsKey(args.Name))
                            return assemblyCache[args.Name];
                    }

                    // Find in package
                    BinaryPackageEntry? ent = package.GetEntry("Dependencies/" + nm.Name + ".dll");
                    if (ent != null)
                    {
                        // Load assembly
                        GZipStream binStrm = new GZipStream(package.GetStream(ent), CompressionMode.Decompress);
                        MemoryStream data = new MemoryStream();
                        binStrm.CopyTo(data);
                        Assembly asm = Assembly.Load(data.ToArray());
                        lock (assemblyCache)
                            assemblyCache[args.Name] = asm;
                        binStrm.Close();
                        return asm;
                    }
                }
                return null;
            };
            binStrm.Close();
            try
            {
                logger.Debug("Loaded " + modAsm.GetTypes().Length + " types.");
            }
            catch
            {
                logger.Fatal("Invalid mod package: " + package.Name + ": assembly incompatible!");
                Environment.Exit(1);
                return null;
            }

            ModInfo info = new ModInfo(id, version, package, modAsm);
            mods.Add(info);
            return info;
        }

        /// <summary>
        /// Loads a mod from a debug directory
        /// </summary>
        /// <param name="modDir">Mod root directory</param>
        /// <param name="manifest">Mod manifest file</param>
        public ModInfo LoadDebug(string modDir, DebugModManifest manifest)
        {
            if (locked)
                throw new ArgumentException("Locked");
            string id = manifest.id;
            string version = manifest.version;
            logger.Info("Loading mod: " + id + ", version: " + version);
            ModInfo? i = GetMod(id);
            if (i != null)
            {
                logger.Fatal("Duplicate mod found!\n" +
                    "Mod file: " + i.Package.Name + "\n" +
                    "Version: " + i.Version + "\n\n" +
                    "Mod file: " + Path.GetFileName(modDir) + " {DEBUG-LOADED}" + "\n"
                    + "Version: " + version);
                Environment.Exit(1);
                return null;
            }

            // Read mod assembly into memory
            logger.Debug("Loading mod assembly...");
            if (!File.Exists(modDir + "/" + manifest.modAssemblyDir + "/" + manifest.modAssemblyName))
            {
                logger.Fatal("Failed to load debug mod assembly file! File: " + modDir + "/" + manifest.modAssemblyDir + "/" + manifest.modAssemblyName);
                Environment.Exit(1);
                return null;
            }
            Assembly modAsm = Assembly.LoadFile(modDir + "/" + manifest.modAssemblyDir + "/" + manifest.modAssemblyName);
            AppDomain.CurrentDomain.AssemblyResolve += (sender, args) =>
            {
                AssemblyName nm = new AssemblyName(args.Name);
                if (nm.FullName == modAsm.GetName().FullName)
                    return modAsm;
                else
                {
                    // Check dependencies
                    lock (assemblyCache)
                    {
                        if (assemblyCache.ContainsKey(args.Name))
                            return assemblyCache[args.Name];
                    }

                    // Find other file
                    if (File.Exists(modDir + "/" + manifest.modAssemblyDir + "/" + nm.Name + ".dll"))
                    {
                        // Load assembly
                        Assembly asm = Assembly.LoadFile(modDir + "/" + manifest.modAssemblyDir + "/" + nm.Name + ".dll");
                        lock (assemblyCache)
                            assemblyCache[args.Name] = asm;
                        return asm;
                    }
                }
                return null;
            };
            try
            {
                logger.Debug("Loaded " + modAsm.GetTypes().Length + " types.");
            }
            catch
            {
                logger.Fatal("Failed to load debug mod assembly file! (assembly incompatible) - File: " + modDir + "/" + manifest.modAssemblyDir + "/" + manifest.modAssemblyName);
                Environment.Exit(1);
                return null;
            }

            AssetManager.AddProvider(new FileAssetProvider(modDir + "/ServerAssets"));
            ModInfo info = new ModInfo(id, version, null, modAsm);
            mods.Add(info);
            return info;
        }

        /// <summary>
        /// Retrieves mods by ID
        /// </summary>
        /// <param name="id">Mod ID string</param>
        /// <returns>ModInfo instance or null</returns>
        public ModInfo? GetMod(string id)
        {
            return mods.Where(t => t.ID == id).FirstOrDefault();
        }

        /// <summary>
        /// Retrieves all mod packages
        /// </summary>
        /// <returns></returns>
        public ModInfo[] GetMods()
        {
            return mods.ToArray();
        }
    }
}