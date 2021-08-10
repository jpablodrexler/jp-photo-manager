using System.Reflection;

namespace JPPhotoManager.Domain
{
    public interface IUserConfigurationService
    {
        string GetPicturesDirectory();
        string GetOneDriveDirectory();
        void SetAsWallpaper(Asset asset, WallpaperStyle style);
        AboutInformation GetAboutInformation(Assembly assembly);
        string GetInitialFolder();
        string GetApplicationDataFolder();
        int GetCatalogBatchSize();
        int GetCatalogCooldownMinutes();
        string[] GetRootCatalogFolderPaths();
    }
}
