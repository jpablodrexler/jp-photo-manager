using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Moq;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Xunit;

namespace JPPhotoManager.Test
{
    public class CatalogAssetsServiceTest
    {
        private string dataDirectory;
        private string assetDataSetPath;
        private string imageDestinationDirectory;
        private string nonCataloguedImageDestinationDirectory;
        private IConfigurationRoot configuration;

        public CatalogAssetsServiceTest()
        {
            dataDirectory = Path.GetDirectoryName(typeof(CatalogAssetsServiceTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");
            assetDataSetPath = Path.Combine(dataDirectory, $"AssetCatalog{Guid.NewGuid()}.json");
            imageDestinationDirectory = Path.Combine(dataDirectory, "NewFolder");
            nonCataloguedImageDestinationDirectory = Path.Combine(dataDirectory, "NonCataloguedNewFolder");

            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", dataDirectory)
                .MockGetValue("appsettings:CatalogBatchSize", "100");

            configuration = configurationMock.Object;

            if (File.Exists(assetDataSetPath))
            {
                File.Delete(assetDataSetPath);
            }

            if (Directory.Exists(imageDestinationDirectory))
            {
                Directory.Delete(imageDestinationDirectory, true);
            }

            if (Directory.Exists(nonCataloguedImageDestinationDirectory))
            {
                Directory.Delete(nonCataloguedImageDestinationDirectory, true);
            }
        }

        [Fact]
        public void CatalogFolderTest()
        {
            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(dataDirectory);

            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService.Object));
            repository.Initialize(this.assetDataSetPath);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService.Object),
                    userConfigurationService.Object);

            string[] fileList = Directory.GetFiles(dataDirectory, "*.jp*g") // jpg and jpeg files
                .Select(f => Path.GetFileName(f))
                .ToArray();

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();
            
            catalogAssetsService.CatalogImages(e => statusChanges.Add(e));

            var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
            var exceptions = statusChanges.Where(s => s.Exception != null).Select(s => s.Exception).ToList();

            var repositoryAssets = repository.GetAssets(dataDirectory);
            Assert.Equal(fileList.Length, processedAssets.Count);
            Assert.Equal(fileList.Length, repositoryAssets.Length);
            Assert.Equal(0, exceptions.Count);

            bool allProcessedAssetsInFileList = processedAssets.All(a => fileList.Contains(a.FileName));
            bool allProcessedAssetsInRepository = processedAssets.All(a => repositoryAssets.Contains(a));
            bool allRepositoryAssetsInProcessed = repositoryAssets.All(a => processedAssets.Contains(a));

