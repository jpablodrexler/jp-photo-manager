using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Application
{
    public interface IJPPhotoManagerApplication
    {
        Asset[] GetAssets(string directory);
        void CatalogImages(CatalogChangeCallback callback);
        void SetAsWallpaper(Asset asset, WallpaperStyle style);
        List<DuplicatedAssetCollection> GetDuplicatedAssets();
        void DeleteAsset(Asset asset, bool deleteFile);
        AboutInformation GetAboutInformation(Assembly assembly);
        Folder[] GetDrives();
        Folder[] GetFolders(Folder parentFolder, bool includeHidden);
        string GetInitialFolder();
        int GetCatalogCooldownMinutes();
        bool MoveAsset(Asset asset, Folder destinationFolder, bool preserveOriginalFile);
        BitmapImage LoadBitmapImage(string imagePath);
    }
}
