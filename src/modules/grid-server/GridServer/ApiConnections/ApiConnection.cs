using Phoenix.Server;
using Phoenix.Server.Players;
using Phoenix.Common.Tasks;
using Phoenix.Common.Logging;
using Phoenix.Common.IO;
using Newtonsoft.Json;
using System.Collections.Generic;
using System.Net.Sockets;
using System.IO;
using System;
using System.Net.Security;
using System.Text;
using System.Net;
using Phoenix.Common.AsyncTasks;
using System.Threading;

namespace GridServer
{
    /// <summary>
    /// API connection type - connection pooling system for Grid API requests
    /// </summary>
    public class ApiConnection
    {
        private TcpClient client;
        private Stream stream;
        private List<ApiConnectionInterface> interfaces = new List<ApiConnectionInterface>();
        private ApiConnectionLoad loadStatus = ApiConnectionLoad.IDLE;
        private object sendLock = new object();
        private bool connected = false;
        private long loadTicks = 0;
        private int interfaceIdGlobal = 0;
        private long lastPacketSentTime;

        private static List<ApiConnection> connectionPool = new List<ApiConnection>();

        private static ApiConnection CreateConnection(string api)
        {
            // Create object
            ApiConnection conn = new ApiConnection();
            conn.connected = true;

            // Build URL
            string url = api;
            if (!url.EndsWith("/"))
                url += "/";
            url += "grid/apiconnector";

            // Send request
            Uri u = new Uri(url);
            TcpClient client = new TcpClient(u.Host, u.Port);
            Stream strm = client.GetStream();

            // Check https
            if (u.Scheme == "https")
            {
                SslStream st = new SslStream(strm);
                st.AuthenticateAsClient(u.Host);
                strm = st;
            }

            // Write request
            strm.Write(Encoding.UTF8.GetBytes("POST " + WebUtility.UrlEncode(u.PathAndQuery) + " HTTP/1.1\r\n"));
            strm.Write(Encoding.UTF8.GetBytes("X-Request-ID: " + Guid.NewGuid().ToString() + "\r\n"));
            strm.Write(Encoding.UTF8.GetBytes("X-Request-RNDID: " + Guid.NewGuid().ToString() + "\r\n"));
            strm.Write(Encoding.UTF8.GetBytes("Upgrade: GRIDAPICONNECTOR\r\n"));
            strm.Write(Encoding.UTF8.GetBytes("\r\n"));

            // Check response
            string line = ReadStreamLine(strm);
            string statusLine = line;
            if (!line.StartsWith("HTTP/1.1 "))
            {
                client.Close();
                throw new IOException("Server returned invalid protocol");
            }

            // Read headers
            Dictionary<string, string> respHeaders = new Dictionary<string, string>();
            while (true)
            {
                line = ReadStreamLine(strm);
                if (line == "")
                    break;
                String key = line.Substring(0, line.IndexOf(": "));
                String value = line.Substring(line.IndexOf(": ") + 2);
                respHeaders[key.ToLower()] = value;
            }

            // Read body if present
            byte[] rData = new byte[0];
            if (respHeaders.ContainsKey("content-length"))
            {
                // Read
                rData = new byte[int.Parse(respHeaders["content-length"])];
                strm.Read(rData);
            }

            // Check status
            int status = int.Parse(statusLine.Split(' ')[1]);
            if (status != 101)
            {
                strm.Close();
                client.Close();

                string err = statusLine.Substring("HTTP/1.1 ".Length);

                throw new IOException("Received HTTP " + err);
            }

            // Assign
            conn.client = client;
            conn.stream = strm;

            // Start handler in new thread
            conn.lastPacketSentTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            AsyncTaskManager.RunAsync(() =>
            {
                // Begin update thread
                long lastTicks = 0;
                long lastTicksChanged = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                int lastBusyInterfaces = 0;
                long lastBusyInterfacesChanged = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                while (conn.IsConnected)
                {
                    // Check ping
                    if (DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - conn.lastPacketSentTime >= 25000)
                    {
                        // Send ping
                        lock (conn.sendLock)
                        {
                            DataWriter wr = new DataWriter(strm);
                            wr.WriteInt(-1);
                        }
                        conn.lastPacketSentTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                    }

                    // Update load state
                    ApiConnectionLoad state = ApiConnectionLoad.IDLE;
                    if (conn.loadTicks >= 10)
                        state = ApiConnectionLoad.NORMAL;
                    if (conn.loadTicks >= 20)
                        state = ApiConnectionLoad.BUSY;
                    if (conn.loadTicks >= 1000)
                        state = ApiConnectionLoad.OVERLOADED;
                    conn.loadStatus=state;

                    // Check ticks
                    if (lastTicks == conn.loadTicks)
                    {
                        // Count down
                        if (conn.loadTicks != 0)
                        {
                            if (DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - lastTicksChanged >= 3000)
                            {
                                // Decrease ticks
                                conn.loadTicks--;
                                lastTicks = conn.loadTicks;
                            }
                        }
                        else
                        {
                            // Load ticks hit zero

                            // Load count
                            int busyInterfaces = 0;
                            lock (conn.interfaces)
                            {
                                foreach (ApiConnectionInterface interf in conn.interfaces)
                                {
                                    if (interf.waitingForResponse)
                                        busyInterfaces++;
                                }
                            }

                            // Check busy interfaces
                            if (busyInterfaces != lastBusyInterfaces)
                            {
                                // Update
                                lastBusyInterfaces = busyInterfaces;
                                lastBusyInterfacesChanged = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                            }
                            else
                            {
                                // Count down
                                if (DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - lastBusyInterfacesChanged >= 30000)
                                {
                                    // 30 seconds passed
                                    // end client
                                    conn.Close();
                                }
                            }
                        }
                    }
                    else
                    {
                        // Update
                        lastTicks = conn.loadTicks;
                        lastTicksChanged = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                        lastBusyInterfacesChanged = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                    }

                    // Wait 100ms
                    Thread.Sleep(100);
                }

                // Ended
                // Remove connection
                lock (connectionPool)
                {
                    connectionPool.Remove(conn);
                }
            });
            AsyncTaskManager.RunAsync(() =>
            {
                // Begin reader thread
                DataReader rd = new DataReader(strm);
                while (conn.IsConnected)
                {
                    try
                    {
                        // Try read
                        // Read target
                        int target = rd.ReadInt();
                        if (target != -1)
                        {
                            // Handle
                            string payload = rd.ReadString();
                            ApiConnectionInterface? inter = null;
                            lock (conn.interfaces)
                            {
                                foreach (ApiConnectionInterface interf in conn.interfaces)
                                {
                                    if (interf.interfaceID == target)
                                    {
                                        // Assign
                                        inter = interf;
                                        break;
                                    }
                                }
                            }

                            // Check
                            if (inter != null)
                            {
                                // Handle response
                                try
                                {
                                    inter.response = JsonConvert.DeserializeObject<Dictionary<string, object>>(payload);
                                }
                                catch
                                { }
                            }
                        }
                    }
                    catch
                    {
                        // Disconnected
                        conn.Close();
                    }
                }
            });

            // Return
            return conn;
        }

