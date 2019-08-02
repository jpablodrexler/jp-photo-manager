using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Windows.Media.Imaging;

namespace AssetManager.Domain
{
    public class CatalogAssetsService : ICatalogAssetsService
    {
        private readonly IAssetRepository assetRepository;
        private readonly IAssetHashCalculatorService assetHashCalculatorService;
        private readonly IStorageService storageService;
        private readonly IUserConfigurationService userConfigurationService;

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

            callback?.Invoke(new CatalogChangeCallbackEventArgs() { Message = string.Empty });
        }

        private void CatalogImages(string directory, CatalogChangeCallback callback)
        {
            try
            {
                if (!this.assetRepository.FolderExists(directory))
                {
                    this.assetRepository.AddFolder(directory);
                }

                callback?.Invoke(new CatalogChangeCallbackEventArgs() { Message = "Inspecting folder " + directory });
                string[] fileNames = this.storageService.GetFileNames(directory);
                List<Asset> cataloguedAssets = null;

                Folder folder = this.assetRepository.GetFolderByPath(directory);
                var thumbnails = this.assetRepository.GetThumbnails(folder.ThumbnailsFilename, out bool isNewThumbnailsFile);
                cataloguedAssets = this.assetRepository.GetCataloguedAssets(directory);

                if (isNewThumbnailsFile)
                {
                    foreach (var asset in cataloguedAssets)
                    {
                        asset.ImageData = LoadThumbnail(thumbnails, directory, asset.FileName);
                    }
                }

                string[] newFileNames = GetNewFileNames(fileNames, cataloguedAssets);
                string[] deletedFileNames = GetDeletedFileNames(fileNames, cataloguedAssets);

                foreach (var fileName in newFileNames)
                {
                    Asset newAsset = new Asset()
                    {
                        FileName = fileName,
                        FolderId = folder.FolderId,
                        Folder = folder,
                        ImageData = LoadThumbnail(thumbnails, directory, fileName)
                    };

                    if (isNewThumbnailsFile)
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
                }

                foreach (var fileName in deletedFileNames)
                {
                    Asset deletedAsset = new Asset()
                    {
                        FileName = fileName,
                        FolderId = folder.FolderId,
                        Folder = folder
                    };

                    this.assetRepository.DeleteAsset(directory, fileName, deleteFile: false);

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
                }

                if (this.assetRepository.HasChanges() || isNewThumbnailsFile)
                {
                    this.assetRepository.SaveCatalog(thumbnails, folder.ThumbnailsFilename);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
            }

            var subdirectories = new DirectoryInfo(directory).EnumerateDirectories();

            foreach (var subdir in subdirectories)
            {
                this.CatalogImages(subdir.FullName, callback);
            }
        }

        private BitmapImage LoadThumbnail(Dictionary<string, byte[]> thumbnails, string directoryName, string fileName)
        {
            BitmapImage thumbnailImage = null;

            if (!this.assetRepository.IsAssetCatalogued(directoryName, fileName))
            {
                this.CreateThumbnail(thumbnails, Path.Combine(directoryName, fileName));

                if (thumbnails.ContainsKey(fileName))
                {
                    thumbnailImage = this.storageService.LoadBitmapImage(thumbnails[fileName]);
                }
            }

            return thumbnailImage;
        }

        public Asset CreateThumbnail(Dictionary<string, byte[]> thumbnails, string imagePath)
        {
            Asset asset;
            var directoryName = Path.GetDirectoryName(imagePath);

            const double MAX_WIDTH = 200;
            const double MAX_HEIGHT = 150;

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
            thumbnails[Path.GetFileName(imagePath)] = thumbnailBuffer;
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

            this.assetRepository.AddAsset(asset);

            return asset;
        }

        private string[] GetNewFileNames(string[] fileNames, List<Asset> cataloguedAssets)
        {
            return fileNames.Except(cataloguedAssets.Select(ca => ca.FileName))
                            .Where(f => f.EndsWith(".jpg") || f.EndsWith(".png") || f.EndsWith(".gif"))
                            .ToArray();
        }

        private string[] GetDeletedFileNames(string[] fileNames, List<Asset> cataloguedAssets)
        {
            return cataloguedAssets.Select(ca => ca.FileName).Except(fileNames).ToArray();
        }
    }
}
