using System;
using System.Net.Http;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Phoenix.Common;
using Phoenix.Server;
using Phoenix.Server.Configuration;

namespace GridServer
{

    public class GridSession
    {
        private string _accountID;
        private string _token;
        private GameServer _server;

        /// <summary>
        /// Creates a full login response container
        /// </summary>
        /// <param name="accountID">Player account ID</param>
        /// <param name="token">Game session token</param>
        /// <param name="server">Server instance</param>
        public GridSession(string accountID, string token, GameServer server)
        {
            _accountID = accountID;
            _token = token;
            _server = server;
        }

        /// <summary>
        /// Account ID
        /// </summary>
        public string AccountID 
        { 
            get
            {
                return _accountID;
            }
        }

        /// <summary>
        /// Session token
        /// </summary>
        public string SessionToken
        {
            get
            {
                return _token;
            }
        }

        /// <summary>
        /// Refreshes the token
        /// </summary>
        /// <returns>True if successful, false otherwise</returns>
        public bool Refresh()
        {
            if (_token.Split('.').Length != 3)
                return false;

            // Attempt to refresh token
            try
            {
                // Create url
                AbstractConfigurationSegment conf = _server.GetConfiguration("gridapi");
                if (!conf.HasEntry("grid-api-server"))
                    conf.SetString("grid-api-server", PhoenixEnvironment.DefaultAPIServer);
                string url = conf.GetString("grid-api-server");
                conf = _server.GetConfiguration("server");
                if (!url.EndsWith("/"))
                    url += "/";
                url += "tokens/refresh";

                // Refresh token
                HttpClient cl = new HttpClient();
                cl.DefaultRequestHeaders.Add("Authorization", "Bearer " + _token);
                cl.DefaultRequestHeaders.Add("Server-Authorization", "Bearer " + conf.GetString("token"));
                string result = cl.GetAsync(url).GetAwaiter().GetResult().Content.ReadAsStringAsync().GetAwaiter().GetResult();
                if (result != null && result != "")
                {
                    _token = result.Trim();
                    return true;
                }
            }
            catch
            {
            }
            return false;
        }

        private static class Base64Url
        {
            public static string Encode(byte[] arg)
            {
                if (arg == null)
                {
                    throw new ArgumentNullException("arg");
                }

                var s = Convert.ToBase64String(arg);
                return s
                    .Replace("=", "")
                    .Replace("/", "_")
                    .Replace("+", "-");
            }

            public static string ToBase64(string arg)
            {
                if (arg == null)
                {
                    throw new ArgumentNullException("arg");
                }

                var s = arg
                        .PadRight(arg.Length + (4 - arg.Length % 4) % 4, '=')
                        .Replace("_", "/")
                        .Replace("-", "+");

                return s;
            }

            public static byte[] Decode(string arg)
            {
                return Convert.FromBase64String(ToBase64(arg));
            }
        }
    }
}