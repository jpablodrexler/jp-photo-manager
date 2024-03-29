﻿using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Domain.Interfaces.Repositories;
using JPPhotoManager.Domain.Interfaces.Services;
using JPPhotoManager.Domain.Services;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.Infrastructure.Repositories;
using JPPhotoManager.Infrastructure.Services;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Moq;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests.Integration
{
    public class CatalogMoveAssetsServiceTests
    {
        private string _dataDirectory;
        private string _imageDestinationDirectory;
        private string _nonCataloguedImageDestinationDirectory;
        private IConfigurationRoot _configuration;
        private readonly DbContextOptions<AppDbContext> _contextOptions;
        private readonly AppDbContext _dbContext;
        private readonly SqliteConnection _connection;

        public CatalogMoveAssetsServiceTests()
        {
            _dataDirectory = Path.GetDirectoryName(typeof(CatalogMoveAssetsServiceTests).Assembly.Location);
            _dataDirectory = Path.Combine(_dataDirectory, "TestFiles");
            _imageDestinationDirectory = Path.Combine(_dataDirectory, "NewFolder");
            _nonCataloguedImageDestinationDirectory = Path.Combine(_dataDirectory, "NonCataloguedNewFolder");

            Mock<IConfigurationRoot> configurationMock = new();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", _dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", Path.Combine(_dataDirectory, "ApplicationData", Guid.NewGuid().ToString()))
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:BackupsToKeep", "2")
                .MockGetValue("appsettings:ThumbnailsDictionaryEntriesToKeep", "5");

            _configuration = configurationMock.Object;

            if (Directory.Exists(_imageDestinationDirectory))
            {
                Directory.Delete(_imageDestinationDirectory, true);
            }

            if (Directory.Exists(_nonCataloguedImageDestinationDirectory))
            {
                Directory.Delete(_nonCataloguedImageDestinationDirectory, true);
            }

            // Create and open a connection. This creates the SQLite in-memory database, which will persist until the connection is closed
            // at the end of the test (see Dispose below).
            _connection = new SqliteConnection("Filename=:memory:");
            _connection.Open();

            // These options will be used by the context instances in this test suite, including the connection opened above.
            _contextOptions = new DbContextOptionsBuilder<AppDbContext>()
                .UseSqlite(_connection)
                .Options;

            _dbContext = new AppDbContext(_contextOptions);
            _dbContext.Database.EnsureCreated();
        }

        [Fact]
        public async void CatalogFolderTest()
        {
            string appDataFolder = Path.Combine(_dataDirectory, "ApplicationData", Guid.NewGuid().ToString());
            string binaryFilesFolder = Path.Combine(appDataFolder, "Thumbnails");

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetAppFilesDirectory()).Returns(Path.Combine(appDataFolder));
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetApplicationDataFolder()).Returns(appDataFolder);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetBinaryFilesDirectory()).Returns(binaryFilesFolder);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetPicturesDirectory()).Returns(_dataDirectory);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetOneDriveDirectory()).Returns(_dataDirectory);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetCatalogBatchSize()).Returns(1000);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetThumbnailsDictionaryEntriesToKeep()).Returns(5);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetRootCatalogFolderPaths()).Returns(new string[] { _dataDirectory });

            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            var jpegFiles = Directory.GetFiles(_dataDirectory, "*.jp*g") // jpg and jpeg files
            .Select(f => Path.GetFileName(f));

            var pngFiles = Directory.GetFiles(_dataDirectory, "*.png") // png files
                .Select(f => Path.GetFileName(f));

            List<string> fileList = new();
            fileList.AddRange(jpegFiles);
            fileList.AddRange(pngFiles);

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();

            await catalogAssetsService.CatalogAssetsAsync(e => statusChanges.Add(e));

            var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
            var exceptions = statusChanges.Where(s => s.Exception != null).Select(s => s.Exception).ToList();

            var folder = folderRepository.GetFolderByPath(_dataDirectory);
            var repositoryAssets = assetRepository.GetAssets(folder, 0);
            processedAssets.Should().HaveSameCount(fileList);
            repositoryAssets.Items.Should().HaveSameCount(fileList);
            exceptions.Should().BeEmpty();

            processedAssets.Should().OnlyContain(a => fileList.Contains(a.FileName));
            processedAssets.Should().OnlyContain(a => repositoryAssets.Items.Contains(a));
            repositoryAssets.Items.Should().OnlyContain(a => processedAssets.Contains(a));
        }

        [Fact]
        public async void CatalogFolderLargerThanBatchSizeTest()
        {
            string appDataFolder = Path.Combine(_dataDirectory, "ApplicationData", Guid.NewGuid().ToString());
            string binaryFilesFolder = Path.Combine(appDataFolder, "Thumbnails");

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            int batchSize = 5;

            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetAppFilesDirectory()).Returns(Path.Combine(appDataFolder));
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetApplicationDataFolder()).Returns(appDataFolder);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetBinaryFilesDirectory()).Returns(binaryFilesFolder);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetPicturesDirectory()).Returns(_dataDirectory);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetOneDriveDirectory()).Returns(_dataDirectory);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetCatalogBatchSize()).Returns(batchSize);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetThumbnailsDictionaryEntriesToKeep()).Returns(5);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetRootCatalogFolderPaths()).Returns(new string[] { _dataDirectory });

            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            var jpegFiles = Directory.GetFiles(_dataDirectory, "*.jp*g") // jpg and jpeg files
                .Select(f => Path.GetFileName(f));

            var pngFiles = Directory.GetFiles(_dataDirectory, "*.png") // png files
                .Select(f => Path.GetFileName(f));

            List<string> fileList = new();
            fileList.AddRange(jpegFiles);
            fileList.AddRange(pngFiles);

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();

            await catalogAssetsService.CatalogAssetsAsync(e => statusChanges.Add(e));

            var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
            var exceptions = statusChanges.Where(s => s.Exception != null).Select(s => s.Exception).ToList();

            var folder = folderRepository.GetFolderByPath(_dataDirectory);
            var repositoryAssets = assetRepository.GetAssets(folder, 0);
            fileList.Should().HaveCountGreaterThan(batchSize);
            processedAssets.Should().HaveCount(batchSize);
            repositoryAssets.Items.Should().HaveCount(batchSize);
            exceptions.Should().BeEmpty();

            processedAssets.Should().OnlyContain(a => fileList.Contains(a.FileName));
            processedAssets.Should().OnlyContain(a => repositoryAssets.Items.Contains(a));
            repositoryAssets.Items.Should().OnlyContain(a => processedAssets.Contains(a));
        }

        [Fact]
        public async void CatalogFolderRemovesDeletedFileTest()
        {
            string appDataFolder = Path.Combine(_dataDirectory, "ApplicationData", Guid.NewGuid().ToString());
            string binaryFilesFolder = Path.Combine(appDataFolder, "Thumbnails");
            int batchSize = 1000;
            string deletedFile;
            var statusChanges = new List<CatalogChangeCallbackEventArgs>();
            Asset[] repositoryAssets;

            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetAppFilesDirectory()).Returns(Path.Combine(appDataFolder));
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetApplicationDataFolder()).Returns(appDataFolder);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetBinaryFilesDirectory()).Returns(binaryFilesFolder);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetPicturesDirectory()).Returns(_dataDirectory);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetOneDriveDirectory()).Returns(_dataDirectory);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetCatalogBatchSize()).Returns(batchSize);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetThumbnailsDictionaryEntriesToKeep()).Returns(5);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetRootCatalogFolderPaths()).Returns(new string[] { _dataDirectory });

                var folderRepository = mock.Container.Resolve<IFolderRepository>();
                var assetRepository = mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

                var jpegFiles = Directory.GetFiles(_dataDirectory, "*.jp*g") // jpg and jpeg files
                .Select(f => Path.GetFileName(f));

                var pngFiles = Directory.GetFiles(_dataDirectory, "*.png") // png files
                    .Select(f => Path.GetFileName(f));

                List<string> fileList = new();
                fileList.AddRange(jpegFiles);
                fileList.AddRange(pngFiles);

                await catalogAssetsService.CatalogAssetsAsync(e => statusChanges.Add(e));

                var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
                var folder = folderRepository.GetFolderByPath(_dataDirectory);
                repositoryAssets = assetRepository.GetAssets(folder, 0).Items;
                deletedFile = fileList[0];
            }

            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetAppFilesDirectory()).Returns(Path.Combine(appDataFolder));
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetApplicationDataFolder()).Returns(appDataFolder);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetBinaryFilesDirectory()).Returns(binaryFilesFolder);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetPicturesDirectory()).Returns(_dataDirectory);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetOneDriveDirectory()).Returns(_dataDirectory);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetCatalogBatchSize()).Returns(batchSize);

                mock.Mock<IDirectoryComparer>().Setup(
                    c => c.GetDeletedFileNames(It.IsAny<string[]>(), It.IsAny<List<Asset>>()))
                    .Returns(new string[] { deletedFile });

                var folderRepository = mock.Container.Resolve<IFolderRepository>();
                var assetRepository = mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

                statusChanges.Clear();
                await catalogAssetsService.CatalogAssetsAsync(e => statusChanges.Add(e));
                var folder = folderRepository.GetFolderByPath(_dataDirectory);
                var repositoryAssetsAfterDelete = assetRepository.GetAssets(folder, 0);

                repositoryAssets.Should().Contain(a => a.FileName == deletedFile);
                repositoryAssetsAfterDelete.Items.Should().NotContain(a => a.FileName == deletedFile);
                statusChanges.Should().Contain(s => s.Asset != null && s.Asset.FileName == deletedFile);
            }
        }

        [Fact]
        public async void CatalogFolderRemovesDeletedFileLargerThanBatchSizeTest()
        {
            string appDataFolder = Path.Combine(_dataDirectory, "ApplicationData", Guid.NewGuid().ToString());
            string binaryFilesFolder = Path.Combine(appDataFolder, "Thumbnails");
            int batchSize = 1000;
            var statusChanges = new List<CatalogChangeCallbackEventArgs>();
            List<string> fileList = new();

            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetAppFilesDirectory()).Returns(Path.Combine(appDataFolder));
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetApplicationDataFolder()).Returns(appDataFolder);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetBinaryFilesDirectory()).Returns(binaryFilesFolder);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetPicturesDirectory()).Returns(_dataDirectory);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetOneDriveDirectory()).Returns(_dataDirectory);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetCatalogBatchSize()).Returns(batchSize);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetThumbnailsDictionaryEntriesToKeep()).Returns(5);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetRootCatalogFolderPaths()).Returns(new string[] { _dataDirectory });

                var folderRepository = mock.Container.Resolve<IFolderRepository>();
                var assetRepository = mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

                var jpegFiles = Directory.GetFiles(_dataDirectory, "*.jp*g") // jpg and jpeg files
                .Select(f => Path.GetFileName(f));

                var pngFiles = Directory.GetFiles(_dataDirectory, "*.png") // png files
                    .Select(f => Path.GetFileName(f));

                fileList.AddRange(jpegFiles);
                fileList.AddRange(pngFiles);

                await catalogAssetsService.CatalogAssetsAsync(e => statusChanges.Add(e));

                var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
                var folder = folderRepository.GetFolderByPath(_dataDirectory);
                var repositoryAssets = assetRepository.GetAssets(folder, 0);
            }

            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                batchSize = 5;

                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetAppFilesDirectory()).Returns(Path.Combine(appDataFolder));
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetApplicationDataFolder()).Returns(appDataFolder);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetBinaryFilesDirectory()).Returns(binaryFilesFolder);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetPicturesDirectory()).Returns(_dataDirectory);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetOneDriveDirectory()).Returns(_dataDirectory);
                mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetCatalogBatchSize()).Returns(batchSize);

                mock.Mock<IDirectoryComparer>().Setup(
                    c => c.GetDeletedFileNames(It.IsAny<string[]>(), It.IsAny<List<Asset>>()))
                    .Returns(fileList.ToArray());

                var folderRepository = mock.Container.Resolve<IFolderRepository>();
                var assetRepository = mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

                statusChanges.Clear();
                await catalogAssetsService.CatalogAssetsAsync(e => statusChanges.Add(e));
                var folder = folderRepository.GetFolderByPath(_dataDirectory);
                var repositoryAssetsAfterDelete = assetRepository.GetAssets(folder, 0);

                repositoryAssetsAfterDelete.Items.Should().HaveCount(fileList.Count - batchSize);
            }
        }

        [Fact]
        public void CatalogNonExistentFolderTest()
        {
            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetApplicationDataFolder()).Returns(Path.Combine(_dataDirectory, "ApplicationData", Guid.NewGuid().ToString()));
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetPicturesDirectory()).Returns(Path.Combine(_dataDirectory, "NonExistent"));

            var repository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();

            catalogAssetsService.CatalogAssetsAsync(e => statusChanges.Add(e));

            statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).Should().BeEmpty();
            statusChanges.Where(s => s.Exception != null).Select(s => s.Exception).Should().BeEmpty();
        }

        [Theory]
        [InlineData("Image 2.jpg", 30197, 720, 1280, "0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078")]
        [InlineData("Image 1.jpg", 29857, 720, 1280, "1fafae17c3c5c38d1205449eebdb9f5976814a5e54ec5797270c8ec467fe6d6d1190255cbaac11d9057c4b2697d90bc7116a46ed90c5ffb71e32e569c3b47fb9")]
        public void CreateAssetTest(string fileName, int fileSize, int pixelHeight, int pixelWidth, string hash)
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            var repository = mock.Container.Resolve<IFolderRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            repository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, fileName);
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, fileName);

            asset.FileName.Should().Be(fileName);
            asset.FileSize.Should().Be(fileSize);
            asset.Folder.Path.Should().Be(_dataDirectory);
            asset.FullPath.Should().Be(imagePath);
            asset.PixelHeight.Should().Be(pixelHeight);
            asset.PixelWidth.Should().Be(pixelWidth);
            asset.Hash.Should().Be(hash);
            asset.ThumbnailCreationDateTime.Should().NotBe(DateTime.MinValue);
        }

        [Fact]
        public void CreateAssetOfDuplicatedFilesCompareHashesTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            var repository = mock.Container.Resolve<IFolderRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            repository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 2 duplicated.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2 duplicated.jpg");

            Assert.NotEqual(asset.FileName, duplicatedAsset.FileName);
            duplicatedAsset.Hash.Should().Be(asset.Hash);
        }

        [Fact]
        public void CreateAssetOfDifferentFilesCompareHashesTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            var repository = mock.Container.Resolve<IFolderRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            repository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 1.jpg");

            duplicatedAsset.FileName.Should().NotBe(asset.FileName);
            duplicatedAsset.Hash.Should().NotBe(asset.Hash);
        }

        [Fact]
        public void MoveExistingAssetTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<RecentTargetPathRepository>().As<IRecentTargetPathRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });
            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = folderRepository.AddFolder(_dataDirectory);
            Folder destinationFolder = folderRepository.AddFolder(_imageDestinationDirectory);

            string sourceImagePath = Path.Combine(_dataDirectory, "Image 4.jpg");
            string destinationImagePath = Path.Combine(_imageDestinationDirectory, "Image 4.jpg");
            File.Exists(sourceImagePath).Should().BeTrue();
            File.Exists(destinationImagePath).Should().BeFalse();

            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 4.jpg");
            
            assetRepository.ContainsThumbnail(sourceFolder, asset.FileName).Should().BeTrue();
            assetRepository.ContainsThumbnail(destinationFolder, asset.FileName).Should().BeFalse();

            moveAssetsService.MoveAssets(new Asset[] { asset }, destinationFolder, preserveOriginalFile: false).Should().BeTrue();

            File.Exists(sourceImagePath).Should().BeFalse();
            File.Exists(destinationImagePath).Should().BeTrue();

            assetRepository.ContainsThumbnail(sourceFolder, asset.FileName).Should().BeFalse();
            assetRepository.ContainsThumbnail(destinationFolder, asset.FileName).Should().BeFalse();

            // Validates if the catalogued assets for the source folder are updated properly.
            var assets = assetRepository.GetCataloguedAssets(sourceFolder);
            assets.Should().NotContain(a => a.FileName == "Image 4.jpg");

            // Validates if the catalogued assets for the destination folder are not updated yet.
            assets = assetRepository.GetCataloguedAssets(destinationFolder);
            assets.Should().NotContain(a => a.FileName == "Image 4.jpg");
        }

        [Fact]
        public void MoveNonExistingAssetTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IFolderRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = repository.AddFolder(_dataDirectory);
            Folder destinationFolder = repository.AddFolder(_imageDestinationDirectory);

            string sourceImagePath = Path.Combine(_dataDirectory, "Nonexistent Image.jpg");
            string destinationImagePath = Path.Combine(_imageDestinationDirectory, "Nonexistent Image.jpg");
            File.Exists(sourceImagePath).Should().BeFalse();
            File.Exists(destinationImagePath).Should().BeFalse();

            Asset asset = new()
            {
                FileName = "Nonexistent Image.jpg",
                Folder = sourceFolder,
                FolderId = sourceFolder.FolderId
            };

            asset.Folder.Should().Be(sourceFolder);
            asset.Folder.Should().NotBe(destinationFolder);

            Func<bool> function = () =>
                moveAssetsService.MoveAssets(new Asset[] { asset }, destinationFolder, preserveOriginalFile: false);
            function.Should().Throw<ArgumentException>();
        }

        [Fact]
        public void MoveAssetToSamePathTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });
            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = folderRepository.AddFolder(_dataDirectory);

            string sourceImagePath = Path.Combine(_dataDirectory, "Image 5.jpg");
            File.Exists(sourceImagePath).Should().BeTrue();

            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 5.jpg");

            assetRepository.ContainsThumbnail(asset.Folder, asset.FileName).Should().BeTrue();

            moveAssetsService.MoveAssets(new Asset[] { asset }, sourceFolder, preserveOriginalFile: false).Should().BeFalse();

            File.Exists(sourceImagePath).Should().BeTrue();

            assetRepository.ContainsThumbnail(sourceFolder, asset.FileName).Should().BeTrue();
        }

        [Fact]
        public void MoveAssetToNonCataloguedFolderTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<RecentTargetPathRepository>().As<IRecentTargetPathRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });
            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = folderRepository.AddFolder(_dataDirectory);
            Folder destinationFolder = folderRepository.GetFolderByPath(_nonCataloguedImageDestinationDirectory);

            destinationFolder.Should().BeNull();

            destinationFolder = new Folder { Path = _nonCataloguedImageDestinationDirectory };

            string sourceImagePath = Path.Combine(_dataDirectory, "Image 7.jpg");
            File.Exists(sourceImagePath).Should().BeTrue();

            string destinationImagePath = Path.Combine(_nonCataloguedImageDestinationDirectory, "Image 7.jpg");
            File.Exists(destinationImagePath).Should().BeFalse();

            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 7.jpg");
            
            Assert.True(assetRepository.ContainsThumbnail(sourceFolder, asset.FileName));

            moveAssetsService.MoveAssets(new Asset[] { asset }, destinationFolder, preserveOriginalFile: false).Should().BeTrue();

            File.Exists(sourceImagePath).Should().BeFalse();
            File.Exists(destinationImagePath).Should().BeTrue();
            assetRepository.ContainsThumbnail(sourceFolder, asset.FileName).Should().BeFalse();
        }

        [Fact]
        public void MoveNullAssetTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });

            var repository = mock.Container.Resolve<IFolderRepository>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = repository.AddFolder(_dataDirectory);
            Folder destinationFolder = repository.AddFolder(_imageDestinationDirectory);

            Func<bool> function = () =>
                moveAssetsService.MoveAssets(null, destinationFolder, preserveOriginalFile: false);
            function.Should().Throw<ArgumentNullException>();
        }

        [Fact]
        public void MoveNullSourceFolderTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });

            var repository = mock.Container.Resolve<IFolderRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = repository.AddFolder(_dataDirectory);
            Folder destinationFolder = repository.AddFolder(_imageDestinationDirectory);

            Func<bool> function = () =>
                moveAssetsService.MoveAssets(new Asset[] { new Asset { Folder = null } }, destinationFolder, preserveOriginalFile: false);
            function.Should().Throw<ArgumentNullException>();
        }

        [Fact]
        public void MoveNullDestinationFolderTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });

            var repository = mock.Container.Resolve<IFolderRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = repository.AddFolder(_dataDirectory);
            Folder destinationFolder = repository.AddFolder(_imageDestinationDirectory);

            Func<bool> function = () =>
                moveAssetsService.MoveAssets(new Asset[] { new Asset { Folder = new Folder { } } }, null, preserveOriginalFile: false);
            function.Should().Throw<ArgumentNullException>();
        }

        [Fact]
        public void CopyExistingAssetTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<RecentTargetPathRepository>().As<IRecentTargetPathRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });

            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = folderRepository.AddFolder(_dataDirectory);
            Folder destinationFolder = folderRepository.AddFolder(_imageDestinationDirectory);

            string sourceImagePath = Path.Combine(_dataDirectory, "Image 5.jpg");
            string destinationImagePath = Path.Combine(_imageDestinationDirectory, "Image 5.jpg");
            File.Exists(sourceImagePath).Should().BeTrue();
            File.Exists(destinationImagePath).Should().BeFalse();

            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 5.jpg");
            
            assetRepository.ContainsThumbnail(sourceFolder, asset.FileName).Should().BeTrue();
            assetRepository.ContainsThumbnail(destinationFolder, asset.FileName).Should().BeFalse();

            moveAssetsService.MoveAssets(new Asset[] { asset }, destinationFolder, preserveOriginalFile: true).Should().BeTrue();

            File.Exists(sourceImagePath).Should().BeTrue();
            File.Exists(destinationImagePath).Should().BeTrue();

            assetRepository.ContainsThumbnail(sourceFolder, asset.FileName).Should().BeTrue();
            assetRepository.ContainsThumbnail(destinationFolder, asset.FileName).Should().BeFalse();

            // Validates if the catalogued assets for the source folder are updated properly.
            var assets = assetRepository.GetCataloguedAssets(sourceFolder);
            assets.Should().ContainSingle(a => a.FileName == "Image 5.jpg");

            // Validates if the catalogued assets for the destination folder are not updated yet.
            assets = assetRepository.GetCataloguedAssets(destinationFolder);
            assets.Should().NotContain(a => a.FileName == "Image 5.jpg");
        }

        [Fact]
        public void CopyNonExistingAssetTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });

            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = folderRepository.AddFolder(_dataDirectory);
            Folder destinationFolder = folderRepository.AddFolder(_imageDestinationDirectory);

            string sourceImagePath = Path.Combine(_dataDirectory, "Nonexistent Image.jpg");
            string destinationImagePath = Path.Combine(_imageDestinationDirectory, "Nonexistent Image.jpg");
            File.Exists(sourceImagePath).Should().BeFalse();
            File.Exists(destinationImagePath).Should().BeFalse();

            Asset asset = new()
            {
                FileName = "Nonexistent Image.jpg",
                Folder = sourceFolder,
                FolderId = sourceFolder.FolderId
            };

            asset.Folder.Should().Be(sourceFolder);
            asset.Folder.Should().NotBe(destinationFolder);

            Func<bool> function = () =>
                moveAssetsService.MoveAssets(new Asset[] { asset }, destinationFolder, preserveOriginalFile: true);
            function.Should().Throw<ArgumentException>();
        }

        [Fact]
        public void CopyAssetToSamePathTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });

            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = folderRepository.AddFolder(_dataDirectory);

            string sourceImagePath = Path.Combine(_dataDirectory, "Image 5.jpg");
            File.Exists(sourceImagePath).Should().BeTrue();

            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 5.jpg");
            
            Assert.True(assetRepository.ContainsThumbnail(sourceFolder, asset.FileName));

            moveAssetsService.MoveAssets(new Asset[] { asset }, sourceFolder, preserveOriginalFile: true).Should().BeFalse();

            File.Exists(sourceImagePath).Should().BeTrue();
            assetRepository.ContainsThumbnail(sourceFolder, asset.FileName).Should().BeTrue();
        }

        [Fact]
        public void DeleteExistingImageTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });
            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = folderRepository.AddFolder(_dataDirectory);

            string sourceImagePath = Path.Combine(_dataDirectory, "Image 6.jpg");
            File.Exists(sourceImagePath).Should().BeTrue();

            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 6.jpg");
            
            Assert.True(assetRepository.ContainsThumbnail(sourceFolder, asset.FileName));

            moveAssetsService.DeleteAssets(new Asset[] { asset }, deleteFile: true);

            File.Exists(sourceImagePath).Should().BeFalse();
            assetRepository.ContainsThumbnail(sourceFolder, asset.FileName).Should().BeFalse();
        }

        [Fact]
        public void DeleteNonExistingImageTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });

            var repository = mock.Container.Resolve<IFolderRepository>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = repository.AddFolder(_dataDirectory);

            string sourceImagePath = Path.Combine(_dataDirectory, "Nonexistent Image.jpg");
            File.Exists(sourceImagePath).Should().BeFalse();

            Asset asset = new()
            {
                FileName = "Nonexistent Image.jpg",
                Folder = sourceFolder,
                FolderId = sourceFolder.FolderId
            };

            Action action = () =>
                moveAssetsService.DeleteAssets(new Asset[] { asset }, deleteFile: true);
            action.Should().Throw<ArgumentException>();
        }

        [Fact]
        public void DeleteNullAssetTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });

            var repository = mock.Container.Resolve<IFolderRepository>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Folder sourceFolder = repository.AddFolder(_dataDirectory);

            Action action = () =>
                moveAssetsService.DeleteAssets(null, deleteFile: true);
            action.Should().Throw<ArgumentNullException>();
        }

        [Fact]
        public void DeleteNullFolderTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            Action action = () =>
                moveAssetsService.DeleteAssets(new Asset[] { new Asset { Folder = null } }, deleteFile: true);
            action.Should().Throw<ArgumentNullException>();
        }

        [Fact]
        public async void LogOnExceptionTest()
        {
            string appDataFolder = Path.Combine(_dataDirectory, "ApplicationData", Guid.NewGuid().ToString());

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                    cfg.RegisterType<MoveAssetsService>().As<IMoveAssetsService>().SingleInstance();
                });
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetApplicationDataFolder()).Returns(appDataFolder);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetPicturesDirectory()).Returns(_dataDirectory);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetOneDriveDirectory()).Returns(_dataDirectory);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetCatalogBatchSize()).Returns(1000);
            mock.Mock<IUserConfigurationService>().Setup(conf => conf.GetRootCatalogFolderPaths()).Returns(new string[] { _dataDirectory });

            mock.Mock<IStorageService>().Setup(s => s.FolderExists(It.IsAny<string>())).Returns(true);
            mock.Mock<IStorageService>().Setup(s => s.GetFileNames(It.IsAny<string>())).Throws(new IOException());
            mock.Mock<IStorageService>().Setup(s => s.ResolveDataDirectory(It.IsAny<double>())).Returns(appDataFolder);

            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var moveAssetsService = mock.Container.Resolve<IMoveAssetsService>();

            string[] fileList = Directory.GetFiles(_dataDirectory, "*.jp*g") // jpg and jpeg files
            .Select(f => Path.GetFileName(f))
            .ToArray();

            var statusChanges = new List<CatalogChangeCallbackEventArgs>();

            await catalogAssetsService.CatalogAssetsAsync(e => statusChanges.Add(e));

            var processedAssets = statusChanges.Where(s => s.Asset != null).Select(s => s.Asset).ToList();
            var exceptions = statusChanges.Where(s => s.Exception != null).Select(s => s.Exception).ToList();

            var folder = folderRepository.GetFolderByPath(_dataDirectory);
            var repositoryAssets = assetRepository.GetAssets(folder, 0);
            processedAssets.Should().BeEmpty();
            repositoryAssets.Items.Should().BeEmpty();
            exceptions.Should().ContainSingle();
        }
    }
}
