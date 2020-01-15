using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Application
{
    public interface IApplication
    {
        Asset[] GetAssets(string directory);
        ImportNewAssetsConfiguration GetImportNewAssetsConfiguration();
        void SetImportNewAssetsConfiguration(ImportNewAssetsConfiguration importConfiguration);
        List<ImportNewAssetsResult> ImportNewImages(StatusChangeCallback callback);
        void CatalogImages(CatalogChangeCallback callback, CancellationToken token);
        void SetAsWallpaper(Asset asset, WallpaperStyle style);
        List<DuplicatedAssetCollection> GetDuplicatedAssets();
        void DeleteAsset(Asset asset, bool deleteFile);
        AboutInformation GetAboutInformation(Assembly assembly);
        Folder[] GetDrives();
        Folder[] GetFolders(Folder parentFolder, bool includeHidden);
        string GetInitialFolder();
        int GetCatalogCooldownMinutes();
        bool MoveAsset(Asset asset, Folder destinationFolder, bool preserveOriginalFile);
        BitmapImage LoadBitmapImage(string imagePath, Rotation rotation);
    }
}
