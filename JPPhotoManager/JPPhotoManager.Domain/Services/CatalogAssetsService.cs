using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Domain.Interfaces.Repositories;
using JPPhotoManager.Domain.Interfaces.Services;
using log4net;
using System.IO;
using System.Reflection;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain.Services
{
    public class CatalogAssetsService : ICatalogAssetsService
    {
        private readonly IAssetRepository _assetRepository;
        private readonly IFolderRepository _folderRepository;
        private readonly IAssetHashCalculatorService _assetHashCalculatorService;
        private readonly IStorageService _storageService;
        private readonly IUserConfigurationService _userConfigurationService;
        private readonly IDirectoryComparer _directoryComparer;

        private static readonly ILog _log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        private string _currentFolderPath;

        public CatalogAssetsService(
            IAssetRepository assetRepository,
            IFolderRepository folderRepository,
            IAssetHashCalculatorService assetHashCalculatorService,
            IStorageService storageService,
            IUserConfigurationService userConfigurationService,
            IDirectoryComparer directoryComparer)
        {
            _assetRepository = assetRepository;
            _folderRepository = folderRepository;
            _assetHashCalculatorService = assetHashCalculatorService;
            _storageService = storageService;
            _userConfigurationService = userConfigurationService;
            _directoryComparer = directoryComparer;
        }

        public async Task CatalogAssetsAsync(CatalogChangeCallback callback)
        {
            await Task.Run(() =>
            {
                int cataloguedAssetsBatchCount = 0;
                List<string> visitedFolders = new();

                try
                {
                    Folder[] foldersToCatalog = GetFoldersToCatalog();

                    // TODO: Since the root folders to catalog are combined in the same list
                    // with the catalogued sub-folders, the catalog process should keep a list
                    // of the already visited folders so they don't get catalogued twice
                    // in the same execution.

                    foreach (Folder folder in foldersToCatalog)
                    {
                        cataloguedAssetsBatchCount = CatalogAssets(folder.Path, callback, cataloguedAssetsBatchCount, visitedFolders);
                    }

                    DeleteUnusedThumbnails(callback);

                    callback?.Invoke(new CatalogChangeCallbackEventArgs() { Message = string.Empty });
                }
                catch (Exception ex)
                {
                    _log.Error(ex);
                    callback?.Invoke(new CatalogChangeCallbackEventArgs { Exception = ex });
                }
                finally
                {
                    callback?.Invoke(new CatalogChangeCallbackEventArgs { Message = string.Empty });
                }
            });
        }

        private Folder[] GetFoldersToCatalog()
        {
            string[] rootPaths = _userConfigurationService.GetRootCatalogFolderPaths();

            foreach (string root in rootPaths)
            {
                if (!_folderRepository.FolderExists(root))
                {
                    _folderRepository.AddFolder(root);
                }
            }

            return _folderRepository.GetFolders();
        }

        private int CatalogAssets(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount, List<string> visitedFolders)
        {
            if (!visitedFolders.Contains(directory))
            {
                _currentFolderPath = directory;
                int batchSize = _userConfigurationService.GetCatalogBatchSize();

                if (_storageService.FolderExists(directory))
                {
                    cataloguedAssetsBatchCount = CatalogExistingFolder(directory, callback, cataloguedAssetsBatchCount, batchSize, visitedFolders);
                }
                else if (!string.IsNullOrEmpty(directory) && !_storageService.FolderExists(directory))
                {
                    cataloguedAssetsBatchCount = CatalogNonExistingFolder(directory, callback, cataloguedAssetsBatchCount, batchSize);
                }

                visitedFolders.Add(directory);
            }

            return cataloguedAssetsBatchCount;
        }

        private int CatalogExistingFolder(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount, int batchSize, List<string> visitedFolders)
        {
            Folder folder;

            if (cataloguedAssetsBatchCount >= batchSize)
            {
                return cataloguedAssetsBatchCount;
            }

            if (!_folderRepository.FolderExists(directory))
            {
                folder = _folderRepository.AddFolder(directory);

                callback?.Invoke(new CatalogChangeCallbackEventArgs
                {
                    Folder = folder,
                    Message = $"Folder {directory} added to catalog",
                    Reason = ReasonEnum.FolderCreated
                });
            }

            callback?.Invoke(new CatalogChangeCallbackEventArgs() { Message = "Inspecting folder " + directory });
            string[] fileNames = _storageService.GetFileNames(directory);
            folder = _folderRepository.GetFolderByPath(directory);
            List<Asset> cataloguedAssets = _assetRepository.GetCataloguedAssets(folder);

            cataloguedAssetsBatchCount = CatalogNewAssets(directory, callback, cataloguedAssetsBatchCount, batchSize, fileNames, cataloguedAssets);
            cataloguedAssetsBatchCount = CatalogUpdatedAssets(directory, callback, cataloguedAssetsBatchCount, batchSize, fileNames, cataloguedAssets);
            cataloguedAssetsBatchCount = CatalogDeletedAssets(directory, callback, cataloguedAssetsBatchCount, batchSize, fileNames, folder, cataloguedAssets);

            if (cataloguedAssetsBatchCount < batchSize)
            {
                var subdirectories = new DirectoryInfo(directory).EnumerateDirectories();

                foreach (var subdir in subdirectories)
                {
                    cataloguedAssetsBatchCount = CatalogAssets(subdir.FullName, callback, cataloguedAssetsBatchCount, visitedFolders);
                }
            }

            return cataloguedAssetsBatchCount;
        }

        private List<Asset> DeleteUnusedThumbnails(CatalogChangeCallback callback)
        {
            callback?.Invoke(new CatalogChangeCallbackEventArgs() { Message = "Deleting unused thumbnails" });

            var thumbnailsList = _assetRepository.GetThumbnailsList();
            var thumbnailsFileNamesList = thumbnailsList.Select(t => new FileInfo(t).Name);
            var cataloguedAssets = _assetRepository.GetCataloguedAssets();
            var cataloguedAssetsThumbnails = cataloguedAssets.Select(a => a.ThumbnailBlobName);
            var thumbnailsListToDelete = thumbnailsFileNamesList.Except(cataloguedAssetsThumbnails);

            foreach (var file in thumbnailsListToDelete)
            {
                _assetRepository.DeleteThumbnail(file);
            }

            return cataloguedAssets;
        }

        private int CatalogNonExistingFolder(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount, int batchSize)
        {
            if (cataloguedAssetsBatchCount >= batchSize)
            {
                return cataloguedAssetsBatchCount;
            }

            // If the folder doesn't exist anymore, the corresponding entry in the catalog and the thumbnails file are both deleted.
            // TODO: This should be tested in a new test method, in which the non existent folder is explicitly added to the catalog.
            Folder folder = _folderRepository.GetFolderByPath(directory);

            if (folder != null)
            {
                List<Asset> cataloguedAssets = _assetRepository.GetCataloguedAssets(folder);

                foreach (var asset in cataloguedAssets)
                {
                    if (cataloguedAssetsBatchCount >= batchSize)
                    {
                        break;
                    }

                    _assetRepository.DeleteAsset(folder, asset.FileName);
                    cataloguedAssetsBatchCount++;

                    callback?.Invoke(new CatalogChangeCallbackEventArgs
                    {
                        Asset = asset,
                        Message = $"Image {Path.Combine(directory, asset.FileName)} deleted from catalog",
                        Reason = ReasonEnum.AssetDeleted
                    });
                }

                cataloguedAssets = _assetRepository.GetCataloguedAssets(folder);

                if (cataloguedAssets.Count == 0)
                {
                    _assetRepository.DeleteThumbnails(folder);
                    _folderRepository.DeleteFolder(folder);

                    callback?.Invoke(new CatalogChangeCallbackEventArgs
                    {
                        Folder = folder,
                        Message = "Folder " + directory + " deleted from catalog",
                        Reason = ReasonEnum.FolderDeleted
                    });
                }
            }

            return cataloguedAssetsBatchCount;
        }

        private int CatalogNewAssets(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount, int batchSize, string[] fileNames, List<Asset> cataloguedAssets)
        {
            string[] newFileNames = _directoryComparer.GetNewFileNames(fileNames, cataloguedAssets);

            foreach (var fileName in newFileNames)
            {
                if (cataloguedAssetsBatchCount >= batchSize)
                {
                    break;
                }

                Asset newAsset = CreateAsset(directory, fileName);
                newAsset.ImageData = LoadThumbnail(directory, fileName, newAsset.ThumbnailPixelWidth, newAsset.ThumbnailPixelHeight);
                cataloguedAssets.Add(newAsset);

                callback?.Invoke(new CatalogChangeCallbackEventArgs
                {
                    Asset = newAsset,
                    CataloguedAssets = cataloguedAssets,
                    Message = $"Image {Path.Combine(directory, fileName)} added to catalog",
                    Reason = ReasonEnum.AssetCreated
                });

                cataloguedAssetsBatchCount++;
            }

            return cataloguedAssetsBatchCount;
        }

        private int CatalogUpdatedAssets(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount, int batchSize, string[] fileNames, List<Asset> cataloguedAssets)
        {
            string[] updatedFileNames = _directoryComparer.GetUpdatedFileNames(fileNames, cataloguedAssets);
            Folder folder = _folderRepository.GetFolderByPath(directory);

            foreach (var fileName in updatedFileNames)
            {
                if (cataloguedAssetsBatchCount >= batchSize)
                {
                    break;
                }

                _assetRepository.DeleteAsset(folder, fileName);
                string fullPath = Path.Combine(directory, fileName);

                if (_storageService.FileExists(fullPath))
                {
                    Asset updatedAsset = CreateAsset(directory, fileName);
                    updatedAsset.ImageData = LoadThumbnail(directory, fileName, updatedAsset.ThumbnailPixelWidth, updatedAsset.ThumbnailPixelHeight);
                    cataloguedAssets.Add(updatedAsset);

                    callback?.Invoke(new CatalogChangeCallbackEventArgs
                    {
                        Asset = updatedAsset,
                        CataloguedAssets = cataloguedAssets,
                        Message = $"Image {fullPath} updated in catalog",
                        Reason = ReasonEnum.AssetUpdated
                    });

                    cataloguedAssetsBatchCount++;
                }
            }

            return cataloguedAssetsBatchCount;
        }

        private int CatalogDeletedAssets(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount, int batchSize, string[] fileNames, Folder folder, List<Asset> cataloguedAssets)
        {
            string[] deletedFileNames = _directoryComparer.GetDeletedFileNames(fileNames, cataloguedAssets);

            foreach (var fileName in deletedFileNames)
            {
                if (cataloguedAssetsBatchCount >= batchSize)
                {
                    break;
                }

                Asset deletedAsset = new()
                {
                    FileName = fileName,
                    FolderId = folder.FolderId,
                    Folder = folder
                };

                _assetRepository.DeleteAsset(folder, fileName);

                callback?.Invoke(new CatalogChangeCallbackEventArgs
                {
                    Asset = deletedAsset,
                    Message = $"Image {Path.Combine(directory, fileName)} deleted from catalog",
                    Reason = ReasonEnum.AssetDeleted
                });

                cataloguedAssetsBatchCount++;
            }

            return cataloguedAssetsBatchCount;
        }

        private BitmapImage LoadThumbnail(string directoryName, string fileName, int width, int height)
        {
            BitmapImage thumbnailImage = null;
            var folder = _folderRepository.GetFolderByPath(directoryName);

            if (_assetRepository.ContainsThumbnail(folder, fileName))
            {
                thumbnailImage = _assetRepository.LoadThumbnail(folder, fileName, width, height);
            }

            return thumbnailImage;
        }

        public Asset CreateAsset(string directoryName, string fileName)
        {
            Asset asset = null;

            const double MAX_WIDTH = 200;
            const double MAX_HEIGHT = 150;

            Folder folder = _folderRepository.GetFolderByPath(directoryName);

            if (!_assetRepository.IsAssetCatalogued(folder, fileName))
            {
                string imagePath = Path.Combine(directoryName, fileName);
                byte[] imageBytes = _storageService.GetFileBytes(imagePath);
                ushort? exifOrientation = _storageService.GetExifOrientation(imageBytes);
                Rotation rotation = exifOrientation.HasValue ? _storageService.GetImageRotation(exifOrientation.Value) : Rotation.Rotate0;
                BitmapImage originalImage = _storageService.LoadBitmapImage(imageBytes, rotation);

                double originalDecodeWidth = originalImage.PixelWidth;
                double originalDecodeHeight = originalImage.PixelHeight;
                double thumbnailDecodeWidth;
                double thumbnailDecodeHeight;
                double percentage;

                // If the original image is landscape
                if (originalDecodeWidth > originalDecodeHeight)
                {
                    thumbnailDecodeWidth = MAX_WIDTH;
                    percentage = MAX_WIDTH * 100d / originalDecodeWidth;
                    thumbnailDecodeHeight = percentage * originalDecodeHeight / 100d;
                }
                else // If the original image is portrait
                {
                    thumbnailDecodeHeight = MAX_HEIGHT;
                    percentage = MAX_HEIGHT * 100d / originalDecodeHeight;
                    thumbnailDecodeWidth = percentage * originalDecodeWidth / 100d;
                }

                BitmapImage thumbnailImage = _storageService.LoadBitmapImage(imageBytes,
                    rotation,
                    Convert.ToInt32(thumbnailDecodeWidth),
                    Convert.ToInt32(thumbnailDecodeHeight));
                bool isPng = imagePath.EndsWith(".png", StringComparison.OrdinalIgnoreCase);
                byte[] thumbnailBuffer = isPng ? _storageService.GetPngBitmapImage(thumbnailImage) : _storageService.GetJpegBitmapImage(thumbnailImage);

                if (folder == null)
                {
                    folder = _folderRepository.AddFolder(directoryName);
                }

                asset = new Asset
                {
                    FileName = Path.GetFileName(imagePath),
                    FolderId = folder.FolderId,
                    Folder = folder,
                    FileSize = new FileInfo(imagePath).Length,
                    PixelWidth = Convert.ToInt32(originalDecodeWidth),
                    PixelHeight = Convert.ToInt32(originalDecodeHeight),
                    ThumbnailPixelWidth = Convert.ToInt32(thumbnailDecodeWidth),
                    ThumbnailPixelHeight = Convert.ToInt32(thumbnailDecodeHeight),
                    ImageRotation = rotation,
                    ThumbnailCreationDateTime = DateTime.Now,
                    Hash = _assetHashCalculatorService.CalculateHash(imageBytes)
                };

                _assetRepository.AddAsset(asset, folder, thumbnailBuffer);
            }

            return asset;
        }
    }
}