        /// <summary>
        /// Creates or retrieves API connection objects from the pool
        /// </summary>
        /// <param name="api">Grid API address</param>
        /// <exception cref="System.IO.IOException">Thrown if connecting to the API fails</exception>
        /// <returns>ApiConnection instance</returns>
        public static ApiConnection Connect(string api)
        {
            // Go through pool
            lock(connectionPool)
            {
                // Find idle connections first
                foreach (ApiConnection conn in connectionPool)
                {
                    if (conn.loadStatus == ApiConnectionLoad.IDLE && conn.IsConnected)
                    {
                        // Return if possible
                        conn.loadTicks++;
                        if (conn.IsConnected)
                            return conn;
                    }
                }

                // Then try normal load
                foreach (ApiConnection conn in connectionPool)
                {
                    if (conn.loadStatus == ApiConnectionLoad.NORMAL && conn.IsConnected)
                    {
                        // Return if possible
                        conn.loadTicks++;
                        if (conn.IsConnected)
                            return conn;
                    }
                }

                // Then try the somewhat higher load
                foreach (ApiConnection conn in connectionPool)
                {
                    if (conn.loadStatus == ApiConnectionLoad.BUSY && conn.IsConnected)
                    {
                        // Return if possible
                        conn.loadTicks++;
                        if (conn.IsConnected)
                            return conn;
                    }
                }
            }

            // Create new connection
            ApiConnection conn2 = CreateConnection(api);

            // Add
            lock (connectionPool)
            {
                connectionPool.Add(conn2);
            }

            // Return
            return conn2;
        }

        /// <summary>
        /// Creates new interfaces
        /// </summary>
        public ApiConnectionInterface CreateInterface()
        {
            // Create
            ApiConnectionInterface inter = new ApiConnectionInterface();
            inter.interfaceID = interfaceIdGlobal++;
            inter.connection = this;

            // Add
            lock (interfaces)
            {
                interfaces.Add(inter);
            }

            // Increase load
            loadTicks++;

            // Return
            return inter;
        }

        /// <summary>
        /// Checks if the client is connected
        /// </summary>
        public bool IsConnected
        {
            get
            {
                return connected;
            }
        }

        /// <summary>
        /// Retrieves the connection load
        /// </summary>
        public ApiConnectionLoad Load
        {
            get
            {
                return loadStatus;
            }
        }

        /// <summary>
        /// Closes the connection forcefully
        /// </summary>
        public void Close()
        {
            if (!connected)
                return;
            connected = false;

            // Disconnect
            try
            {
                stream.Close();
            }
            catch { }
            try
            {
                client.Close();
            }
            catch { }

            // Clear interfaces
            lock (interfaces)
            {
                interfaces.Clear();
            }
        }

        internal void CloseInterface(ApiConnectionInterface inter)
        {
            // Remove interface
            lock(interfaces)
            {
                interfaces.Remove(inter);
            }
        }

        internal void SendRequest(int interfaceID, string requestType, Dictionary<string, object> payload, int timeout)
        {
            // Write
            lock(sendLock)
            {
                DataWriter wr = new DataWriter(stream);
                wr.WriteInt(interfaceID);
                wr.WriteString(requestType);
                wr.WriteString(JsonConvert.SerializeObject(payload));
                lastPacketSentTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            }

            // Increase load
            loadTicks++;
        }

        private static string ReadStreamLine(Stream strm)
        {
            string buffer = "";
            while (true)
            {
                int b = strm.ReadByte();
                if (b == -1)
                    break;
                char ch = (char)b;
                if (ch == '\n')
                    return buffer;
                else if (ch != '\r')
                    buffer += ch;
            }
            return buffer;
        }

    }
}