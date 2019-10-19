using System.Collections.Generic;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain
{
    public interface IAssetRepository
    {
        void Initialize();
        Asset[] GetAssets(string directory);
        void AddAsset(Asset asset, byte[] thumbnailData);
        Folder AddFolder(string path);
        bool FolderExists(string path);
        Folder[] GetFolders();
        Folder GetFolderByPath(string path);
        void SaveCatalog(Folder folder);
        List<Asset> GetCataloguedAssets();
        List<Asset> GetCataloguedAssets(string directory);
        bool IsAssetCatalogued(string directoryName, string fileName);
        void DeleteAsset(string directory, string deletedFileName);
        bool HasChanges();
        bool ContainsThumbnail(string directoryName, string fileName);
        BitmapImage LoadThumbnail(string directoryName, string fileName);
        bool FolderHasThumbnails(Folder folder);
    }
}
