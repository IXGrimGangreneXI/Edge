using Phoenix.Server;
using Phoenix.Server.Players;
using Phoenix.Common.Tasks;
using Phoenix.Common.Logging;
using System;

namespace GridServer
{
    /// <summary>
    /// API connection load enum
    /// </summary>
    public enum ApiConnectionLoad
    {
        IDLE,

        NORMAL,

        BUSY,

        OVERLOADED
    }
}