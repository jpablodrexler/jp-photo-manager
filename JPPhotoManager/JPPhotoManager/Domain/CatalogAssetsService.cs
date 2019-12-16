using log4net;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
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

        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        public CatalogAssetsService(
            IAssetRepository assetRepository,
            IAssetHashCalculatorService assetHashCalculatorService,
            IStorageService storageService,
            IUserConfigurationService userConfigurationService)
        {
            this.assetRepository = assetRepository;
            this.assetHashCalculatorService = assetHashCalculatorService;
            this.storageService = storageService;
            this.userConfigurationService = userConfigurationService;
        }

        public void CatalogImages(CatalogChangeCallback callback)
        {
            try
            {
                string myPicturesDirectoryPath = this.userConfigurationService.GetPicturesDirectory();
                this.CatalogImages(myPicturesDirectoryPath, callback);

                Folder[] folders = this.assetRepository.GetFolders();

                foreach (var f in folders)
                {
                    string parentDirectory = this.storageService.GetParentDirectory(f.Path);

                    if (f.Path != myPicturesDirectoryPath && parentDirectory != myPicturesDirectoryPath)
                    {
                        this.CatalogImages(f.Path, callback);
                    }
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }

            callback?.Invoke(new CatalogChangeCallbackEventArgs() { Message = string.Empty });
        }

        private void CatalogImages(string directory, CatalogChangeCallback callback)
        {
            try
            {
                if (storageService.FolderExists(directory))
                {
                    if (!this.assetRepository.FolderExists(directory))
                    {
                        this.assetRepository.AddFolder(directory);
                    }

                    callback?.Invoke(new CatalogChangeCallbackEventArgs() { Message = "Inspecting folder " + directory });
                    string[] fileNames = this.storageService.GetFileNames(directory);
                    List<Asset> cataloguedAssets = null;

                    Folder folder = this.assetRepository.GetFolderByPath(directory);
                    cataloguedAssets = this.assetRepository.GetCataloguedAssets(directory);
                    bool folderHasThumbnails = this.assetRepository.FolderHasThumbnails(folder);

                    if (!folderHasThumbnails)
                    {
                        foreach (var asset in cataloguedAssets)
                        {
                            asset.ImageData = LoadThumbnail(directory, asset.FileName);
                        }
                    }

                    string[] newFileNames = GetNewFileNames(fileNames, cataloguedAssets);
                    string[] deletedFileNames = GetDeletedFileNames(fileNames, cataloguedAssets);
                    int batchSize = this.userConfigurationService.GetCatalogBatchSize();
                    int batchCount = 0;

                    foreach (var fileName in newFileNames)
                    {
                        Asset newAsset = new Asset()
                        {
                            FileName = fileName,
                            FolderId = folder.FolderId,
                            Folder = folder,
                            ImageData = LoadThumbnail(directory, fileName)
                        };

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

                        batchCount++;

                        if (batchCount >= batchSize)
                        {
                            this.assetRepository.SaveCatalog(folder);
                            batchCount = 0;
                        }
                    }

                    foreach (var fileName in deletedFileNames)
                    {
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

                        batchCount++;

                        if (batchCount >= batchSize)
                        {
                            this.assetRepository.SaveCatalog(folder);
                            batchCount = 0;
                        }
                    }

                    if (this.assetRepository.HasChanges() || !folderHasThumbnails)
                    {
                        this.assetRepository.SaveCatalog(folder);
                    }

                    var subdirectories = new DirectoryInfo(directory).EnumerateDirectories();

                    foreach (var subdir in subdirectories)
                    {
                        this.CatalogImages(subdir.FullName, callback);
                    }
                }
                else
                {
                    // TODO: Should validate that if the folder doesn't exist anymore, the corresponding entry in the catalog and the thumbnails file are both deleted.
                    // This should be tested in a new test method, in which the non existent folder is explicitly added to the catalog.
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
                callback?.Invoke(new CatalogChangeCallbackEventArgs { Exception = ex });
            }
        }

        private BitmapImage LoadThumbnail(string directoryName, string fileName)
        {
            BitmapImage thumbnailImage = null;

            this.CreateAsset(directoryName, fileName);

            if (this.assetRepository.ContainsThumbnail(directoryName, fileName))
            {
                thumbnailImage = this.assetRepository.LoadThumbnail(directoryName, fileName);
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
                BitmapImage originalImage = this.storageService.LoadBitmapImage(imagePath);

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

                byte[] imageBytes = this.storageService.GetFileBytes(imagePath);
                BitmapImage thumbnailImage = this.storageService.LoadBitmapImage(imageBytes,
                    Convert.ToInt32(thumbnailDecodeWidth),
                    Convert.ToInt32(thumbnailDecodeHeight));
                byte[] thumbnailBuffer = this.storageService.GetJpegBitmapImage(thumbnailImage);
                Folder folder = this.assetRepository.GetFolderByPath(directoryName);

                asset = new Asset
                {
                    FileName = Path.GetFileName(imagePath),
                    FolderId = folder.FolderId,
                    Folder = folder,
                    FileSize = new FileInfo(imagePath).Length,
                    PixelWidth = Convert.ToInt32(originalDecodeWidth),
                    PixelHeight = Convert.ToInt32(originalDecodeHeight),
                    ThumbnailCreationDateTime = DateTime.Now,
                    Hash = this.assetHashCalculatorService.CalculateHash(imageBytes)
                };

                this.assetRepository.AddAsset(asset, thumbnailBuffer);
            }

            return asset;
        }

        private string[] GetNewFileNames(string[] fileNames, List<Asset> cataloguedAssets)
        {
            return fileNames.Except(cataloguedAssets.Select(ca => ca.FileName))
                            .Where(f => f.EndsWith(".jpg", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".jpeg", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".png", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".gif", StringComparison.InvariantCultureIgnoreCase))
                            .ToArray();
        }

        private string[] GetNewFileNames(string[] sourceFileNames, string[] destinationFileNames)
        {
            return sourceFileNames.Except(destinationFileNames)
                            .Where(f => f.EndsWith(".jpg", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".jpeg", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".png", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".gif", StringComparison.InvariantCultureIgnoreCase))
                            .ToArray();
        }

        private string[] GetDeletedFileNames(string[] fileNames, List<Asset> cataloguedAssets)
        {
            return cataloguedAssets.Select(ca => ca.FileName).Except(fileNames).ToArray();
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

            if (!this.storageService.ImageExists(sourcePath))
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

            if (this.storageService.ImageExists(sourcePath) && !this.storageService.ImageExists(destinationPath))
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

            if (deleteFile && !this.storageService.ImageExists(asset, asset.Folder))
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

        public void ImportNewImages(string sourceDirectory, string destinationDirectory)
        {
            string[] sourceFileNames = this.storageService.GetFileNames(sourceDirectory);
            string[] destinationFileNames = this.storageService.GetFileNames(destinationDirectory);
            string[] newFileNames = GetNewFileNames(sourceFileNames, destinationFileNames);

            foreach (string newImage in newFileNames)
            {
                this.storageService.CopyImage(
                    Path.Combine(sourceDirectory, newImage),
                    Path.Combine(destinationDirectory, newImage));
            }
        }
    }
}
