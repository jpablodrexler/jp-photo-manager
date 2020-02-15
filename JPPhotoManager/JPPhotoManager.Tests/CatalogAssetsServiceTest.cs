using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Moq;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class CatalogAssetsServiceTest
    {
        private string dataDirectory;
        private string assetsDataFilePath;
        private string foldersDataFilePath;
        private string importsDataFilePath;
        private string imageDestinationDirectory;
        private string nonCataloguedImageDestinationDirectory;
        private IConfigurationRoot configuration;

        public CatalogAssetsServiceTest()
        {
            dataDirectory = Path.GetDirectoryName(typeof(CatalogAssetsServiceTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");
            assetsDataFilePath = Path.Combine(dataDirectory, $"asset.{Guid.NewGuid()}.db");
            foldersDataFilePath = Path.Combine(dataDirectory, $"folder.{Guid.NewGuid()}.db");
            importsDataFilePath = Path.Combine(dataDirectory, $"import.{Guid.NewGuid()}.db");
            imageDestinationDirectory = Path.Combine(dataDirectory, "NewFolder");
            nonCataloguedImageDestinationDirectory = Path.Combine(dataDirectory, "NonCataloguedNewFolder");

            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", dataDirectory)
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:AssetsDataFilePath", assetsDataFilePath)
                .MockGetValue("appsettings:FoldersDataFilePath", foldersDataFilePath)
                .MockGetValue("appsettings:ImportsDataFilePath", importsDataFilePath);

            configuration = configurationMock.Object;

            if (File.Exists(assetsDataFilePath))
            {
                File.Delete(assetsDataFilePath);
            }

            if (File.Exists(foldersDataFilePath))
            {
                File.Delete(foldersDataFilePath);
            }

            if (File.Exists(importsDataFilePath))
            {
                File.Delete(importsDataFilePath);
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
            userConfigurationService.Setup(conf => conf.GetOneDriveDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetCatalogBatchSize()).Returns(1000);
            userConfigurationService.Setup(conf => conf.GetAssetsDataFilePath()).Returns(assetsDataFilePath);
            userConfigurationService.Setup(conf => conf.GetFoldersDataFilePath()).Returns(foldersDataFilePath);
            userConfigurationService.Setup(conf => conf.GetImportsDataFilePath()).Returns(importsDataFilePath);

            IStorageService storageService = new StorageService(userConfigurationService.Object);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService.Object);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService.Object,
                    new DirectoryComparer());

            var jpegFiles = Directory.GetFiles(dataDirectory, "*.jp*g") // jpg and jpeg files
                .Select(f => Path.GetFileName(f));

            var pngFiles = Directory.GetFiles(dataDirectory, "*.png") // png files
                .Select(f => Path.GetFileName(f));

            List<string> fileList = new List<string>();
            fileList.AddRange(jpegFiles);
            fileList.AddRange(pngFiles);

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();
            
            catalogAssetsService.CatalogImages(e => statusChanges.Add(e));

            var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
            var exceptions = statusChanges.Where(s => s.Exception != null).Select(s => s.Exception).ToList();

            var repositoryAssets = repository.GetAssets(dataDirectory);
            processedAssets.Should().HaveSameCount(fileList);
            repositoryAssets.Should().HaveSameCount(fileList);
            exceptions.Should().BeEmpty();

            processedAssets.Should().OnlyContain(a => fileList.Contains(a.FileName));
            processedAssets.Should().OnlyContain(a => repositoryAssets.Contains(a));
            repositoryAssets.Should().OnlyContain(a => processedAssets.Contains(a));
        }

        [Fact]
        public void CatalogFolderLargerThanBatchSizeTest()
        {
            int batchSize = 5;

            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetOneDriveDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetCatalogBatchSize()).Returns(batchSize);
            userConfigurationService.Setup(conf => conf.GetAssetsDataFilePath()).Returns(assetsDataFilePath);
            userConfigurationService.Setup(conf => conf.GetFoldersDataFilePath()).Returns(foldersDataFilePath);
            userConfigurationService.Setup(conf => conf.GetImportsDataFilePath()).Returns(importsDataFilePath);

            IStorageService storageService = new StorageService(userConfigurationService.Object);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService.Object);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService.Object,
                    new DirectoryComparer());

            var jpegFiles = Directory.GetFiles(dataDirectory, "*.jp*g") // jpg and jpeg files
                .Select(f => Path.GetFileName(f));

            var pngFiles = Directory.GetFiles(dataDirectory, "*.png") // png files
                .Select(f => Path.GetFileName(f));

            List<string> fileList = new List<string>();
            fileList.AddRange(jpegFiles);
            fileList.AddRange(pngFiles);

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();

            catalogAssetsService.CatalogImages(e => statusChanges.Add(e));

            var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
            var exceptions = statusChanges.Where(s => s.Exception != null).Select(s => s.Exception).ToList();

            var repositoryAssets = repository.GetAssets(dataDirectory);
            fileList.Should().HaveCountGreaterThan(batchSize);
            processedAssets.Should().HaveCount(batchSize);
            repositoryAssets.Should().HaveCount(batchSize);
            exceptions.Should().BeEmpty();

            processedAssets.Should().OnlyContain(a => fileList.Contains(a.FileName));
            processedAssets.Should().OnlyContain(a => repositoryAssets.Contains(a));
            repositoryAssets.Should().OnlyContain(a => processedAssets.Contains(a));
        }

        [Fact]
        public void CatalogFolderRemovesDeletedFileTest()
        {
            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetOneDriveDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetCatalogBatchSize()).Returns(1000);
            userConfigurationService.Setup(conf => conf.GetAssetsDataFilePath()).Returns(assetsDataFilePath);
            userConfigurationService.Setup(conf => conf.GetFoldersDataFilePath()).Returns(foldersDataFilePath);
            userConfigurationService.Setup(conf => conf.GetImportsDataFilePath()).Returns(importsDataFilePath);

            IStorageService storageService = new StorageService(userConfigurationService.Object);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService.Object);
            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    assetHashCalculatorService,
                    storageService,
                    userConfigurationService.Object,
                    directoryComparer);

            var jpegFiles = Directory.GetFiles(dataDirectory, "*.jp*g") // jpg and jpeg files
                .Select(f => Path.GetFileName(f));

            var pngFiles = Directory.GetFiles(dataDirectory, "*.png") // png files
                .Select(f => Path.GetFileName(f));

            List<string> fileList = new List<string>();
            fileList.AddRange(jpegFiles);
            fileList.AddRange(pngFiles);

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();

            catalogAssetsService.CatalogImages(e => statusChanges.Add(e));

            var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
            var repositoryAssets = repository.GetAssets(dataDirectory);
            string deletedFile = fileList[0];

            Mock<IDirectoryComparer> directoryComparerMock = new Mock<IDirectoryComparer>();
            directoryComparerMock.Setup(
                c => c.GetDeletedFileNames(It.IsAny<string[]>(), It.IsAny<List<Asset>>()))
                .Returns(new string[] { deletedFile });

            catalogAssetsService = new CatalogAssetsService(
                    repository,
                    assetHashCalculatorService,
                    storageService,
                    userConfigurationService.Object,
                    directoryComparerMock.Object);

            statusChanges.Clear();
            catalogAssetsService.CatalogImages(e => statusChanges.Add(e));
            var repositoryAssetsAfterDelete = repository.GetAssets(dataDirectory);

            repositoryAssets.Should().Contain(a => a.FileName == deletedFile);
            repositoryAssetsAfterDelete.Should().NotContain(a => a.FileName == deletedFile);
            statusChanges.Should().Contain(s => s.Asset != null && s.Asset.FileName == deletedFile);
        }

        [Fact]
        public void CatalogFolderRemovesDeletedFileLargerThanBatchSizeTest()
        {
            int batchSize = 1000;

            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetOneDriveDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetCatalogBatchSize()).Returns(batchSize);
            userConfigurationService.Setup(conf => conf.GetAssetsDataFilePath()).Returns(assetsDataFilePath);
            userConfigurationService.Setup(conf => conf.GetFoldersDataFilePath()).Returns(foldersDataFilePath);
            userConfigurationService.Setup(conf => conf.GetImportsDataFilePath()).Returns(importsDataFilePath);

            IStorageService storageService = new StorageService(userConfigurationService.Object);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService.Object);
            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    assetHashCalculatorService,
                    storageService,
                    userConfigurationService.Object,
                    directoryComparer);

            var jpegFiles = Directory.GetFiles(dataDirectory, "*.jp*g") // jpg and jpeg files
                .Select(f => Path.GetFileName(f));

            var pngFiles = Directory.GetFiles(dataDirectory, "*.png") // png files
                .Select(f => Path.GetFileName(f));

            List<string> fileList = new List<string>();
            fileList.AddRange(jpegFiles);
            fileList.AddRange(pngFiles);

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();

            catalogAssetsService.CatalogImages(e => statusChanges.Add(e));

            var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
            var repositoryAssets = repository.GetAssets(dataDirectory);
            
            Mock<IDirectoryComparer> directoryComparerMock = new Mock<IDirectoryComparer>();
            directoryComparerMock.Setup(
                c => c.GetDeletedFileNames(It.IsAny<string[]>(), It.IsAny<List<Asset>>()))
                .Returns(fileList.ToArray());

            batchSize = 5;
            userConfigurationService.Setup(conf => conf.GetCatalogBatchSize()).Returns(batchSize);

            catalogAssetsService = new CatalogAssetsService(
                    repository,
                    assetHashCalculatorService,
                    storageService,
                    userConfigurationService.Object,
                    directoryComparerMock.Object);

            statusChanges.Clear();
            catalogAssetsService.CatalogImages(e => statusChanges.Add(e));
            var repositoryAssetsAfterDelete = repository.GetAssets(dataDirectory);

            repositoryAssetsAfterDelete.Should().HaveCount(fileList.Count - batchSize);
        }

        [Fact]
        public void CatalogNonExistentFolderTest()
        {
            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(Path.Combine(dataDirectory, "NonExistent"));
            userConfigurationService.Setup(conf => conf.GetAssetsDataFilePath()).Returns(assetsDataFilePath);
            userConfigurationService.Setup(conf => conf.GetFoldersDataFilePath()).Returns(foldersDataFilePath);
            userConfigurationService.Setup(conf => conf.GetImportsDataFilePath()).Returns(importsDataFilePath);

            IStorageService storageService = new StorageService(userConfigurationService.Object);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService.Object);
            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    assetHashCalculatorService,
                    storageService,
                    userConfigurationService.Object,
                    directoryComparer);

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();

            catalogAssetsService.CatalogImages(e => statusChanges.Add(e));

            statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).Should().BeEmpty();
            statusChanges.Where(s => s.Exception != null).Select(s => s.Exception).Should().BeEmpty();
        }

        [Fact]
        public void CreateAssetTest1()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            asset.FileName.Should().Be("Image 2.jpg");
            asset.FileSize.Should().Be(30197);
            asset.Folder.Path.Should().Be(dataDirectory);
            asset.FullPath.Should().Be(imagePath);
            asset.PixelHeight.Should().Be(720);
            asset.PixelWidth.Should().Be(1280);
            asset.Hash.Should().Be("0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078");
            asset.ThumbnailCreationDateTime.Should().NotBe(DateTime.MinValue);
        }

        [Fact]
        public void CreateAssetTest2()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.GetFolderByPath(nonCataloguedImageDestinationDirectory);

            Assert.Null(destinationFolder);

            destinationFolder = new Folder { Path = nonCataloguedImageDestinationDirectory };
            
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            Assert.Throws<ArgumentNullException>(() =>
                catalogAssetsService.MoveAsset(null, destinationFolder, preserveOriginalFile: false));
        }

        [Fact]
        public void MoveNullSourceFolderTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            Assert.Throws<ArgumentNullException>(() =>
                catalogAssetsService.MoveAsset(new Asset { Folder = null }, destinationFolder, preserveOriginalFile: false));
        }

        [Fact]
        public void MoveNullDestinationFolderTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            Assert.Throws<ArgumentNullException>(() =>
                catalogAssetsService.MoveAsset(new Asset { Folder = new Folder { } }, null, preserveOriginalFile: false));
        }

        [Fact]
        public void CopyExistingAssetTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            Assert.Throws<ArgumentNullException>(() =>
                catalogAssetsService.DeleteAsset(null, deleteFile: true));
        }

        [Fact]
        public void DeleteNullFolderTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(storageService, userConfigurationService);
            
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            Assert.Throws<ArgumentNullException>(() =>
                catalogAssetsService.DeleteAsset(new Asset { Folder = null }, deleteFile: true));
        }

        [Fact]
        public void LogOnExceptionTest()
        {
            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetOneDriveDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetCatalogBatchSize()).Returns(1000);

            Mock<IStorageService> storageService = new Mock<IStorageService>();
            storageService.Setup(s => s.FolderExists(It.IsAny<string>())).Returns(true);
            storageService.Setup(s => s.GetFileNames(It.IsAny<string>())).Throws(new IOException());

            IAssetRepository repository = new AssetRepository(new StorageService(userConfigurationService.Object), userConfigurationService.Object);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService.Object,
                    userConfigurationService.Object,
                    new DirectoryComparer());

            string[] fileList = Directory.GetFiles(dataDirectory, "*.jp*g") // jpg and jpeg files
                .Select(f => Path.GetFileName(f))
                .ToArray();

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();
            
            catalogAssetsService.CatalogImages(e => statusChanges.Add(e));

            var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
            var exceptions = statusChanges.Where(s => s.Exception != null).Select(s => s.Exception).ToList();

            var repositoryAssets = repository.GetAssets(dataDirectory);
            Assert.Empty(processedAssets);
            Assert.Empty(repositoryAssets);
            Assert.Single(exceptions);

            bool allProcessedAssetsInFileList = processedAssets.All(a => fileList.Contains(a.FileName));
            bool allProcessedAssetsInRepository = processedAssets.All(a => repositoryAssets.Contains(a));
            bool allRepositoryAssetsInProcessed = repositoryAssets.All(a => processedAssets.Contains(a));

            Assert.True(allProcessedAssetsInFileList);
            Assert.True(allProcessedAssetsInRepository);
            Assert.True(allRepositoryAssetsInProcessed);
        }

        [Fact]
        public void SaveCatalogOnOperationCanceledExceptionTest()
        {
            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetOneDriveDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetCatalogBatchSize()).Returns(1000);

            Mock<IStorageService> storageService = new Mock<IStorageService>();
            storageService.Setup(s => s.FolderExists(It.IsAny<string>())).Returns(true);
            storageService.Setup(s => s.GetFileNames(It.IsAny<string>())).Throws(new OperationCanceledException());

            Mock<IAssetRepository> repository = new Mock<IAssetRepository>();
            
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository.Object,
                    new AssetHashCalculatorService(),
                    storageService.Object,
                    userConfigurationService.Object,
                    new DirectoryComparer());

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();

            Assert.Throws<OperationCanceledException>(() => catalogAssetsService.CatalogImages(e => statusChanges.Add(e)));
            repository.Verify(r => r.SaveCatalog(It.IsAny<Folder>()), Times.Once);
        }
    }
}
