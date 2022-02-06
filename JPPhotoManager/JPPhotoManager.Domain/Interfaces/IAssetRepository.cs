using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain.Interfaces
{
    public interface IAssetRepository
    {
        Asset[] GetAssets(string directory);
        void AddAsset(Asset asset, byte[] thumbnailData);
        Folder AddFolder(string path);
        bool FolderExists(string path);
        Folder[] GetFolders();
        Folder[] GetSubFolders(Folder parentFolder, bool includeHidden);
        Folder GetFolderByPath(string path);
        void SaveCatalog(Folder folder);
        bool BackupExists();
        void WriteBackup();
        List<Asset> GetCataloguedAssets();
        List<Asset> GetCataloguedAssets(string directory);
        bool IsAssetCatalogued(string directoryName, string fileName);
        void DeleteAsset(string directory, string deletedFileName);
        void DeleteFolder(Folder folder);
        bool HasChanges();
        bool ContainsThumbnail(string directoryName, string fileName);
        BitmapImage LoadThumbnail(string directoryName, string fileName, int width, int height);
        bool FolderHasThumbnails(Folder folder);
        SyncAssetsConfiguration GetSyncAssetsConfiguration();
        void SaveSyncAssetsConfiguration(SyncAssetsConfiguration syncAssetsConfiguration);
        List<string> GetRecentTargetPaths();
        void SaveRecentTargetPaths(List<string> recentTargetPaths);
    }
}
