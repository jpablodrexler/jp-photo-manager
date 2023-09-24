using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain.Interfaces
{
    public interface IAssetRepository
    {
        PaginatedData<Asset> GetAssets(Folder folder, int pageIndex);
        void AddAsset(Asset asset, Folder folder, byte[] thumbnailData);
        List<Asset> GetCataloguedAssets();
        List<Asset> GetCataloguedAssets(Folder folder);
        bool IsAssetCatalogued(Folder folder, string fileName);
        void DeleteAsset(Folder folder, string deletedFileName);
        bool ContainsThumbnail(string directoryName, string fileName);
        BitmapImage LoadThumbnail(Folder folder, string fileName, int width, int height);
        SyncAssetsConfiguration GetSyncAssetsConfiguration();
        void SaveSyncAssetsConfiguration(SyncAssetsConfiguration syncAssetsConfiguration);
        List<string> GetRecentTargetPaths();
        void SaveRecentTargetPaths(List<string> recentTargetPaths);
        void DeleteThumbnail(string thumbnailBlobName);
        string[] GetThumbnailsList();
    }
}
