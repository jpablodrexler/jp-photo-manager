using log4net;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Threading;
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

        public void CatalogImages(CatalogChangeCallback callback)
        {
            int cataloguedAssetsBatchCount = 0;

            try
            {
                // TODO: Allow the user to configure additional root folders.
	            // TODO: Validate if some of the root folders are not valid or don't exist any longer.
	            string[] rootFolders = new string[]
	            {
	                this.userConfigurationService.GetOneDriveDirectory(),
	                this.userConfigurationService.GetPicturesDirectory()
	            };
	
	            foreach (string path in rootFolders)
	            {
                    cataloguedAssetsBatchCount = this.CatalogImages(path, callback, cataloguedAssetsBatchCount);
	            }

                Folder[] folders = this.assetRepository.GetFolders();

                callback?.Invoke(new CatalogChangeCallbackEventArgs() { Message = string.Empty });
                
                foreach (var f in folders)
                {
                    string parentDirectory = this.storageService.GetParentDirectory(f.Path);

                    // TODO: This condition is meant to avoid cataloging the same folder twice. However, it only works with only one level of subfolders. Must be improved to support a complex directory tree.
                    if (!rootFolders.Any(p => string.Compare(p, f.Path, StringComparison.OrdinalIgnoreCase) == 0) && !rootFolders.Any(p => string.Compare(p, parentDirectory, StringComparison.OrdinalIgnoreCase) == 0))
                    {
                        cataloguedAssetsBatchCount = this.CatalogImages(f.Path, callback, cataloguedAssetsBatchCount);
                    }
                }
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

        private int CatalogImages(string directory, CatalogChangeCallback callback, int cataloguedAssetsBatchCount)
        {
            this.currentFolderPath = directory;

            if (storageService.FolderExists(directory))
            {
                if (!this.assetRepository.FolderExists(directory))
                {
                    this.assetRepository.AddFolder(directory);
                }

                callback?.Invoke(new CatalogChangeCallbackEventArgs() { Message = "Inspecting folder " + directory });
                string[] fileNames = this.storageService.GetFileNames(directory);
                List<Asset> cataloguedAssets;

                Folder folder = this.assetRepository.GetFolderByPath(directory);
                cataloguedAssets = this.assetRepository.GetCataloguedAssets(directory);
                bool folderHasThumbnails = this.assetRepository.FolderHasThumbnails(folder);

                if (!folderHasThumbnails)
                {
                    foreach (var asset in cataloguedAssets)
                    {
                        asset.ImageData = LoadThumbnail(directory, asset.FileName, asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight);
                    }
                }

                string[] newFileNames = directoryComparer.GetNewFileNames(fileNames, cataloguedAssets);
                string[] deletedFileNames = directoryComparer.GetDeletedFileNames(fileNames, cataloguedAssets);
                int batchSize = this.userConfigurationService.GetCatalogBatchSize();
                
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
                        Message = "Creating thumbnail for " + Path.Combine(directory, fileName),
                        Reason = ReasonEnum.Created
                    });

                    cataloguedAssetsBatchCount++;
                }

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
                        Asset = new Asset()
                        {
                            FileName = fileName,
                            FolderId = folder.FolderId,
                            Folder = folder
                        },
                        Reason = ReasonEnum.Deleted
                    });

                    cataloguedAssetsBatchCount++;
                }

                if (this.assetRepository.HasChanges() || !folderHasThumbnails)
                {
                    this.assetRepository.SaveCatalog(folder);
                }

                if (cataloguedAssetsBatchCount < batchSize)
                {
                    var subdirectories = new DirectoryInfo(directory).EnumerateDirectories();

                    foreach (var subdir in subdirectories)
                    {
                        cataloguedAssetsBatchCount = this.CatalogImages(subdir.FullName, callback, cataloguedAssetsBatchCount);
                    }
                }
            }
            else
            {
                // TODO: Should validate that if the folder doesn't exist anymore, the corresponding entry in the catalog and the thumbnails file are both deleted.
                // This should be tested in a new test method, in which the non existent folder is explicitly added to the catalog.
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

        public bool MoveAsset(Asset asset, Folder destinationFolder, bool preserveOriginalFile)
        {
            #region Parameters validation

            if (asset == null)
            {
                throw new ArgumentNullException(nameof(asset), "asset cannot be null.");
            }

            if (asset.Folder == null)
            {
                throw new ArgumentNullException(nameof(asset), "asset.Folder cannot be null.");
            }

            if (destinationFolder == null)
            {
                throw new ArgumentNullException(nameof(destinationFolder), "destinationFolder cannot be null.");
            }

            #endregion

            bool result = false;
            string sourcePath = asset.FullPath;
            string destinationPath = Path.Combine(destinationFolder.Path, asset.FileName);
            bool isDestinationFolderInCatalog;

            if (!this.storageService.FileExists(sourcePath))
            {
                throw new ArgumentException(sourcePath);
            }

            var folder = this.assetRepository.GetFolderByPath(destinationFolder.Path);

            // If the folder is null, it means is not present in the catalog.
            // TODO: IF THE DESTINATION FOLDER IS NEW, THE FOLDER NAVIGATION CONTROL SHOULD DISPLAY IT WHEN THE USER GOES BACK TO THE MAIN WINDOW.
            isDestinationFolderInCatalog = folder != null;

            if (isDestinationFolderInCatalog)
            {
                destinationFolder = folder;
            }

            if (this.storageService.FileExists(sourcePath) && !this.storageService.FileExists(destinationPath))
            {
                result = this.storageService.CopyImage(sourcePath, destinationPath);
                
                if (result)
                {
                    if (!preserveOriginalFile)
                    {
                        this.DeleteAsset(asset, deleteFile: true);
                    }

                    if (isDestinationFolderInCatalog)
                    {
                        this.CreateAsset(destinationFolder.Path, asset.FileName);
                        this.assetRepository.SaveCatalog(destinationFolder);
                    }
                }
            }

            return result;
        }

        public void DeleteAsset(Asset asset, bool deleteFile)
        {
            #region Parameters validation

            if (asset == null)
            {
                throw new ArgumentNullException(nameof(asset), "Asset cannot be null.");
            }

            if (asset.Folder == null)
            {
                throw new ArgumentNullException(nameof(asset), "Asset.Folder cannot be null.");
            }

            if (deleteFile && !this.storageService.FileExists(asset, asset.Folder))
            {
                throw new ArgumentException("File does not exist: " + asset.FullPath);
            }

            #endregion

            this.assetRepository.DeleteAsset(asset.Folder.Path, asset.FileName);

            if (deleteFile)
            {
                this.storageService.DeleteFile(asset.Folder.Path, asset.FileName);
            }

            this.assetRepository.SaveCatalog(asset.Folder);
        }
    }
}
