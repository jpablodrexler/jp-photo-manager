using System.Reflection;

namespace JPPhotoManager.Domain.Interfaces
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
        string GetRepositoryOwner();
        string GetRepositoryName();
    }
}
