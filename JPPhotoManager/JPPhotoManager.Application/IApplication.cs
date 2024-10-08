﻿using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Entities;
using System.Reflection;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Application
{
    public interface IApplication
    {
        PaginatedData<Asset> GetAssets(string directory, int pageIndex);
        void LoadThumbnail(Asset asset);
        SyncAssetsConfiguration GetSyncAssetsConfiguration();
        void SetSyncAssetsConfiguration(SyncAssetsConfiguration syncConfiguration);
        Task<List<SyncAssetsResult>> SyncAssetsAsync(ProcessStatusChangedCallback callback);
        ConvertAssetsConfiguration GetConvertAssetsConfiguration();
        void SetConvertAssetsConfiguration(ConvertAssetsConfiguration convertConfiguration);
        Task<List<ConvertAssetsResult>> ConvertAssetsAsync(ProcessStatusChangedCallback callback);
        Task CatalogAssetsAsync(CatalogChangeCallback callback);
        void SetAsWallpaper(Asset asset, WallpaperStyle style);
        List<List<Asset>> GetDuplicatedAssets();
        void DeleteAssets(Asset[] assets, bool deleteFiles);
        AboutInformation GetAboutInformation(Assembly assembly);
        Folder[] GetDrives();
        Folder[] GetSubFolders(Folder parentFolder, bool includeHidden);
        string GetInitialFolder();
        int GetCatalogCooldownMinutes();
        bool MoveAssets(Asset[] assets, Folder destinationFolder, bool preserveOriginalFiles);
        BitmapImage LoadBitmapImage(string imagePath, Rotation rotation);
        bool FileExists(string fullPath);
        List<string> GetRecentTargetPaths();
        Folder[] GetRootCatalogFolders();
        bool IsAlreadyRunning();
        Task<Release> CheckNewReleaseAsyc();
        void ShrinkDatabase();
    }
}
