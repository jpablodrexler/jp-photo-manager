using JPPhotoManager.Domain.Entities;
using System.Reflection;

namespace JPPhotoManager.Domain.Interfaces.Services
{
    public interface IUserConfigurationService
    {
        string GetAppFilesDirectory();
        string GetBinaryFilesDirectory();
        string GetDatabaseDirectory();
        string GetPicturesDirectory();
        string GetOneDriveDirectory();
        void SetAsWallpaper(Asset asset, WallpaperStyle style);
        AboutInformation GetAboutInformation(Assembly assembly);
        string GetInitialFolder();
        string GetApplicationDataFolder();
        int GetCatalogBatchSize();
        int GetCatalogCooldownMinutes();
        int GetThumbnailsDictionaryEntriesToKeep();
        string[] GetRootCatalogFolderPaths();
        string GetRepositoryOwner();
        string GetRepositoryName();
    }
}
