using JPPhotoManager.Domain;
using System.Collections.Generic;
using System.Reflection;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Application
{
    public interface IApplication
    {
        Asset[] GetAssets(string directory);
        void LoadThumbnail(Asset asset);
        ImportNewAssetsConfiguration GetImportNewAssetsConfiguration();
        void SetImportNewAssetsConfiguration(ImportNewAssetsConfiguration importConfiguration);
        List<ImportNewAssetsResult> ImportNewAssets(StatusChangeCallback callback);
        void CatalogAssets(CatalogChangeCallback callback);
        void SetAsWallpaper(Asset asset, WallpaperStyle style);
        List<List<Asset>> GetDuplicatedAssets();
        void DeleteAsset(Asset asset, bool deleteFile);
        AboutInformation GetAboutInformation(Assembly assembly);
        Folder[] GetDrives();
        Folder[] GetFolders(Folder parentFolder, bool includeHidden);
        string GetInitialFolder();
        int GetCatalogCooldownMinutes();
        bool MoveAsset(Asset asset, Folder destinationFolder, bool preserveOriginalFile);
        BitmapImage LoadBitmapImage(string imagePath, Rotation rotation);
        bool FileExists(string fullPath);
        List<string> GetRecentTargetPaths();
    }
}
