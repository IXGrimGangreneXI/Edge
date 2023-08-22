using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using GridServer.Components;
using Phoenix.Common;
using Phoenix.Common.Networking.Registry;
using Phoenix.Common.SceneReplication;
using Phoenix.Server;
using Phoenix.Server.Components;
using Phoenix.Server.NetworkServerLib;

namespace GridServer
{
    /// <summary>
    /// Dedicated Server Startup Class
    /// </summary>
    public class GridDedicatedServer : PhoenixDedicatedServer
    {
        public override void Prepare()
        {
            // Called after loading the assemblies
        }

        public override bool SupportMods()
        {
            return false;
        }

        protected override void SetupServers()
        {
            // Set grid server
            PhoenixEnvironment.DefaultAPIServer = "https://grid.sentinel.projectedge.net:16718/";

            // Create server
            GameServer server = new GameServer("server");
            server.ProtocolVersion = 1;

            // Create registry
            ChannelRegistry registry = new ChannelRegistry();
            server.ChannelRegistry = registry;

            // Add core components
            server.AddComponent(new TaskManagerComponent());
            server.AddComponent(new PlayerManagerComponent());
            server.AddComponent(new AuthenticationManagerComponent());
            server.AddComponent(new SceneReplicationComponent());
            server.AddComponent(new ConfigManagerComponent());
            server.AddComponent(new ServerListPublisherComponent());
            server.AddComponent(new NetworkServerComponent());

            // Add grid components
            server.AddComponent(new GridComponent());

            // Add replication channel
            registry.Register(new SceneReplicationChannel());

            // Add it
            AddServer(server);
        }
    }
}
