using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

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
    }
}