            Assert.True(allProcessedAssetsInFileList);
            Assert.True(allProcessedAssetsInRepository);
            Assert.True(allRepositoryAssetsInProcessed);
        }

        [Fact]
        public void CatalogNonExistentFolderTest()
        {
            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(Path.Combine(dataDirectory, "NonExistent"));

            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService.Object));
            repository.Initialize(this.assetDataSetPath);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService.Object),
                    userConfigurationService.Object);

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();

            catalogAssetsService.CatalogImages(e => statusChanges.Add(e));

            var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
            var exceptions = statusChanges.Where(s => s.Exception != null).Select(s => s.Exception).ToList();

            Assert.Empty(processedAssets);
            Assert.Empty(exceptions);
        }

        [Fact]
        public void CreateAssetTest1()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            Assert.Equal("Image 2.jpg", asset.FileName);
            Assert.Equal(30197, asset.FileSize);
            Assert.Equal(dataDirectory, asset.Folder.Path);
            Assert.Equal(imagePath, asset.FullPath);
            Assert.Equal(720, asset.PixelHeight);
            Assert.Equal(1280, asset.PixelWidth);
            Assert.Equal("0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078", asset.Hash);
            Assert.NotEqual(DateTime.MinValue, asset.ThumbnailCreationDateTime);
        }

        [Fact]
        public void CreateAssetTest2()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            Assert.Equal("Image 1.jpg", asset.FileName);
            Assert.Equal(29857, asset.FileSize);
            Assert.Equal(dataDirectory, asset.Folder.Path);
            Assert.Equal(imagePath, asset.FullPath);
            Assert.Equal(720, asset.PixelHeight);
            Assert.Equal(1280, asset.PixelWidth);
            Assert.Equal("1fafae17c3c5c38d1205449eebdb9f5976814a5e54ec5797270c8ec467fe6d6d1190255cbaac11d9057c4b2697d90bc7116a46ed90c5ffb71e32e569c3b47fb9", asset.Hash);
            Assert.NotEqual(DateTime.MinValue, asset.ThumbnailCreationDateTime);
        }

        [Fact]
        public void CreateAssetOfDuplicatedFilesCompareHashesTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            Assert.True(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

            Assert.NotEqual(asset.FileName, duplicatedAsset.FileName);
            Assert.Equal(asset.Hash, duplicatedAsset.Hash);
        }

        [Fact]
        public void CreateAssetOfDifferentFilesCompareHashesTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            Assert.NotEqual(asset.FileName, duplicatedAsset.FileName);
            Assert.NotEqual(asset.Hash, duplicatedAsset.Hash);
        }

        [Fact]
        public void MoveExistingAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 4.jpg");
            string destinationImagePath = Path.Combine(imageDestinationDirectory, "Image 4.jpg");
            Assert.True(File.Exists(sourceImagePath));
            Assert.False(File.Exists(destinationImagePath));

            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 4.jpg");
            repository.SaveCatalog(sourceFolder);
            repository.SaveCatalog(destinationFolder);

            Assert.True(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));
            Assert.False(repository.ContainsThumbnail(destinationFolder.Path, asset.FileName));
            
            bool result = catalogAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: false);

            Assert.True(result);
            Assert.False(File.Exists(sourceImagePath));
            Assert.True(File.Exists(destinationImagePath));

            Assert.False(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));
            Assert.True(repository.ContainsThumbnail(destinationFolder.Path, asset.FileName));

            // Validates if the catalogued assets for the source folder are updated properly.
            var assets = repository.GetCataloguedAssets(sourceFolder.Path);
            int count = assets.Count(a => a.FileName == "Image 4.jpg");
            Assert.Equal(0, count);

            // Validates if the catalogued assets for the destination folder are updated properly.
            assets = repository.GetCataloguedAssets(destinationFolder.Path);
            count = assets.Count(a => a.FileName == "Image 4.jpg");
            Assert.Equal(1, count);
        }

        [Fact]
        public void MoveNonExistingAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Nonexistent Image.jpg");
            string destinationImagePath = Path.Combine(imageDestinationDirectory, "Nonexistent Image.jpg");
            Assert.False(File.Exists(sourceImagePath));
            Assert.False(File.Exists(destinationImagePath));

            Asset asset = new Asset
            {
                FileName = "Nonexistent Image.jpg",
                Folder = sourceFolder,
                FolderId = sourceFolder.FolderId
            };

            Assert.Equal(sourceFolder, asset.Folder);
            Assert.NotEqual(destinationFolder, asset.Folder);

            Assert.Throws<ArgumentException>(() =>
                catalogAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: false));
        }

        [Fact]
        public void MoveAssetToSamePathTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 5.jpg");
            Assert.True(File.Exists(sourceImagePath));

            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 5.jpg");
            repository.SaveCatalog(sourceFolder);

            Assert.True(repository.ContainsThumbnail(asset.Folder.Path, asset.FileName));

            bool result = catalogAssetsService.MoveAsset(asset, sourceFolder, preserveOriginalFile: false);

            Assert.False(result);
            Assert.True(File.Exists(sourceImagePath));

            Assert.True(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));
        }

        [Fact]
        public void MoveAssetToNonCataloguedFolderTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.GetFolderByPath(nonCataloguedImageDestinationDirectory);

            Assert.Null(destinationFolder);

            destinationFolder = new Folder { Path = nonCataloguedImageDestinationDirectory };
            
            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 7.jpg");
            Assert.True(File.Exists(sourceImagePath));

            string destinationImagePath = Path.Combine(nonCataloguedImageDestinationDirectory, "Image 7.jpg");
            Assert.False(File.Exists(destinationImagePath));

            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 7.jpg");
            repository.SaveCatalog(sourceFolder);

            Assert.True(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));

            bool result = catalogAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: false);

            Assert.True(result);
            Assert.False(File.Exists(sourceImagePath));
            Assert.True(File.Exists(destinationImagePath));
            Assert.False(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));
        }

        [Fact]
        public void MoveNullAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            Assert.Throws<ArgumentNullException>(() =>
                catalogAssetsService.MoveAsset(null, destinationFolder, preserveOriginalFile: false));
        }

        [Fact]
        public void MoveNullSourceFolderTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            Assert.Throws<ArgumentNullException>(() =>
                catalogAssetsService.MoveAsset(new Asset { Folder = null }, destinationFolder, preserveOriginalFile: false));
        }

        [Fact]
        public void MoveNullDestinationFolderTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            Assert.Throws<ArgumentNullException>(() =>
                catalogAssetsService.MoveAsset(new Asset { Folder = new Folder { } }, null, preserveOriginalFile: false));
        }

        [Fact]
        public void CopyExistingAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 5.jpg");
            string destinationImagePath = Path.Combine(imageDestinationDirectory, "Image 5.jpg");
            Assert.True(File.Exists(sourceImagePath));
            Assert.False(File.Exists(destinationImagePath));

            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 5.jpg");
            repository.SaveCatalog(sourceFolder);
            repository.SaveCatalog(destinationFolder);

            Assert.True(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));
            Assert.False(repository.ContainsThumbnail(destinationFolder.Path, asset.FileName));

            bool result = catalogAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: true);

            Assert.True(result);
            Assert.True(File.Exists(sourceImagePath));
            Assert.True(File.Exists(destinationImagePath));

            Assert.True(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));
            Assert.True(repository.ContainsThumbnail(destinationFolder.Path, asset.FileName));

            // Validates if the catalogued assets for the source folder are updated properly.
            var assets = repository.GetCataloguedAssets(sourceFolder.Path);
            int count = assets.Count(a => a.FileName == "Image 5.jpg");
            Assert.Equal(1, count);

            // Validates if the catalogued assets for the destination folder are updated properly.
            assets = repository.GetCataloguedAssets(destinationFolder.Path);
            count = assets.Count(a => a.FileName == "Image 5.jpg");
            Assert.Equal(1, count);
        }

        [Fact]
        public void CopyNonExistingAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Nonexistent Image.jpg");
            string destinationImagePath = Path.Combine(imageDestinationDirectory, "Nonexistent Image.jpg");
            Assert.False(File.Exists(sourceImagePath));
            Assert.False(File.Exists(destinationImagePath));

            Asset asset = new Asset
            {
                FileName = "Nonexistent Image.jpg",
                Folder = sourceFolder,
                FolderId = sourceFolder.FolderId
            };

            Assert.Equal(sourceFolder, asset.Folder);
            Assert.NotEqual(destinationFolder, asset.Folder);

            Assert.Throws<ArgumentException>(() =>
                catalogAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: true));
        }

        [Fact]
        public void CopyAssetToSamePathTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            
            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 5.jpg");
            Assert.True(File.Exists(sourceImagePath));
            
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 5.jpg");
            repository.SaveCatalog(sourceFolder);
            
            Assert.True(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));
            
            bool result = catalogAssetsService.MoveAsset(asset, sourceFolder, preserveOriginalFile: true);

            Assert.False(result);
            Assert.True(File.Exists(sourceImagePath));
            Assert.True(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));
        }

        [Fact]
        public void DeleteExistingImageTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 6.jpg");
            Assert.True(File.Exists(sourceImagePath));

            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 6.jpg");
            repository.SaveCatalog(sourceFolder);

            Assert.True(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));

            catalogAssetsService.DeleteAsset(asset, deleteFile: true);

            Assert.False(File.Exists(sourceImagePath));
            Assert.False(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));
        }

        [Fact]
        public void DeleteNonExistingImageTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Nonexistent Image.jpg");
            Assert.False(File.Exists(sourceImagePath));

            Asset asset = new Asset
            {
                FileName = "Nonexistent Image.jpg",
                Folder = sourceFolder,
                FolderId = sourceFolder.FolderId
            };

            Assert.Throws<ArgumentException>(() =>
                catalogAssetsService.DeleteAsset(asset, deleteFile: true));
        }

        [Fact]
        public void DeleteNullAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            Folder sourceFolder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            Assert.Throws<ArgumentNullException>(() =>
                catalogAssetsService.DeleteAsset(null, deleteFile: true));
        }

        [Fact]
        public void DeleteNullFolderTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            Assert.Throws<ArgumentNullException>(() =>
                catalogAssetsService.DeleteAsset(new Asset { Folder = null }, deleteFile: true));
        }
    }
}
