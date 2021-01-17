using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Moq;
using SimplePortableDatabase;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class CatalogMoveAssetsServiceTest
    {
        private string dataDirectory;
        private string imageDestinationDirectory;
        private string nonCataloguedImageDestinationDirectory;
        private IConfigurationRoot configuration;

        public CatalogMoveAssetsServiceTest()
        {
            dataDirectory = Path.GetDirectoryName(typeof(CatalogMoveAssetsServiceTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");
            imageDestinationDirectory = Path.Combine(dataDirectory, "NewFolder");
            nonCataloguedImageDestinationDirectory = Path.Combine(dataDirectory, "NonCataloguedNewFolder");

            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", Path.Combine(dataDirectory, Guid.NewGuid().ToString()))
                .MockGetValue("appsettings:CatalogBatchSize", "100");

            configuration = configurationMock.Object;

            if (Directory.Exists(imageDestinationDirectory))
            {
                Directory.Delete(imageDestinationDirectory, true);
            }

            if (Directory.Exists(nonCataloguedImageDestinationDirectory))
            {
                Directory.Delete(nonCataloguedImageDestinationDirectory, true);
            }
        }

        // TODO: MOVE TO INTEGRATION TESTS PROJECT
        [Fact]
        public void CatalogFolderTest()
        {
            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<Database>().As<IDatabase>().SingleInstance();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetApplicationDataFolder()).Returns(Path.Combine(dataDirectory, Guid.NewGuid().ToString()));
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetPicturesDirectory()).Returns(dataDirectory);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetOneDriveDirectory()).Returns(dataDirectory);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetCatalogBatchSize()).Returns(1000);

                var repository = mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

                var jpegFiles = Directory.GetFiles(dataDirectory, "*.jp*g") // jpg and jpeg files
                .Select(f => Path.GetFileName(f));

                var pngFiles = Directory.GetFiles(dataDirectory, "*.png") // png files
                    .Select(f => Path.GetFileName(f));

                List<string> fileList = new List<string>();
                fileList.AddRange(jpegFiles);
                fileList.AddRange(pngFiles);

                var statusChanges = new List<CatalogChangeCallbackEventArgs>();

                catalogAssetsService.CatalogAssets(e => statusChanges.Add(e));

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
        }

        // TODO: MOVE TO INTEGRATION TESTS PROJECT
        [Fact]
        public void CatalogFolderLargerThanBatchSizeTest()
        {
            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<Database>().As<IDatabase>().SingleInstance();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                int batchSize = 5;

                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetApplicationDataFolder()).Returns(Path.Combine(dataDirectory, Guid.NewGuid().ToString()));
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetPicturesDirectory()).Returns(dataDirectory);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetOneDriveDirectory()).Returns(dataDirectory);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetCatalogBatchSize()).Returns(batchSize);

                var repository = mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

                var jpegFiles = Directory.GetFiles(dataDirectory, "*.jp*g") // jpg and jpeg files
                    .Select(f => Path.GetFileName(f));

                var pngFiles = Directory.GetFiles(dataDirectory, "*.png") // png files
                    .Select(f => Path.GetFileName(f));

                List<string> fileList = new List<string>();
                fileList.AddRange(jpegFiles);
                fileList.AddRange(pngFiles);

                var statusChanges = new List<CatalogChangeCallbackEventArgs>();

                catalogAssetsService.CatalogAssets(e => statusChanges.Add(e));

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
        }

        [Fact]
        public void CatalogFolderRemovesDeletedFileTest()
        {
            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(Path.Combine(dataDirectory, Guid.NewGuid().ToString()));
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetOneDriveDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetCatalogBatchSize()).Returns(1000);

            IDatabase database = new Database();
            IStorageService storageService = new StorageService(userConfigurationService.Object);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService.Object);
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

            catalogAssetsService.CatalogAssets(e => statusChanges.Add(e));

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
            catalogAssetsService.CatalogAssets(e => statusChanges.Add(e));
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
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(Path.Combine(dataDirectory, Guid.NewGuid().ToString()));
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetOneDriveDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetCatalogBatchSize()).Returns(batchSize);

            IDatabase database = new Database();
            IStorageService storageService = new StorageService(userConfigurationService.Object);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService.Object);
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

            catalogAssetsService.CatalogAssets(e => statusChanges.Add(e));

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
            catalogAssetsService.CatalogAssets(e => statusChanges.Add(e));
            var repositoryAssetsAfterDelete = repository.GetAssets(dataDirectory);

            repositoryAssetsAfterDelete.Should().HaveCount(fileList.Count - batchSize);
        }

        [Fact]
        public void CatalogNonExistentFolderTest()
        {
            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(Path.Combine(dataDirectory, Guid.NewGuid().ToString()));
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(Path.Combine(dataDirectory, "NonExistent"));

            IDatabase database = new Database();
            IStorageService storageService = new StorageService(userConfigurationService.Object);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService.Object);
            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    assetHashCalculatorService,
                    storageService,
                    userConfigurationService.Object,
                    directoryComparer);

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();

            catalogAssetsService.CatalogAssets(e => statusChanges.Add(e));

            statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).Should().BeEmpty();
            statusChanges.Where(s => s.Exception != null).Select(s => s.Exception).Should().BeEmpty();
        }

        [Fact]
        public void CreateAssetTest1()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
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
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            asset.FileName.Should().Be("Image 1.jpg");
            asset.FileSize.Should().Be(29857);
            asset.Folder.Path.Should().Be(dataDirectory);
            asset.FullPath.Should().Be(imagePath);
            asset.PixelHeight.Should().Be(720);
            asset.PixelWidth.Should().Be(1280);
            asset.Hash.Should().Be("1fafae17c3c5c38d1205449eebdb9f5976814a5e54ec5797270c8ec467fe6d6d1190255cbaac11d9057c4b2697d90bc7116a46ed90c5ffb71e32e569c3b47fb9");
            asset.ThumbnailCreationDateTime.Should().NotBe(DateTime.MinValue);
        }

        [Fact]
        public void CreateAssetOfDuplicatedFilesCompareHashesTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
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

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

            Assert.NotEqual(asset.FileName, duplicatedAsset.FileName);
            duplicatedAsset.Hash.Should().Be(asset.Hash);
        }

        [Fact]
        public void CreateAssetOfDifferentFilesCompareHashesTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
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

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            duplicatedAsset.FileName.Should().NotBe(asset.FileName);
            duplicatedAsset.Hash.Should().NotBe(asset.Hash);
        }

        [Fact]
        public void MoveExistingAssetTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 4.jpg");
            string destinationImagePath = Path.Combine(imageDestinationDirectory, "Image 4.jpg");
            File.Exists(sourceImagePath).Should().BeTrue();
            File.Exists(destinationImagePath).Should().BeFalse();

            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 4.jpg");
            repository.SaveCatalog(sourceFolder);
            repository.SaveCatalog(destinationFolder);

            repository.ContainsThumbnail(sourceFolder.Path, asset.FileName).Should().BeTrue();
            repository.ContainsThumbnail(destinationFolder.Path, asset.FileName).Should().BeFalse();

            moveAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: false).Should().BeTrue();

            File.Exists(sourceImagePath).Should().BeFalse();
            File.Exists(destinationImagePath).Should().BeTrue();

            repository.ContainsThumbnail(sourceFolder.Path, asset.FileName).Should().BeFalse();
            repository.ContainsThumbnail(destinationFolder.Path, asset.FileName).Should().BeTrue();

            // Validates if the catalogued assets for the source folder are updated properly.
            var assets = repository.GetCataloguedAssets(sourceFolder.Path);
            assets.Should().NotContain(a => a.FileName == "Image 4.jpg");

            // Validates if the catalogued assets for the destination folder are updated properly.
            assets = repository.GetCataloguedAssets(destinationFolder.Path);
            assets.Should().ContainSingle(a => a.FileName == "Image 4.jpg");
        }

        [Fact]
        public void MoveNonExistingAssetTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            string sourceImagePath = Path.Combine(dataDirectory, "Nonexistent Image.jpg");
            string destinationImagePath = Path.Combine(imageDestinationDirectory, "Nonexistent Image.jpg");
            File.Exists(sourceImagePath).Should().BeFalse();
            File.Exists(destinationImagePath).Should().BeFalse();

            Asset asset = new Asset
            {
                FileName = "Nonexistent Image.jpg",
                Folder = sourceFolder,
                FolderId = sourceFolder.FolderId
            };

            asset.Folder.Should().Be(sourceFolder);
            asset.Folder.Should().NotBe(destinationFolder);

            Func<bool> function = () =>
                moveAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: false);
            function.Should().Throw<ArgumentException>();
        }

        [Fact]
        public void MoveAssetToSamePathTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 5.jpg");
            File.Exists(sourceImagePath).Should().BeTrue();

            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 5.jpg");
            repository.SaveCatalog(sourceFolder);

            repository.ContainsThumbnail(asset.Folder.Path, asset.FileName).Should().BeTrue();

            moveAssetsService.MoveAsset(asset, sourceFolder, preserveOriginalFile: false).Should().BeFalse();

            File.Exists(sourceImagePath).Should().BeTrue();

            repository.ContainsThumbnail(sourceFolder.Path, asset.FileName).Should().BeTrue();
        }

        [Fact]
        public void MoveAssetToNonCataloguedFolderTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.GetFolderByPath(nonCataloguedImageDestinationDirectory);

            destinationFolder.Should().BeNull();

            destinationFolder = new Folder { Path = nonCataloguedImageDestinationDirectory };
            
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 7.jpg");
            File.Exists(sourceImagePath).Should().BeTrue();

            string destinationImagePath = Path.Combine(nonCataloguedImageDestinationDirectory, "Image 7.jpg");
            File.Exists(destinationImagePath).Should().BeFalse();

            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 7.jpg");
            repository.SaveCatalog(sourceFolder);

            Assert.True(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));

            moveAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: false).Should().BeTrue();

            File.Exists(sourceImagePath).Should().BeFalse();
            File.Exists(destinationImagePath).Should().BeTrue();
            repository.ContainsThumbnail(sourceFolder.Path, asset.FileName).Should().BeFalse();
        }

        [Fact]
        public void MoveNullAssetTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            Func<bool> function = () =>
                moveAssetsService.MoveAsset(null, destinationFolder, preserveOriginalFile: false);
            function.Should().Throw<ArgumentNullException>();
        }

        [Fact]
        public void MoveNullSourceFolderTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            Func<bool> function = () =>
                moveAssetsService.MoveAsset(new Asset { Folder = null }, destinationFolder, preserveOriginalFile: false);
            function.Should().Throw<ArgumentNullException>();
        }

        [Fact]
        public void MoveNullDestinationFolderTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            Func<bool> function = () =>
                moveAssetsService.MoveAsset(new Asset { Folder = new Folder { } }, null, preserveOriginalFile: false);
            function.Should().Throw<ArgumentNullException>();
        }

        [Fact]
        public void CopyExistingAssetTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 5.jpg");
            string destinationImagePath = Path.Combine(imageDestinationDirectory, "Image 5.jpg");
            File.Exists(sourceImagePath).Should().BeTrue();
            File.Exists(destinationImagePath).Should().BeFalse();

            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 5.jpg");
            repository.SaveCatalog(sourceFolder);
            repository.SaveCatalog(destinationFolder);

            repository.ContainsThumbnail(sourceFolder.Path, asset.FileName).Should().BeTrue();
            repository.ContainsThumbnail(destinationFolder.Path, asset.FileName).Should().BeFalse();

            moveAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: true).Should().BeTrue();

            File.Exists(sourceImagePath).Should().BeTrue();
            File.Exists(destinationImagePath).Should().BeTrue();

            repository.ContainsThumbnail(sourceFolder.Path, asset.FileName).Should().BeTrue();
            repository.ContainsThumbnail(destinationFolder.Path, asset.FileName).Should().BeTrue();

            // Validates if the catalogued assets for the source folder are updated properly.
            var assets = repository.GetCataloguedAssets(sourceFolder.Path);
            assets.Should().ContainSingle(a => a.FileName == "Image 5.jpg");

            // Validates if the catalogued assets for the destination folder are updated properly.
            assets = repository.GetCataloguedAssets(destinationFolder.Path);
            assets.Should().ContainSingle(a => a.FileName == "Image 5.jpg");
        }

        [Fact]
        public void CopyNonExistingAssetTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            string sourceImagePath = Path.Combine(dataDirectory, "Nonexistent Image.jpg");
            string destinationImagePath = Path.Combine(imageDestinationDirectory, "Nonexistent Image.jpg");
            File.Exists(sourceImagePath).Should().BeFalse();
            File.Exists(destinationImagePath).Should().BeFalse();

            Asset asset = new Asset
            {
                FileName = "Nonexistent Image.jpg",
                Folder = sourceFolder,
                FolderId = sourceFolder.FolderId
            };

            asset.Folder.Should().Be(sourceFolder);
            asset.Folder.Should().NotBe(destinationFolder);

            Func<bool> function = () =>
                moveAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: true);
            function.Should().Throw<ArgumentException>();
        }

        [Fact]
        public void CopyAssetToSamePathTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);
            
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 5.jpg");
            File.Exists(sourceImagePath).Should().BeTrue();
            
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 5.jpg");
            repository.SaveCatalog(sourceFolder);
            
            Assert.True(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));

            moveAssetsService.MoveAsset(asset, sourceFolder, preserveOriginalFile: true).Should().BeFalse();

            File.Exists(sourceImagePath).Should().BeTrue();
            repository.ContainsThumbnail(sourceFolder.Path, asset.FileName).Should().BeTrue();
        }

        [Fact]
        public void DeleteExistingImageTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 6.jpg");
            File.Exists(sourceImagePath).Should().BeTrue();

            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 6.jpg");
            repository.SaveCatalog(sourceFolder);

            Assert.True(repository.ContainsThumbnail(sourceFolder.Path, asset.FileName));

            moveAssetsService.DeleteAsset(asset, deleteFile: true);

            File.Exists(sourceImagePath).Should().BeFalse();
            repository.ContainsThumbnail(sourceFolder.Path, asset.FileName).Should().BeFalse();
        }

        [Fact]
        public void DeleteNonExistingImageTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            string sourceImagePath = Path.Combine(dataDirectory, "Nonexistent Image.jpg");
            File.Exists(sourceImagePath).Should().BeFalse();

            Asset asset = new Asset
            {
                FileName = "Nonexistent Image.jpg",
                Folder = sourceFolder,
                FolderId = sourceFolder.FolderId
            };

            Action action = () =>
                moveAssetsService.DeleteAsset(asset, deleteFile: true);
            action.Should().Throw<ArgumentException>();
        }

        [Fact]
        public void DeleteNullAssetTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            Folder sourceFolder = repository.AddFolder(dataDirectory);

            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            Action action = () =>
                moveAssetsService.DeleteAsset(null, deleteFile: true);
            action.Should().Throw<ArgumentNullException>();
        }

        [Fact]
        public void DeleteNullFolderTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    userConfigurationService,
                    new DirectoryComparer());

            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                    repository,
                    storageService,
                    catalogAssetsService);

            Action action = () =>
                moveAssetsService.DeleteAsset(new Asset { Folder = null }, deleteFile: true);
            action.Should().Throw<ArgumentNullException>();
        }

        [Fact]
        public void LogOnExceptionTest()
        {
            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(Path.Combine(dataDirectory, Guid.NewGuid().ToString()));
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetOneDriveDirectory()).Returns(dataDirectory);
            userConfigurationService.Setup(conf => conf.GetCatalogBatchSize()).Returns(1000);

            Mock<IStorageService> storageService = new Mock<IStorageService>();
            storageService.Setup(s => s.FolderExists(It.IsAny<string>())).Returns(true);
            storageService.Setup(s => s.GetFileNames(It.IsAny<string>())).Throws(new IOException());

            IDatabase database = new Database();
            IAssetRepository repository = new AssetRepository(database, new StorageService(userConfigurationService.Object), userConfigurationService.Object);
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
            
            catalogAssetsService.CatalogAssets(e => statusChanges.Add(e));

            var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
            var exceptions = statusChanges.Where(s => s.Exception != null).Select(s => s.Exception).ToList();

            var repositoryAssets = repository.GetAssets(dataDirectory);
            processedAssets.Should().BeEmpty();
            repositoryAssets.Should().BeEmpty();
            exceptions.Should().ContainSingle();
        }

        [Fact]
        public void SaveCatalogOnOperationCanceledExceptionTest()
        {
            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetApplicationDataFolder()).Returns(Path.Combine(dataDirectory, Guid.NewGuid().ToString()));
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

            Action action = () => catalogAssetsService.CatalogAssets(e => statusChanges.Add(e));
            action.Should().Throw<OperationCanceledException>();
            repository.Verify(r => r.SaveCatalog(It.IsAny<Folder>()), Times.Once);
        }
    }
}
