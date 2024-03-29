﻿using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
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
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                });
            var repository = mock.Create<IFolderRepository>();
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
            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            var folder = folderRepository.AddFolder(_dataDirectory);
            string imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();

            assetRepository.IsAssetCatalogued(folder, "Image 2.jpg").Should().BeFalse();
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
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            string imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
           
            var folder = folderRepository.AddFolder(_dataDirectory);

            assetRepository.IsAssetCatalogued(folder, "Image 2.jpg").Should().BeFalse();
            catalogAssetsService.CreateAsset(_dataDirectory, "Image 2.jpg");
            assetRepository.IsAssetCatalogued(folder, "Image 2.jpg").Should().BeTrue();
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
            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();

            var folder = folderRepository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, "Non Existing Image.jpg");
            File.Exists(imagePath).Should().BeFalse();

            assetRepository.DeleteAsset(folder, "Non Existing Image.jpg");
            assetRepository.IsAssetCatalogued(folder, "Non Existing Image.jpg").Should().BeFalse();

            assetRepository.DeleteAsset(folder, "Non Existing Image.jpg");
            assetRepository.IsAssetCatalogued(folder, "Non Existing Image.jpg").Should().BeFalse();
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
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            var folderRepository = mock.Container.Resolve<IFolderRepository>();
            var assetRepository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            var folder = folderRepository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, "Image 3.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 3.jpg");

            // The asset should no longer be catalogued, but the image should still be in the filesystem.
            assetRepository.DeleteAsset(folder, "Image 3.jpg");
            assetRepository.IsAssetCatalogued(folder, "Image 3.jpg").Should().BeFalse();
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
                    cfg.RegisterType<FolderRepository>().As<IFolderRepository>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<SyncAssetsConfigurationRepository>().As<ISyncAssetsConfigurationRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            var repository = mock.Container.Resolve<ISyncAssetsConfigurationRepository>();

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
            syncConfiguration.Definitions.Any(d => d.SourceDirectory == @"C:\MyFirstGame\Screenshots").Should().BeTrue();
            syncConfiguration.Definitions.Any(d => d.DestinationDirectory == @"C:\Images\MyFirstGame").Should().BeTrue();
            syncConfiguration.Definitions.Any(d => d.SourceDirectory == @"C:\MySecondGame\Screenshots").Should().BeTrue();
            syncConfiguration.Definitions.Any(d => d.DestinationDirectory == @"C:\Images\MySecondGame").Should().BeTrue();
        }
    }
}
