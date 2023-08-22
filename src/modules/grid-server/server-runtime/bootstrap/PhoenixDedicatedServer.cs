using Phoenix.Common.Logging;
using System.ComponentModel;

namespace Phoenix.Server
{

    /// <summary>
    /// Abstract class for phoenix dedicated servers
    /// </summary>
    public abstract class PhoenixDedicatedServer
    {
        private List<GameServer> _gameServers = new List<GameServer>();
        private bool locked = false;

        [EditorBrowsable(EditorBrowsableState.Never)]
        public class Hooks {
            public PhoenixDedicatedServer server;
            public Hooks(PhoenixDedicatedServer server)
            {
                this.server = server;
            }

            public void Init()
            {
                if (server.locked)
                    throw new ArgumentException();
                server.SetupServers();
                server.locked = true;
            }

            public GameServer[] GetServers()
            {
                return server._gameServers.ToArray();
            }
        }

        [EditorBrowsable(EditorBrowsableState.Never)]
        public Hooks setupHooks()
        {
            if (locked)
                throw new ArgumentException();
            Hooks hooks = new Hooks(this);
            return hooks;
        }

        /// <summary>
        /// Called after assemblies are loaded up, and before all game servers are loaded
        /// </summary>
        public abstract void Prepare();

        /// <summary>
        /// Called to set up servers
        /// </summary>
        protected abstract void SetupServers();

        /// <summary>
        /// Defines if mods should be loaded into servers
        /// </summary>
        public abstract bool SupportMods();

        private Logger logger = Logger.GetLogger("Phoenix Server Loader");

        /// <summary>
        /// Adds a server to load
        /// </summary>
        /// <param name="server">Server to load</param>
        protected void AddServer(GameServer server)
        {
            if (locked)
                throw new InvalidOperationException("Server locked");
            logger.Info("Created server " + (_gameServers.Count + 1));
            _gameServers.Add(server);

        }
    }
}
