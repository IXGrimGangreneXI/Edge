global using global::System;
global using global::System.Collections.Generic;
global using global::System.IO;
global using global::System.Linq;
global using global::System.Threading;
global using global::System.Threading.Tasks;

internal static class StreamExtensions
{
    public static void Write(this Stream strm, byte[] data)
    {
        strm.Write(data, 0, data.Length);
    }
    public static void Read(this Stream strm, byte[] data)
    {
        strm.Read(data, 0, data.Length);
    }
}
