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

namespace GridServer.Channels
{
    public class TextFilterRequestPacket : AbstractNetworkPacket
    {
        public string message;
        public bool strictChat;

        public override AbstractNetworkPacket Instantiate()
        {
            return new TextFilterRequestPacket();
        }

        public override void Parse(DataReader reader)
        {
            message = reader.ReadString();
            strictChat = reader.ReadBoolean();
        }

        public override void Write(DataWriter writer)
        {
            throw new NotImplementedException();
        }
    }
}