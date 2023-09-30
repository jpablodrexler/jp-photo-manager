using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;
using JPPhotoManager.Infrastructure;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Moq;
using System.Data.Common;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests.Integration
{
    public class ApplicationTests : IDisposable
    {
        private string _dataDirectory;
        private IConfigurationRoot _configuration;
        private readonly AppDbContext _dbContext;
        private readonly DbConnection _connection;
        private readonly DbContextOptions<AppDbContext> _contextOptions;

        public ApplicationTests()
        {
            _dataDirectory = Path.GetDirectoryName(typeof(ApplicationTests).Assembly.Location);
            _dataDirectory = Path.Combine(_dataDirectory, "TestFiles");

            Mock<IConfigurationRoot> configurationMock = new();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", _dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", Path.Combine(_dataDirectory, "ApplicationData", Guid.NewGuid().ToString()))
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:BackupsToKeep", "2")
                .MockGetValue("appsettings:ThumbnailsDictionaryEntriesToKeep", "5");

            _configuration = configurationMock.Object;

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
        public void GetDuplicatedAssets_WithDuplicates_ReturnArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var storageService = mock.Container.Resolve<IStorageService>();
            var app = mock.Container.Resolve<Application.Application>();

            Folder folder = folderRepository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 2 duplicated.jpg");
            Assert.True(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2 duplicated.jpg");

            var duplicatedAssetSets = app.GetDuplicatedAssets();
            duplicatedAssetSets.Should().ContainSingle();

            var duplicatedAssets = duplicatedAssetSets[0];
            duplicatedAssets.Should().HaveCount(2);
            duplicatedAssets[0].FileName.Should().Be("Image 2.jpg");
            duplicatedAssets[1].FileName.Should().Be("Image 2 duplicated.jpg");

            assetRepository.ContainsThumbnail(duplicatedAssets[0].Folder, duplicatedAssets[0].FileName).Should().BeTrue();
            assetRepository.ContainsThumbnail(duplicatedAssets[1].Folder, duplicatedAssets[1].FileName).Should().BeTrue();
            assetRepository.LoadThumbnail(duplicatedAssets[0].Folder, duplicatedAssets[0].FileName, duplicatedAssets[0].ThumbnailPixelWidth, duplicatedAssets[0].ThumbnailPixelHeight).Should().NotBeNull();
            assetRepository.LoadThumbnail(duplicatedAssets[1].Folder, duplicatedAssets[1].FileName, duplicatedAssets[1].ThumbnailPixelWidth, duplicatedAssets[1].ThumbnailPixelHeight).Should().NotBeNull();
        }

        [Fact]
        public void GetDuplicatedAssets_WithoutDuplicates_ReturnEmptyArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            var repository = mock.Container.Resolve<IFolderRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var app = mock.Container.Resolve<Application.Application>();

            repository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset anotherAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 1.jpg");

            var duplicatedAssetSets = app.GetDuplicatedAssets();
            duplicatedAssetSets.Should().BeEmpty();
        }

        /// <summary>
        /// Tests an scenario when the user searches for duplicates before an
        /// old entry gets deleted from the catalog.
        /// </summary>
        [Fact]
        public void GetDuplicatedAssets_WithInexistingDuplicatedAsset_ReturnEmptyArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var app = mock.Container.Resolve<Application.Application>();

            folderRepository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset anotherAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(_dataDirectory, "Inexistent Image.jpg");
            File.Exists(imagePath).Should().BeFalse();

            Asset inexistentAsset = new()
            {
                FileName = "Inexistent Image.jpg",
                Folder = asset.Folder,
                FolderId = asset.FolderId,
                Hash = asset.Hash
            };

            assetRepository.AddAsset(inexistentAsset, asset.Folder, null);
            var duplicatedAssetSets = app.GetDuplicatedAssets();
            duplicatedAssetSets.Should().BeEmpty();
        }

        /// <summary>
        /// Tests an scenario when the user searches for duplicates before an
        /// old entry gets deleted from the catalog.
        /// </summary>
        [Fact]
        public void GetDuplicatedAssets_WithInexistingNotDuplicatedAsset_ReturnEmptyArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);
            
            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var app = mock.Container.Resolve<Application.Application>();

            Folder folder = folderRepository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, "Inexistent Image.jpg");
            File.Exists(imagePath).Should().BeFalse();

            assetRepository.AddAsset(new Asset
            {
                AssetId = 1,
                FileName = "Inexistent Image.jpg",
                Folder = folder,
                FolderId = folder.FolderId,
                Hash = "0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078"
            }, folder, null);

            imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset anotherAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 1.jpg");

            var duplicatedAssetSets = app.GetDuplicatedAssets();
            duplicatedAssetSets.Should().BeEmpty();
        }

        [Fact]
        public void AddAssets_ToNonExistingFolder_AddFolderToCatalog()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                });

            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();

            Folder folder = new() { Path = "C:\\Inexistent Folder" };

            string imagePath = Path.Combine(_dataDirectory, "Inexistent Image.jpg");
            File.Exists(imagePath).Should().BeFalse();

            assetRepository.AddAsset(new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = folder,
                FolderId = folder.FolderId,
                Hash = "0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078"
            }, folder, null);

            folder = folderRepository.GetFolderByPath("C:\\Inexistent Folder");
            folder.Should().NotBeNull();
            folder.Path.Should().Be("C:\\Inexistent Folder");
            folder.Name.Should().Be("Inexistent Folder");
        }

        [Fact]
        public void GetAssets_WithThumbnailNotFound_ReturnArrayIncludingAssetWithNoThumbnail()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            FolderRepository folderRepository = (FolderRepository)mock.Container.Resolve<IFolderRepository>();
            AssetRepository assetRepository = (AssetRepository)mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var app = mock.Container.Resolve<Application.Application>();

            Folder folder = folderRepository.AddFolder(_dataDirectory);
            Mock<IAssetHashCalculatorService> hashCalculator = mock.Mock<IAssetHashCalculatorService>();
            hashCalculator
                .Setup(hc => hc.CalculateHash(It.IsAny<byte[]>()))
                .Returns("0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078");

            string imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset anotherAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 2 duplicated.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2 duplicated.jpg");
            
            assetRepository.DeleteThumbnail(duplicatedAsset.ThumbnailBlobName);
            
            var assets = app.GetAssets(_dataDirectory, 0);
            assets.Items.Should().NotBeEmpty();

            assetRepository.GetAssets(folder, 0).Items.Should().Contain(a => a.FileName == "Image 2.jpg");
            assetRepository.GetAssets(folder, 0).Items.Should().NotContain(a => a.FileName == "Image 2 duplicated.jpg");
            assetRepository.ContainsThumbnail(folder, "Image 2.jpg").Should().BeTrue();
            assetRepository.ContainsThumbnail(folder, "Image 2 duplicated.jpg").Should().BeFalse();
            assetRepository.LoadThumbnail(folder, asset.FileName, asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight).Should().NotBeNull();
            assetRepository.LoadThumbnail(folder, duplicatedAsset.FileName, duplicatedAsset.ThumbnailPixelWidth, duplicatedAsset.ThumbnailPixelHeight).Should().BeNull();
        }

        public void Dispose()
        {
            _connection.Dispose();
        }
    }
}
