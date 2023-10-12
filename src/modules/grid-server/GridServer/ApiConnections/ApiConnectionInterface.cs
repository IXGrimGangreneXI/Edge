using Phoenix.Server;
using Phoenix.Server.Players;
using Phoenix.Common.Tasks;
using Phoenix.Common.Logging;
using System;
using System.Collections.Generic;
using System.Threading;

namespace GridServer
{
    /// <summary>
    /// API connection interface - connection pooling system for Grid API requests
    /// </summary>
    public class ApiConnectionInterface : IDisposable
    {
        internal int interfaceID;
        internal ApiConnection connection;
        internal Dictionary<string, object>? response;
        internal bool waitingForResponse;
        private object sendLock = new object();
        private bool closed;

        /// <summary>
        /// Sends requests and waits for responses
        /// </summary>
        /// <param name="requestType">Request type</param>
        /// <param name="payload">Request payload</param>
        /// <param name="timeout">Timeout in miliseconds</param>
        /// <returns>Dictionary response or null if timed out</returns>
        public Dictionary<string, object>? Request(string requestType, Dictionary<string, object> payload, int timeout = 5000)
        {
            lock (sendLock)
            {
                // Unset response
                response = null;

                // Send
                connection.SendRequest(interfaceID, requestType, payload, timeout);

                // Wait for response
                long startTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                waitingForResponse = true;
                while (response == null && connection.IsConnected)
                {
                    // Wait
                    if (DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - startTime > timeout)
                        break; // Stop waiting
                    Thread.Sleep(10);
                }
                waitingForResponse = false;

                // Return
                return response;
            }
        }


        /// <summary>
        /// Checks if the interface is closed
        /// </summary>
        public bool IsClosed
        {
            get
            {
                if (!Connection.IsConnected)
                    return true;
                return closed;
            }
        }

        /// <summary>
        /// Retrieves the API connection instance
        /// </summary>
        public ApiConnection Connection
        {
            get
            {
                return connection;
            }
        }

        /// <summary>
        /// Closes the interface
        /// </summary>
        public void Close()
        {
            Dispose();
        }

        /// <summary>
        /// Closes the interface
        /// </summary>
        public void Dispose()
        {
            if (IsClosed)
                return;
            closed = true;
            connection.CloseInterface(this);
        }
    }
}