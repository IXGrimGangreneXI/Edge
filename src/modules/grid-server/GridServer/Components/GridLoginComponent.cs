using Newtonsoft.Json;
using Phoenix.Common;
using Phoenix.Common.IO;
using Phoenix.Common.Events;
using Phoenix.Common.Networking.Connections;
using Phoenix.Server.Configuration;
using Phoenix.Server.Events;
using Phoenix.Server.Players;
using System.Text;
using System.Net.Http;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using GridServer;
using Phoenix.Common.AsyncTasks;
using System.Threading;
using Newtonsoft.Json.Linq;

namespace Phoenix.Server.Components
{
    public class GridLoginComponent : ServerComponent
    {
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

        private bool IsSecureServer()
        {
            // Check server
            if (Server.ServerConnection is NetworkServerConnection)
            {
                AbstractConfigurationSegment conf = Server.GetConfiguration("server");
                if (!conf.HasEntry("phoenix-api-server"))
                    conf.SetString("phoenix-api-server", PhoenixEnvironment.DefaultAPIServer + "servers");

                // Verify certificate & token validity
                bool secureMode = conf.GetBool("secure-mode");
                if (secureMode)
                {
                    // Read server token
                    string? token = conf.GetString("token");
                    if (!secureMode)
                        token = "disabled";
                    else if (token == null || token == "undefined" || token.Split('.').Length != 3)
                    {
                        conf.SetString("token", "undefined");
                        secureMode = false;
                        token = "undefined";
                    }

                    // Check token validity
                    if (secureMode)
                    {
                        // Decode the payload
                        string[] jwt = token.Split('.');
                        string payloadEncoded = jwt[1];
                        try
                        {
                            string payload = Encoding.UTF8.GetString(Base64Url.Decode(payloadEncoded));
                            Dictionary<string, object>? data = JsonConvert.DeserializeObject<Dictionary<string, object>>(payload);
                            if (data == null || !data.ContainsKey("exp"))
                                throw new ArgumentException();

                            // Check expiry
                            long exp = long.Parse(data["exp"].ToString());
                            if (DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() > exp)
                                throw new ArgumentException();
                            long tokenIssueTime = long.Parse(data["iat"].ToString());
                            if (data["cgi"].ToString() != Game.GameID)
                                throw new ArgumentException();

                            AbstractConfigurationSegment? certificate = conf.GetSegment("certificate");
                            if (certificate != null && DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() <= (certificate.GetLong("expiry") - (7 * 24 * 60 * 60 * 1000)) && tokenIssueTime == certificate.GetLong("tokenIssueTime") && Enumerable.SequenceEqual(conf.GetStringArray("addresses"), certificate.GetStringArray("addressesInternal")))
                            {
                                // Valid
                                return true;
                            }
                        }
                        catch
                        {
                        }
                    }
                }
            }
            return false;
        }

        public override string ID => "grid-login-manager";

        protected override string ConfigurationKey => "grid";

        protected override void Define()
        {
            DependsOn("player-manager");
            DependsOn("authentication-manager");
        }

        public override void Init()
        {
            AbstractConfigurationSegment conf = Server.GetConfiguration("gridapi");
            if (!conf.HasEntry("grid-api-server"))
                conf.SetString("grid-api-server", PhoenixEnvironment.DefaultAPIServer);

            // Start session refresher
            AsyncTaskManager.RunAsync(() =>
            {
                while (true)
                {
                    try
                    {
                        Player[] players = ServiceManager.GetService<PlayerManagerService>().Players;
                        foreach (Player plr in players)
                        {
                            // Verify session
                            GridSession ses = plr.GetObject<GridSession>();
                            if (ses != null)
                            {
                                try
                                {
                                    RefreshSession(plr, ses);
                                }
                                catch { }
                            }
                        }
                    }
                    catch { }
                    Thread.Sleep(100);
                }
            });
        }

        private static void RefreshSession(Player plr, GridSession session)
        {
            // Parse token
            string[] parts = session.SessionToken.Split('.');
            string payloadJson = Encoding.UTF8.GetString(Base64Url.Decode(parts[1]));
            JObject payload = JsonConvert.DeserializeObject<JObject>(payloadJson);
            if (DateTimeOffset.UtcNow.ToUnixTimeSeconds() + (15 * 60) >= payload.GetValue("exp").ToObject<long>())
            {
                if (!session.Refresh())
                {
                    // Disconnect
                    plr.Disconnect("disconnect.duplicatelogin");
                }
            }
        }

