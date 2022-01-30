namespace JPPhotoManager.Domain
{
    public class AssetCatalog
    {
        public bool HasChanges { get; set; }
        public double StorageVersion { get; set; }
        public List<Folder> Folders { get; }
        public List<Asset> Assets { get; }
        public SyncAssetsConfiguration SyncAssetsConfiguration { get; set; }
        public List<string> RecentTargetPaths { get; set; }

        public AssetCatalog()
        {
            Folders = new List<Folder>();
            Assets = new List<Asset>();
            SyncAssetsConfiguration = new SyncAssetsConfiguration();
            RecentTargetPaths = new List<string>();
        }
    }
}
