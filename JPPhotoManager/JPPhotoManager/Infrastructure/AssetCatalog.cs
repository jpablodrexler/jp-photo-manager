using JPPhotoManager.Domain;
using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace JPPhotoManager.Infrastructure
{
    public class AssetCatalog
    {
        [JsonIgnore]
        public bool HasChanges { get; set; }
        public double StorageVersion { get; set; }
        public List<Folder> Folders { get; set; }
        public List<Asset> Assets { get; set; }
        public ImportNewAssetsConfiguration ImportNewAssetsConfiguration { get; set; }

        public AssetCatalog()
        {
            this.Folders = new List<Folder>();
            this.Assets = new List<Asset>();
        }
    }
}
