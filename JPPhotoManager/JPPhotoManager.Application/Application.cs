using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.Reflection;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Application
{
    public class Application : IApplication
    {
        private readonly IAssetRepository assetRepository;
        private readonly IImportNewAssetsService importNewAssetsService;
        private readonly ICatalogAssetsService catalogAssetsService;
        private readonly IMoveAssetsService moveAssetsService;
        private readonly IFindDuplicatedAssetsService findDuplicatedAssetsService;
        private readonly IUserConfigurationService userConfigurationService;
        private readonly IStorageService storageService;

        public Application(
            IImportNewAssetsService importNewAssetsService,
            ICatalogAssetsService catalogAssetsService,
            IMoveAssetsService moveAssetsService,
            IFindDuplicatedAssetsService findDuplicatedAssetsService,
            IAssetRepository assetRepository,
            IUserConfigurationService userConfigurationService,
            IStorageService storageService)
        {
            this.importNewAssetsService = importNewAssetsService;
            this.catalogAssetsService = catalogAssetsService;
            this.moveAssetsService = moveAssetsService;
            this.findDuplicatedAssetsService = findDuplicatedAssetsService;
            this.assetRepository = assetRepository;
            this.userConfigurationService = userConfigurationService;
            this.storageService = storageService;
        }

        public Asset[] GetAssets(string directory)
        {
            if (string.IsNullOrWhiteSpace(directory))
            {
                throw new ArgumentException("Directory cannot be null or empty.", directory);
            }

            if (!this.assetRepository.FolderExists(directory))
            {
                this.assetRepository.AddFolder(directory);
            }
            
            return this.assetRepository.GetAssets(directory);
        }

        public void LoadThumbnail(Asset asset)
        {
            asset.ImageData = this.assetRepository.LoadThumbnail(asset.Folder.Path, asset.FileName, asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight);
        }

        public ImportNewAssetsConfiguration GetImportNewAssetsConfiguration()
        {
            return this.assetRepository.GetImportNewAssetsConfiguration();
        }

        public void SetImportNewAssetsConfiguration(ImportNewAssetsConfiguration importConfiguration)
        {
            importConfiguration.Validate();
            importConfiguration.Normalize();
            this.assetRepository.SetImportNewAssetsConfiguration(importConfiguration);
            this.assetRepository.SaveCatalog(null);
        }

        public List<ImportNewAssetsResult> ImportNewAssets(StatusChangeCallback callback)
        {
            return this.importNewAssetsService.Import(callback);
        }

        public void CatalogAssets(CatalogChangeCallback callback)
        {
            this.catalogAssetsService.CatalogAssets(callback);
        }

        public void SetAsWallpaper(Asset asset, WallpaperStyle style)
        {
            if (asset != null)
            {
                this.userConfigurationService.SetAsWallpaper(asset, style);
            }
        }

        /// <summary>
        /// Detects duplicated assets in the catalog.
        /// </summary>
        /// <returns>A list of duplicated sets of assets (corresponding to the same image),
        /// where each item is a list of duplicated assets.</returns>
        public List<List<Asset>> GetDuplicatedAssets()
        {
            return this.findDuplicatedAssetsService.GetDuplicatedAssets();
        }

        public void DeleteAsset(Asset asset, bool deleteFile)
        {
            this.moveAssetsService.DeleteAsset(asset, deleteFile);
        }

        public AboutInformation GetAboutInformation(Assembly assembly)
        {
            return this.userConfigurationService.GetAboutInformation(assembly);
        }

        public Folder[] GetDrives()
        {
            return this.storageService.GetDrives();
        }

        public Folder[] GetFolders(Folder parentFolder, bool includeHidden)
        {
            return this.assetRepository.GetFolders(parentFolder, includeHidden);
        }

        public string GetInitialFolder()
        {
            return this.userConfigurationService.GetInitialFolder();
        }

        public int GetCatalogCooldownMinutes()
        {
            return this.userConfigurationService.GetCatalogCooldownMinutes();
        }

        public bool MoveAsset(Asset asset, Folder destinationFolder, bool preserveOriginalFile)
        {
            return this.moveAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile);
        }

        public BitmapImage LoadBitmapImage(string imagePath, Rotation rotation)
        {
            return this.storageService.LoadBitmapImage(imagePath, rotation);
        }

        public bool FileExists(string fullPath)
        {
            return this.storageService.FileExists(fullPath);
        }

        public List<string> GetRecentTargetPaths()
        {
            return this.assetRepository.GetRecentTargetPaths();
        }

        public Folder[] GetRootCatalogFolders()
        {
            string[] paths = this.userConfigurationService.GetRootCatalogFolderPaths();
            return this.assetRepository.GetFoldersByPaths(paths);
        }
    }
}
