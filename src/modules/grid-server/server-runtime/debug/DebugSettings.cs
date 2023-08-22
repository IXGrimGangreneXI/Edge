using Phoenix.Common.Logging;

namespace Phoenix.Debug.DebugServerRunner
{
    public class DebugSettings
    {
        public string workingDirectory = ".";
        public string[] arguments = new string[0];
        public LogLevel logLevel = LogLevel.TRACE; // Debug log dumps A LOT, like WAYYY TOO MUCH unless you actually have a bug you cant find
    }
}
