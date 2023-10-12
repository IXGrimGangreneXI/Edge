using Newtonsoft.Json;
using Phoenix.Common;
using Phoenix.Common.IO;
using Phoenix.Common.Logging;
using Phoenix.Debug.DebugServerLib;
using Phoenix.Server;
using Phoenix.Server.Bootstrapper.Packages;
using Phoenix.Server.Components;
using Phoenix.Server.NetworkServerLib;
using System.IO.Compression;
using System.Reflection;
using System.Text;

namespace Phoenix.Debug.DebugServerRunner
{
    public class ServerDebugger
    {
        public static void Run(ProjectManifest project, Logger logger, DebugGameDefLib.DebugGameDef game)
        {
            long beginStart = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            string assets = Path.GetFullPath(project.assetsFolder);
            ModManager manager = new ModManager();

            // Log
            logger.Info("Loading debug configuration...");
            logger.Debug("Reading debug properties...");
            if (!File.Exists(project.debugConfig))
            {
                logger.Fatal("Failed to read debug configuration!");
                Environment.Exit(1);
                return;
            }

            // Read config
            string debug = File.ReadAllText(project.debugConfig);
            logger.Debug("Parsing debug properties...");
            DebugSettings? settings = JsonConvert.DeserializeObject<DebugSettings>(debug);
            if (settings == null)
            {
                logger.Debug("Parsing error!");
                logger.Fatal("Failed to read debug configuration!");
                Environment.Exit(1);
                return;
            }

            // Log level
            logger.Debug("Switching log level: " + settings.logLevel);
            Logger.GlobalLogLevel = settings.logLevel;

            Dictionary<string, string> properties = new Dictionary<string, string>();

            // Parse arguments
            logger.Debug("Parsing arguments...");
            for (int i = 0; i < settings.arguments.Length; i++)
            {
                string arg = settings.arguments[i];

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
                        if (i + 1 > settings.arguments.Length)
                        {
                            // Invalid
                            logger.Warn("Invalid argument: " + arg + ", missing value");
                            break;
                        }

                        string value = settings.arguments[i + 1];
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

            // Switch directory
            logger.Debug("Switching to debug working directory...");
            Directory.CreateDirectory(settings.workingDirectory);
            Environment.CurrentDirectory = Path.GetFullPath(settings.workingDirectory);
            game.SetDirectories(Environment.CurrentDirectory, assets);

            // Assign game
            logger.Debug("Registering game implementation...");
            game.Register();

            // Prepare folders
            logger.Debug("Preparing folders...");
            Directory.CreateDirectory(Game.AssetsFolder);

            // Load assembly
            logger.Debug("Loading server assembly...");
            if (!File.Exists(project.serverAssembly))
            {
                logger.Debug("Loading error!");
                logger.Fatal("Failed to find server assembly: " + project.serverAssembly);
                Environment.Exit(1);
                return;
            }
            Assembly asm = Assembly.LoadFrom(project.serverAssembly);

            // Load server
            logger.Info("Preparing game server...");
            logger.Debug("Loading server class...");
            Type? serverType = asm.GetType(project.serverClass);
            if (serverType == null || !serverType.IsAssignableTo(typeof(PhoenixDedicatedServer)))
            {
                logger.Debug("Loading error!");
                logger.Fatal("Failed to load server class: " + project.serverClass);
                Environment.Exit(1);
                return;
            }
            logger.Debug("Instantiating server...");
            ConstructorInfo? cons = serverType.GetConstructor(new Type[0]);
            if (cons == null)
            {
                logger.Debug("Failed to find a constructor!");
                logger.Fatal("Failed to load server class: " + project.serverClass + ": no constructor that takes 0 arguments.");
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
                loadMods = true;

                // Prepare mod support
                logger.Debug("Preparing mod support...");
                Directory.CreateDirectory("Mods");
            }
            if (loadMods)
            {
                logger.Info("Loading mods...");

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
            AssetManager.AddProvider(new FileAssetProvider());
            if (loadMods)
            {
                logger.Debug("Adding mod assets to the asset manager...");

                // Load mod packages
                foreach (ModInfo mod in manager.GetMods())
                {
                    logger.Debug("Adding assets of '" + mod.ID + "' to the asset manager...");
                    AssetManager.AddProvider(new BinaryPackageAssetProvider(mod.Package, "Assets"));
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
                srv.AddComponentPackage(new DebugServerPackage());
                serverID++;
            }
            if (loadMods)
            {
                // Load components
                logger.Debug("Adding mod components to servers...");
                foreach(ModInfo mod in manager.GetMods())
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
                Phoenix.Common.AsyncTasks.AsyncTaskManager.RunAsync(() => {
                    srv.ServerLoop();
                    runningServers--;
                });
                serverID++;
            }
            logger.Info("Done! Startup completed in " + (DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - beginStart) + "ms.");

            // Wait for servers to close
            while (runningServers > 0)
                Thread.Sleep(100);

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
