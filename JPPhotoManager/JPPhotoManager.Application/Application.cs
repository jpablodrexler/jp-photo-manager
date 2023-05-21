using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;
using System.Reflection;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Application
{
    public class Application : IApplication
    {
        private readonly IAssetRepository _assetRepository;
        private readonly ISyncAssetsService _syncAssetsService;
        private readonly ICatalogAssetsService _catalogAssetsService;
        private readonly IMoveAssetsService _moveAssetsService;
        private readonly IFindDuplicatedAssetsService _findDuplicatedAssetsService;
        private readonly IUserConfigurationService _userConfigurationService;
        private readonly IStorageService _storageService;
        private readonly IBatchRenameService _batchRenameService;
        private readonly IProcessService _processService;
        private readonly INewReleaseNotificationService _newReleaseNotificationService;

        public Application(
            ISyncAssetsService syncAssetsService,
            ICatalogAssetsService catalogAssetsService,
            IMoveAssetsService moveAssetsService,
            IFindDuplicatedAssetsService findDuplicatedAssetsService,
            IAssetRepository assetRepository,
            IUserConfigurationService userConfigurationService,
            IStorageService storageService,
            IBatchRenameService batchRenameService,
            IProcessService processService,
            INewReleaseNotificationService newReleaseNotificationService)
        {
            _syncAssetsService = syncAssetsService;
            _catalogAssetsService = catalogAssetsService;
            _moveAssetsService = moveAssetsService;
            _findDuplicatedAssetsService = findDuplicatedAssetsService;
            _assetRepository = assetRepository;
            _userConfigurationService = userConfigurationService;
            _storageService = storageService;
            _batchRenameService = batchRenameService;
            _processService = processService;
            _newReleaseNotificationService = newReleaseNotificationService;
        }

        public PaginatedData<Asset> GetAssets(string directory, int pageIndex)
        {
            if (string.IsNullOrWhiteSpace(directory))
            {
                throw new ArgumentException("Directory cannot be null or empty.", directory);
            }

            if (!_assetRepository.FolderExists(directory))
            {
                _assetRepository.AddFolder(directory);
            }

            return _assetRepository.GetAssets(directory, pageIndex);
        }

        public void LoadThumbnail(Asset asset)
        {
            asset.ImageData = _assetRepository.LoadThumbnail(asset.Folder.Path, asset.FileName, asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight);
        }

        public SyncAssetsConfiguration GetSyncAssetsConfiguration()
        {
            return _assetRepository.GetSyncAssetsConfiguration();
        }

        public void SetSyncAssetsConfiguration(SyncAssetsConfiguration syncConfiguration)
        {
            syncConfiguration.Validate();
            syncConfiguration.Normalize();
            _assetRepository.SaveSyncAssetsConfiguration(syncConfiguration);
            _assetRepository.SaveCatalog(null);
        }

        public async Task<List<SyncAssetsResult>> SyncAssetsAsync(ProcessStatusChangedCallback callback) => await _syncAssetsService.ExecuteAsync(callback);

        public async Task CatalogAssetsAsync(CatalogChangeCallback callback) => await _catalogAssetsService.CatalogAssetsAsync(callback);

        public void SetAsWallpaper(Asset asset, WallpaperStyle style)
        {
            if (asset != null)
            {
                _userConfigurationService.SetAsWallpaper(asset, style);
            }
        }

        /// <summary>
        /// Detects duplicated assets in the catalog.
        /// </summary>
        /// <returns>A list of duplicated sets of assets (corresponding to the same image),
        /// where each item is a list of duplicated assets.</returns>
        public List<List<Asset>> GetDuplicatedAssets() => _findDuplicatedAssetsService.GetDuplicatedAssets();

        public void DeleteAssets(Asset[] assets, bool deleteFiles) => _moveAssetsService.DeleteAssets(assets, deleteFiles);

        public AboutInformation GetAboutInformation(Assembly assembly) => _userConfigurationService.GetAboutInformation(assembly);

        public Folder[] GetDrives() => _storageService.GetDrives();

        public Folder[] GetSubFolders(Folder parentFolder, bool includeHidden) => _assetRepository.GetSubFolders(parentFolder, includeHidden);

        public string GetInitialFolder() => _userConfigurationService.GetInitialFolder();

        public int GetCatalogCooldownMinutes() => _userConfigurationService.GetCatalogCooldownMinutes();

        public bool MoveAssets(Asset[] assets, Folder destinationFolder, bool preserveOriginalFiles) => _moveAssetsService.MoveAssets(assets, destinationFolder, preserveOriginalFiles);

        public BitmapImage LoadBitmapImage(string imagePath, Rotation rotation) => _storageService.LoadBitmapImage(imagePath, rotation);

        public bool FileExists(string fullPath) => _storageService.FileExists(fullPath);

        public List<string> GetRecentTargetPaths() => _assetRepository.GetRecentTargetPaths();

        public Folder[] GetRootCatalogFolders()
        {
            string[] paths = _userConfigurationService.GetRootCatalogFolderPaths();
            Folder[] folders = new Folder[paths.Length];

            for (int i = 0; i < paths.Length; i++)
            {
                folders[i] = _assetRepository.GetFolderByPath(paths[i]);

                if (folders[i] == null)
                {
                    folders[i] = _assetRepository.AddFolder(paths[i]);
                }
            }

            return folders;
        }

        public bool IsAlreadyRunning() => _processService.IsAlreadyRunning();

        public async Task<Release> CheckNewReleaseAsyc() => await _newReleaseNotificationService.CheckNewReleaseAsync();

        public BatchRenameResult BatchRename(Asset[] sourceAssets, string batchFormat, bool overwriteExistingTargetFiles) => _batchRenameService.BatchRename(sourceAssets, batchFormat, overwriteExistingTargetFiles);
    }
}
