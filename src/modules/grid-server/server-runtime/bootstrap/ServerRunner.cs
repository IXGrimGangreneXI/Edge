using Newtonsoft.Json;
using Phoenix.Common;
using Phoenix.Common.Logging;
using Phoenix.Server.Bootstrapper.Packages;
using Phoenix.Server.Components;
using Phoenix.Server.NetworkServerLib;
using System.IO.Compression;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;

namespace Phoenix.Server.Bootstrapper
{
    public class ServerRunner
    {
        private static bool _supportMods;
        public static bool SupportMods
        {
            get
            {
                return _supportMods;
            }
        }
        public static void StartServer(string gameID, string title, string version, string stage, bool offlineSupport, string serverClass, string serverAssembly, BinaryPackage launchPackage, string[] args)
        {
            long beginStart = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

            // Assign game info
            GameInfoImpl info = new GameInfoImpl();
            info.SetDirectories(Environment.CurrentDirectory, Environment.CurrentDirectory + "/Assets");
            info.gameID = gameID;
            info.title = title;
            info.version = version;
            info.developmentStage = stage;
            info.hasOfflineSupport = offlineSupport;
            info.Register();

            // Setup
            string assets = Game.AssetsFolder;
            ModManager manager = new ModManager();
            Logger logger = Logger.GetLogger("Phoenix");
            Dictionary<string, string> properties = new Dictionary<string, string>();

            // Parse arguments
            logger.Debug("Parsing arguments...");
            for (int i = 0; i < args.Length; i++)
            {
                string arg = args[i];

                // Check argument
                if (arg.StartsWith("-"))
                {
                    string cKey = arg.Substring(1);
                    while (cKey.StartsWith("-"))
                        cKey = cKey.Substring(1);
                    if (cKey.Contains("="))
                    {
                        // Single-argument property
                        string value = cKey.Substring(cKey.IndexOf("=") + 1);
                        cKey = cKey.Remove(cKey.IndexOf("="));
                        cKey = cKey.Replace(":", ".").Replace("-", ".").Replace("_", "-");
                        properties[cKey] = value;
                        logger.Debug("Setting argument: " + cKey + ": " + value);
                    }
                    else
                    {
                        // Check arguments
                        if (i + 1 > args.Length)
                        {
                            // Invalid
                            logger.Warn("Invalid argument: " + arg + ", missing value");
                            break;
                        }

                        string value = args[i + 1];
                        cKey = cKey.Replace(":", ".").Replace("-", ".").Replace("_", "-");
                        properties[cKey] = value;
                        logger.Debug("Setting argument: " + cKey + ": " + value);
                        i++;
                    }
                }
                else
                {
                    logger.Warn("Unhandled argument: " + arg + ": not marked as property or value, argument skipped (make sure to quote values with spaces)");
                }
            }
            if (properties.ContainsKey("log.level"))
            {
                LogLevel logLevel = Logger.GlobalLogLevel;
                switch (properties["log.level"])
                {
                    case "debug":
                        logLevel = LogLevel.DEBUG;
                        break;
                    case "error":
                        logLevel = LogLevel.ERROR;
                        break;
                    case "fatal":
                        logLevel = LogLevel.FATAL;
                        break;
                    case "quiet":
                        logLevel = LogLevel.QUIET;
                        break;
                    case "trace":
                        logLevel = LogLevel.TRACE;
                        break;
                    case "warn":
                        logLevel = LogLevel.WARN;
                        break;
                    case "info":
                        logLevel = LogLevel.INFO;
                        break;
                }
                properties.Remove("log.level");
                logger.Debug("Switching log level: " + logLevel);
                Logger.GlobalLogLevel = logLevel;
            }

            // Prepare folders
            logger.Debug("Preparing folders...");
            Directory.CreateDirectory(Game.AssetsFolder);

            // Load assembly
            logger.Debug("Loading server assembly...");

            // Load server
            Assembly asm = Assembly.Load(serverAssembly);

            // Load server
            logger.Info("Preparing game server...");
            logger.Debug("Loading server class...");
            Type? serverType = asm.GetType(serverClass);
            if (serverType == null || !serverType.IsAssignableTo(typeof(PhoenixDedicatedServer)))
            {
                logger.Debug("Loading error!");
                logger.Fatal("Failed to load server class: " + serverClass);
                Environment.Exit(1);
                return;
            }
            logger.Debug("Instantiating server...");
            ConstructorInfo? cons = serverType.GetConstructor(new Type[0]);
            if (cons == null)
            {
                logger.Debug("Failed to find a constructor!");
                logger.Fatal("Failed to load server class: " + serverClass + ": no constructor that takes 0 arguments.");
                Environment.Exit(1);
                return;
            }
            PhoenixDedicatedServer server = (PhoenixDedicatedServer)cons.Invoke(new object[0]);
            PhoenixDedicatedServer.Hooks hooks = server.setupHooks();

            // Start it
            logger.Debug("Preparing server...");
            server.Prepare();
            logger.Debug("Checking mod support...");
            bool loadMods = false;
            if (server.SupportMods())
            {
                _supportMods = true;
                loadMods = true;

                // Prepare mod support
                logger.Debug("Preparing mod support...");
                Directory.CreateDirectory("Mods");
            }
            if (loadMods)
            {
                logger.Info("Loading mods...");

                // Load command line mods
                if (properties.ContainsKey("debug.add.mod") && Game.DebugMode)
                {
                    // Add UNBUILT mods to the server
                    // This is for mod development, does not load packages but instead files

                    // Find mod
                    string modDir = properties["debug.add.mod"];
                    if (File.Exists(modDir + "/modinfo.json"))
                    {
                        logger.Debug("Loading debug mod: " + modDir);
                        DebugModManifest? manifest = JsonConvert.DeserializeObject<DebugModManifest>(File.ReadAllText(modDir + "/modinfo.json"));
                        if (manifest != null)
                            manager.LoadDebug(modDir, manifest);
                    }
                }

                // Load mod packages
                DirectoryInfo mods = new DirectoryInfo("Mods");
                foreach (FileInfo mod in mods.GetFiles("*.pmbp"))
                {
                    logger.Debug("Loading mod package: " + mod.Name);

                    // Read package
                    FileStream strm = mod.OpenRead();
                    manager.Load(strm);
                }
                manager.Lock();
            }
            // Prepare asset manager
            logger.Info("Preparing the asset manager...");
            logger.Debug("Adding game assets to the asset manager...");
            AssetManager.AddProvider(new EncryptedAssetProvider(launchPackage));
            if (loadMods)
            {
                logger.Debug("Adding mod assets to the asset manager...");

                // Load mod packages
                foreach (ModInfo mod in manager.GetMods())
                {
                    logger.Debug("Adding assets of '" + mod.ID + "' to the asset manager...");
                    if (mod.Package != null)
                        AssetManager.AddProvider(new BinaryPackageAssetProvider(mod.Package, "ServerAssets"));
                }
            }
            AssetManager.Lock();

            // Server setup
            logger.Info("Creating servers...");
            hooks.Init();

            // Add components
            logger.Debug("Configuring servers...");
            int serverID = 1;
            foreach (GameServer srv in hooks.GetServers())
            {
                logger.Debug("Configuring " + serverID + "...");
                foreach ((string key, string value) in properties)
                {
                    srv.ConfigurationOverrides.Add(key, value);
                }
                serverID++;
            }
            logger.Debug("Adding components to servers...");
            serverID = 1;
            foreach (GameServer srv in hooks.GetServers())
            {
                logger.Info("Adding components to server " + serverID + "...");
                srv.AddComponent(new NetworkServerComponent());
                srv.AddComponent(new CertificateRefresherComponent());
                serverID++;
            }
            if (loadMods)
            {
                // Load components
                logger.Debug("Adding mod components to servers...");
                foreach (ModInfo mod in manager.GetMods())
                {
                    logger.Info("Loading mod components from " + mod.ID + "...");
                    Type[] types = mod.Assembly.GetTypes();
                    foreach (Type t in types)
                    {
                        if (t.GetCustomAttribute<ModComponent>() != null)
                        {
                            logger.Debug("Loading component type: " + t.Name + "...");
                            if (!t.IsAssignableTo(typeof(Component)) && !t.IsAssignableTo(typeof(IComponentPackage)))
                            {
                                logger.Fatal("Could not load mod component: " + t.FullName + ", mod: " + mod.Package.Name + ": not a server component or package!");
                                Environment.Exit(1);
                                return;
                            }
                            ConstructorInfo? constr = t.GetConstructor(new Type[0]);
                            if (constr == null)
                            {
                                logger.Fatal("Could not load mod component: " + t.FullName + ", mod: " + mod.Package.Name + ": no constructor that takes 0 arguments!");
                                Environment.Exit(1);
                                return;
                            }
                            if (t.IsAssignableTo(typeof(Component)))
                            {
                                Component comp = (Component)constr.Invoke(new object[0]);

                                // Add to servers
                                serverID = 1;
                                foreach (GameServer srv in hooks.GetServers())
                                {
                                    logger.Info("Adding component " + comp.ID + " to server " + serverID);
                                    srv.AddComponent(comp);
                                    serverID++;
                                }
                            }
                            else
                            {
                                IComponentPackage comp = (IComponentPackage)constr.Invoke(new object[0]);

                                // Add to servers
                                serverID = 1;
                                foreach (GameServer srv in hooks.GetServers())
                                {
                                    logger.Info("Adding component package " + comp.ID + " to server " + serverID);
                                    srv.AddComponentPackage(comp);
                                    serverID++;
                                }
                            }
                        }
                    }
                }
            }

            // Initialize servers
            logger.Info("Initialzing and starting servers...");
            serverID = 1;
            foreach (GameServer srv in hooks.GetServers())
            {
                logger.Info("Initializing server " + serverID + "...");
                srv.Init();

                logger.Info("Starting server " + serverID + "...");
                srv.StartServer();

                serverID++;
            }

            // Start servers
            logger.Info("Starting server tick loops...");
            serverID = 1;
            int runningServers = 0;
            foreach (GameServer srv in hooks.GetServers())
            {
                logger.Info("Starting tick loop for server " + serverID);
                runningServers++;
                Phoenix.Common.AsyncTasks.AsyncTaskManager.RunAsync(() =>
                {
                    srv.ServerLoop();
                    runningServers--;
                });
                serverID++;
            }
            logger.Info("Done! Startup completed in " + (DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - beginStart) + "ms.");

            // Wait for servers to close
            while (runningServers > 0)
            {
                Thread.Sleep(100);
            }

            // Stop
            if (loadMods)
            {
                logger.Info("Unloading mods...");
                foreach (ModInfo package in manager.GetMods())
                {
                    logger.Info("Unloading: " + package.ID);
                    package.Package.Close();
                }
            }
            logger.Info("Server closed");
        }

    }
}