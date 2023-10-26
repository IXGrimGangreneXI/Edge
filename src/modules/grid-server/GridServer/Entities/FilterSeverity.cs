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

namespace GridServer.Entities
{
    public enum FilterSeverity
    {

        NONE,

        ALWAYS_FILTERED,

        USER_STRICT_MODE,

        INSTAMUTE

    }
}