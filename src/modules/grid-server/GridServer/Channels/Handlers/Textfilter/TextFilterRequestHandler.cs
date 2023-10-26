using Phoenix.Server;
using Phoenix.Server.Players;
using Phoenix.Common.Tasks;
using Phoenix.Common.Logging;
using System;
using Phoenix.Server.Configuration;
using Phoenix.Common;
using System.Collections.Generic;
using Phoenix.Common.Networking.Channels;
using Phoenix.Common.Networking.Packets;
using Phoenix.Common.IO;
using Newtonsoft.Json;
using Phoenix.Common.Networking.Connections;
using GridServer.Components;

namespace GridServer.Channels
{
    public class TextFilterRequestHandler : PacketHandler<TextFilterRequestPacket>
    {
        protected override PacketHandler<TextFilterRequestPacket> CreateInstance()
        {
            return new TextFilterRequestHandler();
        }

        protected override bool Handle(TextFilterRequestPacket packet)
        {
            // Get client, server and grid component
            PacketChannel channel = GetChannel();
            Connection conn = GetChannel().Connection;
            GameServer? server = conn.GetObject<GameServer>();
            if (server != null)
            {
                // Get grid component
                GridComponent grid = server.GetComponent<GridComponent>();

                // Create connection
                ApiConnection api = ApiConnection.Connect(grid.GridApiServer);

                // Create interface
                ApiConnectionInterface inter = api.CreateInterface();

                // Create request
                Dictionary<string, object> req = new Dictionary<string, object>();
                req["message"] = packet.message;
                req["strictChat"] = packet.strictChat;

                // Send
                TextFilterResultPacket? res = inter.Request<Dictionary<string, object>, TextFilterResultPacket>("TextFilter", req);
                if (res != null)
                {
                    // Send success
                    channel.SendPacket(res);
                }
                else
                {
                    // Send failure
                    res = new TextFilterResultPacket();
                    res.success = false;
                    channel.SendPacket(res);
                }

                // Close interface
                inter.Close();
            }
            return true;
        }
    }
}