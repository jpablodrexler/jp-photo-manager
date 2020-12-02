using JPPhotoManager.Domain;
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
        Task<List<ImportNewAssetsResult>> ImportNewAssetsAsync(ProcessStatusChangedCallback callback);
        Task CatalogAssetsAsync(CatalogChangeCallback callback);
        void SetAsWallpaper(Asset asset, WallpaperStyle style);
        List<List<Asset>> GetDuplicatedAssets();
        void DeleteAssets(Asset[] assets, bool deleteFiles);
        void RemoveDuplicatesFromParentFolder(List<List<Asset>> duplicatedAssets);
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
    }
}
