﻿using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain.Interfaces
{
    public interface IAssetRepository
    {
        PaginatedData<Asset> GetAssets(string directory, int pageIndex);
        void AddAsset(Asset asset, byte[] thumbnailData);
        Folder AddFolder(string path);
        bool FolderExists(string path);
        Folder[] GetFolders();
        Folder[] GetSubFolders(Folder parentFolder, bool includeHidden);
        Folder GetFolderByPath(string path);
        List<Asset> GetCataloguedAssets();
        List<Asset> GetCataloguedAssets(string directory);
        bool IsAssetCatalogued(string directoryName, string fileName);
        void DeleteAsset(string directory, string deletedFileName);
        void DeleteFolder(Folder folder);
        bool ContainsThumbnail(string directoryName, string fileName);
        BitmapImage LoadThumbnail(string directoryName, string fileName, int width, int height);
        SyncAssetsConfiguration GetSyncAssetsConfiguration();
        void SaveSyncAssetsConfiguration(SyncAssetsConfiguration syncAssetsConfiguration);
        List<string> GetRecentTargetPaths();
        void SaveRecentTargetPaths(List<string> recentTargetPaths);
        void DeleteThumbnail(string thumbnailBlobName);
        string[] GetThumbnailsList();
    }
}
