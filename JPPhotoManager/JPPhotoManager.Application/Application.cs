using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;
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
        private readonly IProcessService processService;
        private readonly INewReleaseNotificationService newReleaseNotificationService;

        public Application(
            IImportNewAssetsService importNewAssetsService,
            ICatalogAssetsService catalogAssetsService,
            IMoveAssetsService moveAssetsService,
            IFindDuplicatedAssetsService findDuplicatedAssetsService,
            IAssetRepository assetRepository,
            IUserConfigurationService userConfigurationService,
            IStorageService storageService,
            IProcessService processService,
            INewReleaseNotificationService newReleaseNotificationService)
        {
            this.importNewAssetsService = importNewAssetsService;
            this.catalogAssetsService = catalogAssetsService;
            this.moveAssetsService = moveAssetsService;
            this.findDuplicatedAssetsService = findDuplicatedAssetsService;
            this.assetRepository = assetRepository;
            this.userConfigurationService = userConfigurationService;
            this.storageService = storageService;
            this.processService = processService;
            this.newReleaseNotificationService = newReleaseNotificationService;
        }

        public Asset[] GetAssets(string directory)
        {
            if (string.IsNullOrWhiteSpace(directory))
            {
                throw new ArgumentException("Directory cannot be null or empty.", directory);
            }

            if (!assetRepository.FolderExists(directory))
            {
                assetRepository.AddFolder(directory);
            }

            return assetRepository.GetAssets(directory);
        }

        public void LoadThumbnail(Asset asset)
        {
            asset.ImageData = assetRepository.LoadThumbnail(asset.Folder.Path, asset.FileName, asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight);
        }

        public ImportNewAssetsConfiguration GetImportNewAssetsConfiguration()
        {
            return assetRepository.GetImportNewAssetsConfiguration();
        }

        public void SetImportNewAssetsConfiguration(ImportNewAssetsConfiguration importConfiguration)
        {
            importConfiguration.Validate();
            importConfiguration.Normalize();
            assetRepository.SetImportNewAssetsConfiguration(importConfiguration);
            assetRepository.SaveCatalog(null);
        }

        public BatchRenameResult BatchRename(Asset[] sourceAssets, string batchFormat)
        {
            BatchRenameResult batchRenameResult = new ();

            for (int i = 0; i < sourceAssets.Length; i++)
            {
                string newName = sourceAssets[i].ComputeTargetFileName(batchFormat, i + 1);
                batchRenameResult.SourceAssets.Add(sourceAssets[i]);
                batchRenameResult.TargetFileNames.Add(newName);
            }

            return batchRenameResult;
        }

        public async Task<List<ImportNewAssetsResult>> ImportNewAssets(StatusChangeCallback callback) => await importNewAssetsService.Import(callback);

        public async Task CatalogAssets(CatalogChangeCallback callback) => await catalogAssetsService.CatalogAssets(callback);

        public void SetAsWallpaper(Asset asset, WallpaperStyle style)
        {
            if (asset != null)
            {
                userConfigurationService.SetAsWallpaper(asset, style);
            }
        }

        /// <summary>
        /// Detects duplicated assets in the catalog.
        /// </summary>
        /// <returns>A list of duplicated sets of assets (corresponding to the same image),
        /// where each item is a list of duplicated assets.</returns>
        public List<List<Asset>> GetDuplicatedAssets() => findDuplicatedAssetsService.GetDuplicatedAssets();

        public void DeleteAssets(Asset[] assets, bool deleteFiles) => moveAssetsService.DeleteAssets(assets, deleteFiles);

        public AboutInformation GetAboutInformation(Assembly assembly) => userConfigurationService.GetAboutInformation(assembly);

        public Folder[] GetDrives() => storageService.GetDrives();

        public Folder[] GetSubFolders(Folder parentFolder, bool includeHidden) => assetRepository.GetSubFolders(parentFolder, includeHidden);

        public string GetInitialFolder() => userConfigurationService.GetInitialFolder();

        public int GetCatalogCooldownMinutes() => userConfigurationService.GetCatalogCooldownMinutes();

        public bool MoveAssets(Asset[] assets, Folder destinationFolder, bool preserveOriginalFiles) => moveAssetsService.MoveAssets(assets, destinationFolder, preserveOriginalFiles);

        public BitmapImage LoadBitmapImage(string imagePath, Rotation rotation) => storageService.LoadBitmapImage(imagePath, rotation);

        public bool FileExists(string fullPath) => storageService.FileExists(fullPath);

        public List<string> GetRecentTargetPaths() => assetRepository.GetRecentTargetPaths();

        public Folder[] GetRootCatalogFolders()
        {
            string[] paths = userConfigurationService.GetRootCatalogFolderPaths();
            Folder[] folders = new Folder[paths.Length];

            for (int i = 0; i < paths.Length; i++)
            {
                folders[i] = assetRepository.GetFolderByPath(paths[i]);

                if (folders[i] == null)
                {
                    folders[i] = assetRepository.AddFolder(paths[i]);
                }
            }

            return folders;
        }

        public bool IsAlreadyRunning() => processService.IsAlreadyRunning();

        public async Task<Release> CheckNewRelease() => await newReleaseNotificationService.CheckNewRelease();
    }
}
