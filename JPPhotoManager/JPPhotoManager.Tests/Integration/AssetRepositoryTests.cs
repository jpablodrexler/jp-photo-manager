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
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests.Integration
{
    public class AssetRepositoryTests
    {
        private readonly string _dataDirectory;
        private readonly IConfigurationRoot _configuration;
        private readonly SqliteConnection _connection;
        private readonly DbContextOptions<AppDbContext> _contextOptions;
        private readonly AppDbContext _dbContext;

        public AssetRepositoryTests()
        {
            _dataDirectory = Path.GetDirectoryName(typeof(AssetRepositoryTests).Assembly.Location);
            _dataDirectory = Path.Combine(_dataDirectory, "TestFiles");

            Mock<IConfigurationRoot> configurationMock = new();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", _dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", Path.Combine(_dataDirectory, "ApplicationData", Guid.NewGuid().ToString()))
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:BackupsToKeep", "2")
                .MockGetValue("appsettings:BackupEveryNDays", "7")
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
        public void FolderExists_DataDirectory_ReturnTrue()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterInstance(new AppDbContext(_contextOptions));
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                });
            var repository = mock.Create<IAssetRepository>();
            bool folderExists = repository.FolderExists(_dataDirectory);
            folderExists.Should().BeFalse();
            repository.AddFolder(_dataDirectory);
            folderExists = repository.FolderExists(_dataDirectory);
            folderExists.Should().BeTrue();
        }

        [Fact]
        public void IsAssetCatalogued_AssetNotInCatalog_ReturnFalse()
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
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            string imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();

            repository.IsAssetCatalogued(_dataDirectory, "Image 2.jpg").Should().BeFalse();
        }

        [Fact]
        public void IsAssetCatalogued_AssetInCatalog_ReturnTrue()
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
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            string imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();

            repository.IsAssetCatalogued(_dataDirectory, "Image 2.jpg").Should().BeFalse();
            repository.AddFolder(_dataDirectory);
            catalogAssetsService.CreateAsset(_dataDirectory, "Image 2.jpg");
            repository.IsAssetCatalogued(_dataDirectory, "Image 2.jpg").Should().BeTrue();
        }

        [Fact]
        public void IsAssetCatalogued_DeleteNonExistingAsset_ReturnFalse()
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
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();

            repository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, "Non Existing Image.jpg");
            File.Exists(imagePath).Should().BeFalse();

            repository.DeleteAsset(_dataDirectory, "Non Existing Image.jpg");
            repository.IsAssetCatalogued(_dataDirectory, "Non Existing Image.jpg").Should().BeFalse();

            repository.DeleteAsset(_dataDirectory, "Non Existing Image.jpg");
            repository.IsAssetCatalogued(_dataDirectory, "Non Existing Image.jpg").Should().BeFalse();
        }

        [Fact]
        public void IsAssetCatalogued_DeleteExistingAsset_ReturnFalse()
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
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            repository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, "Image 3.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 3.jpg");

            // The asset should no longer be catalogued, but the image should still be in the filesystem.
            repository.DeleteAsset(_dataDirectory, "Image 3.jpg");
            repository.IsAssetCatalogued(_dataDirectory, "Image 3.jpg").Should().BeFalse();
            File.Exists(imagePath).Should().BeTrue();
        }

        [Fact]
        public void GetSyncAssetsConfiguration_ReturnArray()
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
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyFirstGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MyFirstGame"
                });

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MySecondGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MySecondGame"
                });

            repository.SaveSyncAssetsConfiguration(syncConfiguration);
            
            syncConfiguration = repository.GetSyncAssetsConfiguration();

            syncConfiguration.Definitions.Should().HaveCount(2);
            syncConfiguration.Definitions[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            syncConfiguration.Definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            syncConfiguration.Definitions[1].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            syncConfiguration.Definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
        }
    }
}
