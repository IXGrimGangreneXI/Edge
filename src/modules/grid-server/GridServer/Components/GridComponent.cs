using Phoenix.Server;
using Phoenix.Server.Players;
using Phoenix.Common.Tasks;
using System;
using Phoenix.Common.Logging;

namespace GridServer.Components
{

    public class GridComponent : ServerComponent
    {
        public override string ID => "grid";
        protected override string ConfigurationKey => "gridserver";

        protected override void Define()
        {
            DependsOn("player-manager");
            DependsOn("task-manager");
        }

        public override void PreInit()
        {
            // Disable player limit
            ServiceManager.GetService<PlayerManagerService>().EnablePlayerLimit = false;
            ServiceManager.GetService<PermissionManagerService>().ShouldGrantOperatorPermissionsToOwner = false;
        }

        public override void Init()
        {
            // Periodic penalty check
            long last = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            ServiceManager.GetService<TaskManager>().Repeat(() =>
            {
                if (DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - last >= 30000)
                {
                    last = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                    Player[] players = ServiceManager.GetService<PlayerManagerService>().Players;

                    // Go through players
                    foreach (Player plr in players)
                    {
                        try
                        {
                            if (plr.IsBanned)
                            {
                                // Player is banned but online
                                // Retrieve ban type
                                PlayerDataShard banShard = plr.PlayerData.GetShard("ban");
                                bool temporary = banShard.GetBool("temporary");
                                bool hasReason = banShard.GetBool("hasreason");
                                if (temporary)
                                {
                                    long unban = banShard.GetLong("unbanat");
                                    DateTimeOffset unbanTime = DateTimeOffset.FromUnixTimeSeconds(unban);
                                    if (!hasReason)
                                    {
                                        if (plr.IsConnected)
                                            plr.Disconnect("disconnect.tempbanned.undefined", unbanTime.DateTime.ToLongDateString(), unbanTime.DateTime.ToShortTimeString());
                                        Logger.GetLogger("player-manager").Info("Temporarily banned " + plr.DisplayName + " until " + unbanTime.DateTime.ToLongDateString() + " at " + unbanTime.DateTime.ToShortTimeString());
                                    }
                                    else
                                    {
                                        if (plr.IsConnected)
                                            plr.Disconnect("disconnect.tempbanned", unbanTime.DateTime.ToLongDateString(), unbanTime.DateTime.ToShortTimeString(), banShard.GetString("reason"));
                                        Logger.GetLogger("player-manager").Info("Temporarily banned " + plr.DisplayName + " until " + unbanTime.DateTime.ToLongDateString() + " at " + unbanTime.DateTime.ToShortTimeString() + ": " + banShard.GetString("reason"));
                                    }
                                }
                                else
                                {
                                    if (!hasReason)
                                    {
                                        if (plr.IsConnected)
                                            plr.Disconnect("disconnect.banned.undefined");
                                        Logger.GetLogger("player-manager").Info("Banned " + plr.DisplayName);
                                    }
                                    else
                                    {
                                        if (plr.IsConnected)
                                            plr.Disconnect("disconnect.banned", banShard.GetString("reason"));
                                        Logger.GetLogger("player-manager").Info("Banned " + plr.DisplayName + ": " + banShard.GetString("reason"));
                                    }
                                }
                            }
                        }
                        catch { }
                    }   
                }
            });
        }
    }

}