namespace JPPhotoManager.Domain
{
    public class AssetCatalog
    {
        public bool HasChanges { get; set; }
        public int StorageVersion { get; set; }
        public List<Folder> Folders { get; }
        public List<Asset> Assets { get; }
        public ImportNewAssetsConfiguration ImportNewAssetsConfiguration { get; set; }
        public List<string> RecentTargetPaths { get; set; }

        public AssetCatalog()
        {
            Folders = new List<Folder>();
            Assets = new List<Asset>();
            ImportNewAssetsConfiguration = new ImportNewAssetsConfiguration();
            RecentTargetPaths = new List<string>();
        }
    }
}
