using JPPhotoManager.Domain.Interfaces;
using System.IO;

namespace JPPhotoManager.Domain
{
    public class MoveAssetsService : IMoveAssetsService
    {
        private const int RECENT_TARGET_PATHS_MAX_COUNT = 20;
        private readonly IAssetRepository _assetRepository;
        private readonly IStorageService _storageService;

        public MoveAssetsService(
            IAssetRepository assetRepository,
            IStorageService storageService)
        {
            _assetRepository = assetRepository;
            _storageService = storageService;
        }

        public bool MoveAssets(Asset[] assets, Folder destinationFolder, bool preserveOriginalFiles)
        {
            #region Parameters validation

            if (assets == null || assets.Length == 0)
            {
                throw new ArgumentNullException(nameof(assets), "Assets cannot be null.");
            }

            foreach (Asset asset in assets)
            {
                if (asset == null)
                {
                    throw new ArgumentNullException(nameof(assets), "asset cannot be null.");
                }

                if (asset.Folder == null)
                {
                    throw new ArgumentNullException(nameof(assets), "asset.Folder cannot be null.");
                }

                if (destinationFolder == null)
                {
                    throw new ArgumentNullException(nameof(destinationFolder), "destinationFolder cannot be null.");
                }
            }

            #endregion

            bool result = false;
            var folder = _assetRepository.GetFolderByPath(destinationFolder.Path);

            // If the folder is null, it means is not present in the catalog.
            // TODO: IF THE DESTINATION FOLDER IS NEW, THE FOLDER NAVIGATION CONTROL SHOULD DISPLAY IT WHEN THE USER GOES BACK TO THE MAIN WINDOW.
            bool isDestinationFolderInCatalog = folder != null;

            if (isDestinationFolderInCatalog)
            {
                destinationFolder = folder;
            }

            foreach (Asset asset in assets)
            {
                string sourcePath = asset.FullPath;
                string destinationPath = Path.Combine(destinationFolder.Path, asset.FileName);

                if (!_storageService.FileExists(sourcePath))
                {
                    // This could happen if an image was moved or deleted outside the app.
                    // TODO: Instead of just failing, should remove the file from the catalog.
                    throw new ArgumentException(sourcePath);
                }

                if (_storageService.FileExists(sourcePath) && !_storageService.FileExists(destinationPath))
                {
                    result = _storageService.CopyImage(sourcePath, destinationPath);

                    if (!result)
                    {
                        break;
                    }

                    if (result && !isDestinationFolderInCatalog)
                    {
                        destinationFolder = _assetRepository.AddFolder(destinationFolder.Path);
                        isDestinationFolderInCatalog = true;
                    }
                }
            }

            if (result)
            {
                if (!preserveOriginalFiles)
                {
                    DeleteAssets(assets, deleteFiles: true, saveCatalog: false);
                }

                AddTargetPathToRecent(destinationFolder);
                _assetRepository.SaveCatalog(destinationFolder);
            }

            return result;
        }

        // TODO: Extend automated tests to evaluate recent target paths.
        private void AddTargetPathToRecent(Folder destinationFolder)
        {
            List<string> recentTargetPaths = _assetRepository.GetRecentTargetPaths();

            if (recentTargetPaths.Contains(destinationFolder.Path))
                recentTargetPaths.Remove(destinationFolder.Path);

            recentTargetPaths.Insert(0, destinationFolder.Path);

            recentTargetPaths = recentTargetPaths.Take(RECENT_TARGET_PATHS_MAX_COUNT).ToList();
            _assetRepository.SaveRecentTargetPaths(recentTargetPaths);
        }

        public void DeleteAssets(Asset[] assets, bool deleteFiles, bool saveCatalog = true)
        {
            #region Parameters validation

            if (assets == null || assets.Length == 0)
            {
                throw new ArgumentNullException(nameof(assets), "Assets cannot be null.");
            }

            foreach (Asset asset in assets)
            {
                if (asset == null)
                {
                    throw new ArgumentNullException(nameof(asset), "Asset cannot be null.");
                }

                if (asset.Folder == null)
                {
                    throw new ArgumentNullException(nameof(asset), "Asset.Folder cannot be null.");
                }

                if (deleteFiles && !_storageService.FileExists(asset, asset.Folder))
                {
                    throw new ArgumentException("File does not exist: " + asset.FullPath);
                }
            }

            #endregion

            foreach (Asset asset in assets)
            {
                _assetRepository.DeleteAsset(asset.Folder.Path, asset.FileName);

                if (deleteFiles)
                {
                    _storageService.DeleteFile(asset.Folder.Path, asset.FileName);
                }
            }

            if (saveCatalog)
            {
                _assetRepository.SaveCatalog(assets.FirstOrDefault()?.Folder);
            }
        }
    }
}
