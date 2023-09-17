using System.Reflection;

namespace JPPhotoManager.Domain.Interfaces
{
    public interface IUserConfigurationService
    {
        string GetAppFilesDirectory();
        string GetBinaryFilesDirectory();
        string GetPicturesDirectory();
        string GetOneDriveDirectory();
        void SetAsWallpaper(Asset asset, WallpaperStyle style);
        AboutInformation GetAboutInformation(Assembly assembly);
        string GetInitialFolder();
        string GetApplicationDataFolder();
        int GetCatalogBatchSize();
        int GetCatalogCooldownMinutes();
        int GetBackupsToKeep();
        int GetThumbnailsDictionaryEntriesToKeep();
        string[] GetRootCatalogFolderPaths();
        string GetRepositoryOwner();
        string GetRepositoryName();
        int GetBackupEveryNDays();
    }
}
