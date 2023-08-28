using Phoenix.Server;
using Phoenix.Server.Players;

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
    }

}