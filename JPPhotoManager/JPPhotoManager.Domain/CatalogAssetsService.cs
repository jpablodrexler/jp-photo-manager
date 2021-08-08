using log4net;
using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain
{
    public class CatalogAssetsService : ICatalogAssetsService
    {
        private readonly IAssetRepository assetRepository;
        private readonly IAssetHashCalculatorService assetHashCalculatorService;
        private readonly IStorageService storageService;
        private readonly IUserConfigurationService userConfigurationService;
        private readonly IDirectoryComparer directoryComparer;

        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        private string currentFolderPath;

        public CatalogAssetsService(
            IAssetRepository assetRepository,
            IAssetHashCalculatorService assetHashCalculatorService,
            IStorageService storageService,
            IUserConfigurationService userConfigurationService,
            IDirectoryComparer directoryComparer)
        {
            this.assetRepository = assetRepository;
            this.assetHashCalculatorService = assetHashCalculatorService;
            this.storageService = storageService;
            this.userConfigurationService = userConfigurationService;
            this.directoryComparer = directoryComparer;
        }

        public void CatalogAssets(CatalogChangeCallback callback)
        {
            int cataloguedAssetsBatchCount = 0;

            try
            {
                Folder[] foldersToCatalog = GetFoldersToCatalog();

                // TODO: Since the root folders to catalog are combined in the same list
                // with the catalogued sub-folders, the catalog process should keep a list
                // of the already visited folders so they don't get catalogued twice
                // in the same execution.

                foreach (Folder folder in foldersToCatalog)
                {
                    cataloguedAssetsBatchCount = this.CatalogAssets(folder.Path, callback, cataloguedAssetsBatchCount);
                }

                callback?.Invoke(new CatalogChangeCallbackEventArgs() { Message = string.Empty });
            }
            catch (OperationCanceledException)
            {
                // If the catalog background process is cancelled,
                // there is a risk that it happens while saving the catalog files.
                // This could result in the files being damaged.
                // Therefore the application saves the files before the task is completly shut down.
                Folder currentFolder = this.assetRepository.GetFolderByPath(currentFolderPath);
                this.assetRepository.SaveCatalog(currentFolder);
                throw;
            }
            catch (Exception ex)
            {
                log.Error(ex);
                callback?.Invoke(new CatalogChangeCallbackEventArgs { Exception = ex });
            }
            finally
            {
                callback?.Invoke(new CatalogChangeCallbackEventArgs { Message = string.Empty });
            }
        }

        private Folder[] GetFoldersToCatalog()
        {
            string[] rootPaths = this.userConfigurationService.GetRootCatalogFolderPaths();
            
            foreach (string root in rootPaths)
            {
                if (!this.assetRepository.FolderExists(root))
                {
                    this.assetRepository.AddFolder(root);
                }
            }

            return this.assetRepository.GetFolders();
        }

        private int CatalogAssets(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount)
        {
            this.currentFolderPath = directory;
            int batchSize = this.userConfigurationService.GetCatalogBatchSize();
            
            if (storageService.FolderExists(directory))
            {
                cataloguedAssetsBatchCount = CatalogExistingFolder(directory, callback, cataloguedAssetsBatchCount, batchSize);
            }
            else if (!string.IsNullOrEmpty(directory) && !storageService.FolderExists(directory))
            {
                cataloguedAssetsBatchCount = CatalogNonExistingFolder(directory, callback, cataloguedAssetsBatchCount, batchSize);
            }

            return cataloguedAssetsBatchCount;
        }

        private int CatalogExistingFolder(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount, int batchSize)
        {
            if (!this.assetRepository.FolderExists(directory))
            {
                this.assetRepository.AddFolder(directory);

                callback?.Invoke(new CatalogChangeCallbackEventArgs
                {
                    Message = $"Folder {directory} added to catalog",
                    Reason = ReasonEnum.Created
                });
            }

            callback?.Invoke(new CatalogChangeCallbackEventArgs() { Message = "Inspecting folder " + directory });
            string[] fileNames = this.storageService.GetFileNames(directory);
            Folder folder = this.assetRepository.GetFolderByPath(directory);
            List<Asset> cataloguedAssets = this.assetRepository.GetCataloguedAssets(directory);
            bool folderHasThumbnails = this.assetRepository.FolderHasThumbnails(folder);

            if (!folderHasThumbnails)
            {
                foreach (var asset in cataloguedAssets)
                {
                    asset.ImageData = LoadThumbnail(directory, asset.FileName, asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight);
                }
            }

            cataloguedAssetsBatchCount = CatalogNewAssets(directory, callback, cataloguedAssetsBatchCount, batchSize, fileNames, cataloguedAssets, folderHasThumbnails);
            cataloguedAssetsBatchCount = CatalogUpdatedAssets(directory, callback, cataloguedAssetsBatchCount, batchSize, fileNames, cataloguedAssets, folderHasThumbnails);
            cataloguedAssetsBatchCount = CatalogDeletedAssets(directory, callback, cataloguedAssetsBatchCount, batchSize, fileNames, folder, cataloguedAssets);

            if (this.assetRepository.HasChanges() || !folderHasThumbnails)
            {
                this.assetRepository.SaveCatalog(folder);
            }

            if (cataloguedAssetsBatchCount < batchSize)
            {
                var subdirectories = new DirectoryInfo(directory).EnumerateDirectories();

                foreach (var subdir in subdirectories)
                {
                    cataloguedAssetsBatchCount = this.CatalogAssets(subdir.FullName, callback, cataloguedAssetsBatchCount);
                }
            }

            return cataloguedAssetsBatchCount;
        }

        private int CatalogNonExistingFolder(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount, int batchSize)
        {
            // If the folder doesn't exist anymore, the corresponding entry in the catalog and the thumbnails file are both deleted.
            // TODO: This should be tested in a new test method, in which the non existent folder is explicitly added to the catalog.
            Folder folder = this.assetRepository.GetFolderByPath(directory);

            if (folder != null)
            {
                List<Asset> cataloguedAssets = this.assetRepository.GetCataloguedAssets(directory);

                foreach (var asset in cataloguedAssets)
                {
                    if (cataloguedAssetsBatchCount >= batchSize)
                    {
                        break;
                    }

                    this.assetRepository.DeleteAsset(directory, asset.FileName);
                    cataloguedAssetsBatchCount++;

                    callback?.Invoke(new CatalogChangeCallbackEventArgs
                    {
                        Asset = asset,
                        Message = $"Image {Path.Combine(directory, asset.FileName)} deleted from catalog",
                        Reason = ReasonEnum.Deleted
                    });
                }

                cataloguedAssets = this.assetRepository.GetCataloguedAssets(directory);

                if (cataloguedAssets.Count == 0)
                {
                    this.assetRepository.DeleteFolder(folder);

                    callback?.Invoke(new CatalogChangeCallbackEventArgs
                    {
                        Message = "Folder " + directory + " deleted from catalog",
                        Reason = ReasonEnum.Deleted
                    });
                }

                if (this.assetRepository.HasChanges())
                {
                    this.assetRepository.SaveCatalog(folder);
                }
            }

            return cataloguedAssetsBatchCount;
        }

        private int CatalogNewAssets(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount, int batchSize, string[] fileNames, List<Asset> cataloguedAssets, bool folderHasThumbnails)
        {
            string[] newFileNames = directoryComparer.GetNewFileNames(fileNames, cataloguedAssets);

            foreach (var fileName in newFileNames)
            {
                if (cataloguedAssetsBatchCount >= batchSize)
                {
                    break;
                }

                Asset newAsset = CreateAsset(directory, fileName);
                newAsset.ImageData = LoadThumbnail(directory, fileName, newAsset.ThumbnailPixelWidth, newAsset.ThumbnailPixelHeight);

                if (!folderHasThumbnails)
                {
                    cataloguedAssets.Add(newAsset);
                }

                callback?.Invoke(new CatalogChangeCallbackEventArgs
                {
                    Asset = newAsset,
                    CataloguedAssets = cataloguedAssets,
                    Message = $"Image {Path.Combine(directory, fileName)} added to catalog",
                    Reason = ReasonEnum.Created
                });

                cataloguedAssetsBatchCount++;
            }

            return cataloguedAssetsBatchCount;
        }

        private int CatalogUpdatedAssets(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount, int batchSize, string[] fileNames, List<Asset> cataloguedAssets, bool folderHasThumbnails)
        {
            string[] updatedFileNames = directoryComparer.GetUpdatedFileNames(fileNames, cataloguedAssets);
            Folder folder = this.assetRepository.GetFolderByPath(directory);

            foreach (var fileName in updatedFileNames)
            {
                if (cataloguedAssetsBatchCount >= batchSize)
                {
                    break;
                }

                this.assetRepository.DeleteAsset(directory, fileName);

                Asset updatedAsset = CreateAsset(directory, fileName);
                updatedAsset.ImageData = LoadThumbnail(directory, fileName, updatedAsset.ThumbnailPixelWidth, updatedAsset.ThumbnailPixelHeight);

                if (!folderHasThumbnails)
                {
                    cataloguedAssets.Add(updatedAsset);
                }

                callback?.Invoke(new CatalogChangeCallbackEventArgs
                {
                    Asset = updatedAsset,
                    CataloguedAssets = cataloguedAssets,
                    Message = $"Image {Path.Combine(directory, fileName)} updated in catalog",
                    Reason = ReasonEnum.Updated
                });

                cataloguedAssetsBatchCount++;
            }

            return cataloguedAssetsBatchCount;
        }

        private int CatalogDeletedAssets(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount, int batchSize, string[] fileNames, Folder folder, List<Asset> cataloguedAssets)
        {
            string[] deletedFileNames = directoryComparer.GetDeletedFileNames(fileNames, cataloguedAssets);

            foreach (var fileName in deletedFileNames)
            {
                if (cataloguedAssetsBatchCount >= batchSize)
                {
                    break;
                }

                Asset deletedAsset = new Asset()
                {
                    FileName = fileName,
                    FolderId = folder.FolderId,
                    Folder = folder
                };

                this.assetRepository.DeleteAsset(directory, fileName);

                callback?.Invoke(new CatalogChangeCallbackEventArgs
                {
                    Asset = deletedAsset,
                    Message = $"Image {Path.Combine(directory, fileName)} deleted from catalog",
                    Reason = ReasonEnum.Deleted
                });

                cataloguedAssetsBatchCount++;
            }

            return cataloguedAssetsBatchCount;
        }

        private BitmapImage LoadThumbnail(string directoryName, string fileName, int width, int height)
        {
            BitmapImage thumbnailImage = null;

            if (this.assetRepository.ContainsThumbnail(directoryName, fileName))
            {
                thumbnailImage = this.assetRepository.LoadThumbnail(directoryName, fileName, width, height);
            }

            return thumbnailImage;
        }

        public Asset CreateAsset(string directoryName, string fileName)
        {
            Asset asset = null;
            
            const double MAX_WIDTH = 200;
            const double MAX_HEIGHT = 150;

            if (!this.assetRepository.IsAssetCatalogued(directoryName, fileName))
            {
                string imagePath = Path.Combine(directoryName, fileName);
                byte[] imageBytes = this.storageService.GetFileBytes(imagePath);
                ushort? exifOrientation = this.storageService.GetExifOrientation(imageBytes);
                Rotation rotation = exifOrientation.HasValue ? this.storageService.GetImageRotation(exifOrientation.Value) : Rotation.Rotate0;
                BitmapImage originalImage = this.storageService.LoadBitmapImage(imageBytes, rotation);

                double originalDecodeWidth = originalImage.PixelWidth;
                double originalDecodeHeight = originalImage.PixelHeight;
                double thumbnailDecodeWidth;
                double thumbnailDecodeHeight;
                double percentage;

                // If the original image is landscape
                if (originalDecodeWidth > originalDecodeHeight)
                {
                    thumbnailDecodeWidth = MAX_WIDTH;
                    percentage = (MAX_WIDTH * 100d / originalDecodeWidth);
                    thumbnailDecodeHeight = (percentage * originalDecodeHeight) / 100d;
                }
                else // If the original image is portrait
                {
                    thumbnailDecodeHeight = MAX_HEIGHT;
                    percentage = (MAX_HEIGHT * 100d / originalDecodeHeight);
                    thumbnailDecodeWidth = (percentage * originalDecodeWidth) / 100d;
                }

                BitmapImage thumbnailImage = this.storageService.LoadBitmapImage(imageBytes,
                    rotation,
                    Convert.ToInt32(thumbnailDecodeWidth),
                    Convert.ToInt32(thumbnailDecodeHeight));
                bool isPng = imagePath.EndsWith(".png", StringComparison.OrdinalIgnoreCase);
                byte[] thumbnailBuffer = isPng ? this.storageService.GetPngBitmapImage(thumbnailImage) : this.storageService.GetJpegBitmapImage(thumbnailImage);
                Folder folder = this.assetRepository.GetFolderByPath(directoryName);

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
                    Hash = this.assetHashCalculatorService.CalculateHash(imageBytes)
                };

                this.assetRepository.AddAsset(asset, thumbnailBuffer);
            }

            return asset;
        }
    }
}
