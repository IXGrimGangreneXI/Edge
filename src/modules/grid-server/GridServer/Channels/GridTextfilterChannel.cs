using Phoenix.Server;
using Phoenix.Server.Players;
using Phoenix.Common.Tasks;
using Phoenix.Common.Logging;
using System;
using Phoenix.Server.Configuration;
using Phoenix.Common;
using System.Collections.Generic;
using Phoenix.Common.Networking.Channels;

namespace GridServer.Channels
{
    public class GridTextfilterChannel : PacketChannel
    {
        public override PacketChannel Instantiate()
        {
            return new GridTextfilterChannel();
        }

        protected override void MakeRegistry()
        {
            // Register packets
            RegisterPacket(new TextFilterRequestPacket());
            RegisterPacket(new TextFilterResultPacket());

            // Register handlers
            RegisterHandler(new TextFilterRequestHandler());
        }
    }
}