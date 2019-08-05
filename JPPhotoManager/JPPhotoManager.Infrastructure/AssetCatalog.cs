using JPPhotoManager.Domain;
using Newtonsoft.Json;
using System.Collections.Generic;

namespace JPPhotoManager.Infrastructure
{
    public class AssetCatalog
    {
        [JsonIgnore]
        public bool HasChanges { get; set; }
        public double StorageVersion { get; set; }
        public List<Folder> Folders { get; private set; }
        public List<Asset> Assets { get; private set; }

        public AssetCatalog()
        {
            this.Folders = new List<Folder>();
            this.Assets = new List<Asset>();
        }
    }
}
