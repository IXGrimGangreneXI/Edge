using Newtonsoft.Json;
using Phoenix.Common.Logging;
using Phoenix.Debug.DebugGameDefLib;
using System.Globalization;

namespace Phoenix.Debug.DebugServerRunner
{
    public static class Program
    {
        private static Logger logger;
        public static void Main(string[] args)
        {
            Thread.CurrentThread.CurrentCulture = CultureInfo.InvariantCulture;
            Thread.CurrentThread.CurrentUICulture = CultureInfo.InvariantCulture;
            CultureInfo.DefaultThreadCurrentCulture = CultureInfo.InvariantCulture;
            CultureInfo.DefaultThreadCurrentUICulture = CultureInfo.InvariantCulture;

            // Basic setup
            Console.WriteLine("Preparing debug tools... Please wait...");
            Console.WriteLine("Initializing logging system... Setting log level to debug");
            Logger.GlobalLogLevel = LogLevel.DEBUG;
            logger = Logger.GetLogger("Debug");
            logger.Info("Preparing Debug environment...");

            // Log
            logger.Info("Loading project information...");

            // Check arguments
            logger.Debug("Checking arguments...");
            if (args.Length < 1 || !File.Exists(args[0]))
            {
                logger.Fatal("Invalid usage: expected arguments: path to project.json");
                Environment.Exit(1);
                return;
            }

            // Read manifest
            logger.Debug("Reading project manifest...");
            string manData = File.ReadAllText(args[0]);
            logger.Debug("Parsing project manifest...");
            ProjectManifest? manifest = JsonConvert.DeserializeObject<ProjectManifest>(manData);
            if (manifest == null || manifest.assetsFolder == null || manifest.debugConfig == null || manifest.manifestFile == null || manifest.serverClass == null || manifest.serverAssembly == null)
            {
                logger.Debug("Parsing failure!");
                logger.Fatal("Invalid usage: expected arguments: <path to project.json> <run/build>");
                Environment.Exit(1);
                return;
            }

            // Switch root directory
            Environment.CurrentDirectory = Path.GetFullPath(Path.GetDirectoryName(Path.GetFullPath(args[0])));

            // Find files
            logger.Debug("Finding assets directory...");
            if (!Directory.Exists(manifest.assetsFolder))
            {
                logger.Fatal("Could not find the specified assets directory!");
                Environment.Exit(1);
                return;
            }
            string assets = Path.GetFullPath(manifest.assetsFolder);

            // Load game
            logger.Debug("Loading game information...");
            if (!File.Exists(manifest.manifestFile))
            {
                logger.Fatal("Failed to read game manifest!");
                Environment.Exit(1);
                return;
            }
            DebugGameDef? game = JsonConvert.DeserializeObject<DebugGameDef>(File.ReadAllText(manifest.manifestFile));
            if (game == null || game.gameID == null || game.developmentStage == null || game.title == null || game.version == null)
            {
                logger.Fatal("Failed to parse game manifest!");
                Environment.Exit(1);
                return;
            }
            logger.Debug("Game ID: " + game.gameID);
            logger.Debug("Game title: " + game.title);
            logger.Debug("Version: " + game.version);

            // Handle command
            logger.Debug("Finding operation...");
            if (args.Length < 2 || (args[1] != "build" && args[1] != "run"))
            {
                logger.Fatal("Invalid usage: expected arguments: <path to project.json> <run/build>");
                Environment.Exit(1);
                return;
            }
            if (args[1] == "run")
                ServerDebugger.Run(manifest, logger, game);
            else
            {
                game.SetDirectories(Environment.CurrentDirectory, assets);
                ServerBuilder.Run(manifest, logger, game);
            }
        }
    }
}