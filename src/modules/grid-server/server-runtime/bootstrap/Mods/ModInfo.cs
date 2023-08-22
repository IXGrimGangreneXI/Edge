using Phoenix.Server.Bootstrapper.Packages;
using System.Reflection;

namespace Phoenix.Server
{
    /// <summary>
    /// Mod information type
    /// </summary>
    public class ModInfo
    {
        private string id;
        private string version;
        private BinaryPackage package;
        private Assembly assembly;

        public ModInfo(string id, string version, BinaryPackage package, Assembly assembly)
        {
            this.id = id;
            this.version = version;
            this.package = package;
            this.assembly = assembly;
        }

        /// <summary>
        /// Retrieves the mod ID
        /// </summary>
        public string ID { 
            get 
            {
                return id;
            }
        }

        /// <summary>
        /// Retrieves the mod version string
        /// </summary>
        public string Version
        {
            get
            {
                return version;
            }
        }
        
        /// <summary>
        /// Retrieves the mod binary package
        /// </summary>
        public BinaryPackage Package
        {
            get
            {
                return package;
            }
        }

        /// <summary>
        /// Retrieves the mod assembly
        /// </summary>
        public Assembly Assembly
        {
            get
            {
                return assembly;
            }
        }
    }
}
