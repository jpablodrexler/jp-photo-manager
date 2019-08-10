using System.Collections.Generic;

namespace JPPhotoManager.Domain
{
    public interface IAssetRepository
    {
        void Initialize(string dataDirectory = "");
        Asset[] GetAssets(string directory);
        void AddAsset(Asset asset);
        Folder AddFolder(string path);
        bool FolderExists(string path);
        Folder[] GetFolders();
        Folder GetFolderByPath(string path);
        Dictionary<string, byte[]> GetThumbnails(string thumbnailsFileName, out bool isNewFile);
        void SaveCatalog(Dictionary<string, byte[]> thumbnails, string thumbnailsFileName);
        List<Asset> GetCataloguedAssets();
        List<Asset> GetCataloguedAssets(string directory);
        bool IsAssetCatalogued(string directoryName, string fileName);
        void DeleteAsset(string directory, string deletedFileName);
        bool HasChanges();
    }
}
