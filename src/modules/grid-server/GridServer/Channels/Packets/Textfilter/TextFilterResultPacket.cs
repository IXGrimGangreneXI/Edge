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
using GridServer.Entities;

namespace GridServer.Channels
{
    public class TextFilterResultPacket : AbstractNetworkPacket
    {
        public bool success = true;
        public bool isFiltered;
        public string filteredResult;
        public WordMatch[] matches;
        public FilterSeverity severity;

        public override TextFilterResultPacket Instantiate()
        {
            return new TextFilterResultPacket();
        }

        public override void Parse(DataReader reader)
        {
            throw new NotImplementedException();
        }

        public override void Write(DataWriter writer)
        {
            writer.WriteBoolean(success);
            if (success)
            {
                writer.WriteBoolean(isFiltered);
                writer.WriteString(filteredResult);
                writer.WriteInt(matches.Length);
                foreach (WordMatch match in matches)
                {
                    writer.WriteString(match.phrase);
                    writer.WriteString(match.matchedPhrase);
                    writer.WriteString(match.reason);
                    writer.WriteRawByte((byte)match.severity);
                }
                writer.WriteRawByte((byte)severity);
            }
        }
    }
}