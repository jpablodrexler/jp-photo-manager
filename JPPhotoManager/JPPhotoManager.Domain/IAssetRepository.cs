using System.Collections.Generic;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain
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
        Folder[] GetFoldersByPaths(string[] paths);
        void SaveCatalog(Folder folder);
        List<Asset> GetCataloguedAssets();
        List<Asset> GetCataloguedAssets(string directory);
        bool IsAssetCatalogued(string directoryName, string fileName);
        void DeleteAsset(string directory, string deletedFileName);
        void DeleteFolder(Folder folder);
        bool HasChanges();
        bool ContainsThumbnail(string directoryName, string fileName);
        BitmapImage LoadThumbnail(string directoryName, string fileName, int width, int height);
        bool FolderHasThumbnails(Folder folder);
        ImportNewAssetsConfiguration GetImportNewAssetsConfiguration();
        void SetImportNewAssetsConfiguration(ImportNewAssetsConfiguration importConfiguration);
        List<string> GetRecentTargetPaths();
        void SetRecentTargetPaths(List<string> recentTargetPaths);
    }
}