        public class AuthResponse
        {
            public string token;
            public string accountID;
        }

        [EventListener]
        public void ClientConnected(ClientConnectedEvent ev)
        {
            try
            {
                byte[] magic = Encoding.UTF8.GetBytes("GRIDLOGINSTART");
                ev.EventArgs.ClientOutput.WriteRawBytes(magic);
                for (int i = 0; i < magic.Length; i++)
                {
                    if (magic[i] != ev.EventArgs.ClientInput.ReadRawByte())
                    {
                        throw new Exception();
                    }
                }
            }
            catch
            {
                // Log debug warning
                GetLogger().Trace("WARNING! Failed to authenticate due to the first bit of network traffic not being a Grid login packet.");
                GetLogger().Trace("Please make sure the order of loading for components subscribed to the ClientConnectedEvent event is the same on both client and server.");

                // Disconnect
                if (ev.Client.IsConnected())
                    ev.Client.Close();
                return;
            }

            try
            {
                PlayerManagerService manager = ServiceManager.GetService<PlayerManagerService>();
                if (!IsSecureServer())
                {
                    // Error
                    GetLogger().Warn("Failed to authenticate client: " + ev.Client.GetRemoteAddress() + ": running in insecure mode, Grid cannot function in insecure-mode as it requires a server token!");
                    ev.Client.Close("disconnect.loginfailure.phoenixunreachable");
                }
                else
                {
                    // Secure-mode handshake
                    string secret = ev.EventArgs.ClientInput.ReadString();

                    // Contact phoenix
                    AbstractConfigurationSegment confS = Server.GetConfiguration("server");
                    AbstractConfigurationSegment conf = Server.GetConfiguration("gridapi");
                    if (!conf.HasEntry("grid-api-server"))
                        conf.SetString("grid-api-server", PhoenixEnvironment.DefaultAPIServer);
                    string url = conf.GetString("grid-api-server") + "/grid/gameplay/authenticateplayer";
                    AuthResponse response = null;
                    try
                    {
                        HttpClient cl = new HttpClient();
                        string payload = JsonConvert.SerializeObject(new Dictionary<string, object>() { ["secret"] = secret });
                        cl.DefaultRequestHeaders.Add("Authorization", "Bearer " + confS.GetString("token"));
                        string result = cl.PostAsync(url, new StringContent(payload)).GetAwaiter().GetResult().Content.ReadAsStringAsync().GetAwaiter().GetResult();
                        response = JsonConvert.DeserializeObject<AuthResponse>(result);
                        if (response == null || response.token == null || response.accountID == null)
                            throw new IOException();
                    }
                    catch
                    {
                        GetLogger().Warn("Failed to authenticate client: " + ev.Client.GetRemoteAddress() + ": failed to retrieve player information from Phoenix for Grid login.");

                        // Connect failure
                        ev.EventArgs.ClientOutput.WriteBoolean(false);

                        // Write reason
                        ev.EventArgs.ClientOutput.WriteBoolean(true);
                        ev.EventArgs.ClientOutput.WriteString("disconnect.loginfailure.phoenixunreachable");
                        ev.EventArgs.ClientOutput.WriteInt(0);
                        ev.Client.Close("disconnect.loginfailure.phoenixunreachable");

                        return;
                    }

                    try
                    {
                        // Find player
                        Player plr = ServiceManager.GetService<PlayerManagerService>().GetOnlinePlayer(response.accountID);

                        // Add session
                        GridSession ses = new GridSession(response.accountID, response.token, Server);
                        plr.AddObject(ses);

                        // Success
                        ev.EventArgs.ClientOutput.WriteBoolean(true);
                    }
                    catch
                    {
                        // Connect failure
                        ev.EventArgs.ClientOutput.WriteBoolean(false);

                        // Write reason
                        ev.EventArgs.ClientOutput.WriteBoolean(true);
                        ev.EventArgs.ClientOutput.WriteString("disconnect.loginfailure.authfailure");
                        ev.EventArgs.ClientOutput.WriteInt(0);
                        ev.Client.Close("disconnect.loginfailure.authfailure");
                    }
                }
            }
            catch
            {
                if (ev.Client.IsConnected())
                    ev.Client.Close();
            }
        }
    }
}