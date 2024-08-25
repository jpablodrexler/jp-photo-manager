using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Domain.Interfaces.Repositories;
using JPPhotoManager.Domain.Interfaces.Services;
using System.Reflection;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Application
{
    public class Application : IApplication
    {
        private readonly IAssetRepository _assetRepository;
        private readonly IFolderRepository _folderRepository;
        private readonly IRecentTargetPathRepository _recentTargetPathRepository;
        private readonly ISyncAssetsConfigurationRepository _syncAssetsConfigurationRepository;
        private readonly ISyncAssetsService _syncAssetsService;
        private readonly IConvertAssetsConfigurationRepository _convertAssetsConfigurationRepository;
        private readonly IConvertAssetsService _convertAssetsService;
        private readonly ICatalogAssetsService _catalogAssetsService;
        private readonly IMoveAssetsService _moveAssetsService;
        private readonly IFindDuplicatedAssetsService _findDuplicatedAssetsService;
        private readonly IUserConfigurationService _userConfigurationService;
        private readonly IStorageService _storageService;
        private readonly IBatchRenameService _batchRenameService;
        private readonly IProcessService _processService;
        private readonly INewReleaseNotificationService _newReleaseNotificationService;
        private readonly Queue<string> _recentThumbnailsQueue;

        public Application(
            ISyncAssetsService syncAssetsService,
            IConvertAssetsService convertAssetsService,
            ICatalogAssetsService catalogAssetsService,
            IMoveAssetsService moveAssetsService,
            IFindDuplicatedAssetsService findDuplicatedAssetsService,
            IAssetRepository assetRepository,
            IFolderRepository folderRepository,
            IRecentTargetPathRepository recentTargetPathRepository,
            ISyncAssetsConfigurationRepository syncAssetsConfigurationRepository,
            IConvertAssetsConfigurationRepository convertAssetsConfigurationRepository,
            IUserConfigurationService userConfigurationService,
            IStorageService storageService,
            IBatchRenameService batchRenameService,
            IProcessService processService,
            INewReleaseNotificationService newReleaseNotificationService)
        {
            _syncAssetsService = syncAssetsService;
            _convertAssetsService = convertAssetsService;
            _catalogAssetsService = catalogAssetsService;
            _moveAssetsService = moveAssetsService;
            _findDuplicatedAssetsService = findDuplicatedAssetsService;
            _assetRepository = assetRepository;
            _folderRepository = folderRepository;
            _recentTargetPathRepository = recentTargetPathRepository;
            _syncAssetsConfigurationRepository = syncAssetsConfigurationRepository;
            _convertAssetsConfigurationRepository = convertAssetsConfigurationRepository;
            _userConfigurationService = userConfigurationService;
            _storageService = storageService;
            _batchRenameService = batchRenameService;
            _processService = processService;
            _newReleaseNotificationService = newReleaseNotificationService;
            _recentThumbnailsQueue = new Queue<string>();
        }

        public PaginatedData<Asset> GetAssets(string directory, int pageIndex)
        {
            if (string.IsNullOrWhiteSpace(directory))
            {
                throw new ArgumentException("Directory cannot be null or empty.", directory);
            }

            if (!_folderRepository.FolderExists(directory))
            {
                _folderRepository.AddFolder(directory);
            }

            Folder folder = _folderRepository.GetFolderByPath(directory);
            RemoveOldThumbnailsDictionaryEntries(folder);
            
            return _assetRepository.GetAssets(folder, pageIndex);
        }

        private void RemoveOldThumbnailsDictionaryEntries(Folder folder)
        {
            int entriesToKeep = _userConfigurationService.GetThumbnailsDictionaryEntriesToKeep();

            if (!_recentThumbnailsQueue.Contains(folder.Path))
            {
                _recentThumbnailsQueue.Enqueue(folder.Path);
            }

            if (_recentThumbnailsQueue.Count > entriesToKeep)
            {
                var pathToRemove = _recentThumbnailsQueue.Dequeue();
                var folderToRemove = _folderRepository.GetFolderByPath(pathToRemove);
                var assets = _assetRepository.GetAssetsByFolderId(folderToRemove.FolderId);

                foreach (var asset in assets)
                {
                    _assetRepository.RemoveThumbnailCache(asset.ThumbnailBlobName);
                }
            }
        }

        public void LoadThumbnail(Asset asset)
        {
            var folder = _folderRepository.GetFolderByPath(asset.Folder.Path);
            asset.ImageData = _assetRepository.LoadThumbnail(folder, asset.FileName, asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight);
        }

        public SyncAssetsConfiguration GetSyncAssetsConfiguration()
        {
            return _syncAssetsConfigurationRepository.GetSyncAssetsConfiguration();
        }

        public void SetSyncAssetsConfiguration(SyncAssetsConfiguration syncConfiguration)
        {
            syncConfiguration.Validate();
            syncConfiguration.Normalize();
            _syncAssetsConfigurationRepository.SaveSyncAssetsConfiguration(syncConfiguration);
        }

        public async Task<List<SyncAssetsResult>> SyncAssetsAsync(ProcessStatusChangedCallback callback) => await _syncAssetsService.ExecuteAsync(callback);

        public ConvertAssetsConfiguration GetConvertAssetsConfiguration()
        {
            return _convertAssetsConfigurationRepository.GetConvertAssetsConfiguration();
        }

        public void SetConvertAssetsConfiguration(ConvertAssetsConfiguration convertConfiguration)
        {
            convertConfiguration.Validate();
            convertConfiguration.Normalize();
            _convertAssetsConfigurationRepository.SaveConvertAssetsConfiguration(convertConfiguration);
        }

        public async Task<List<ConvertAssetsResult>> ConvertAssetsAsync(ProcessStatusChangedCallback callback) => await _convertAssetsService.ExecuteAsync(callback);

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

        public Folder[] GetSubFolders(Folder parentFolder, bool includeHidden) => _folderRepository.GetSubFolders(parentFolder, includeHidden);

        public string GetInitialFolder() => _userConfigurationService.GetInitialFolder();

        public int GetCatalogCooldownMinutes() => _userConfigurationService.GetCatalogCooldownMinutes();

        public bool MoveAssets(Asset[] assets, Folder destinationFolder, bool preserveOriginalFiles) => _moveAssetsService.MoveAssets(assets, destinationFolder, preserveOriginalFiles);

        public BitmapImage LoadBitmapImage(string imagePath, Rotation rotation) => _storageService.LoadBitmapImage(imagePath, rotation);

        public bool FileExists(string fullPath) => _storageService.FileExists(fullPath);

        public List<string> GetRecentTargetPaths() => _recentTargetPathRepository.GetRecentTargetPaths();

        public Folder[] GetRootCatalogFolders()
        {
            string[] paths = _userConfigurationService.GetRootCatalogFolderPaths();
            Folder[] folders = new Folder[paths.Length];

            for (int i = 0; i < paths.Length; i++)
            {
                folders[i] = _folderRepository.GetFolderByPath(paths[i]);

                if (folders[i] == null)
                {
                    folders[i] = _folderRepository.AddFolder(paths[i]);
                }
            }

            return folders;
        }

        public bool IsAlreadyRunning() => _processService.IsAlreadyRunning();

        public async Task<Release> CheckNewReleaseAsyc() => await _newReleaseNotificationService.CheckNewReleaseAsync();

        public BatchRenameResult BatchRename(Asset[] sourceAssets, string batchFormat, bool overwriteExistingTargetFiles) => _batchRenameService.BatchRename(sourceAssets, batchFormat, overwriteExistingTargetFiles);
    }
}
