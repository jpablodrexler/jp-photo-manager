using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Moq;
using SimplePortableDatabase;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests.Integration
{
    public class AssetRepositoryTests
    {
        private readonly string dataDirectory;
        private readonly IConfigurationRoot configuration;

        public AssetRepositoryTests()
        {
            dataDirectory = Path.GetDirectoryName(typeof(AssetRepositoryTests).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");

            Mock<IConfigurationRoot> configurationMock = new();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", Path.Combine(dataDirectory, "ApplicationData", Guid.NewGuid().ToString()))
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:BackupsToKeep", "2")
                .MockGetValue("appsettings:BackupEveryNDays", "7")
                .MockGetValue("appsettings:ThumbnailsDictionaryEntriesToKeep", "5");

            configuration = configurationMock.Object;
        }

        [Fact]
        public void FolderExists_DataDirectory_ReturnTrue()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                });
            var repository = mock.Create<IAssetRepository>();
            bool folderExists = repository.FolderExists(dataDirectory);
            folderExists.Should().BeFalse();
            repository.AddFolder(dataDirectory);
            folderExists = repository.FolderExists(dataDirectory);
            folderExists.Should().BeTrue();
        }

        [Fact]
        public void HasChanges_Initial_ReturnFalse()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                });
            var repository = mock.Create<IAssetRepository>();
            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            repository.HasChanges().Should().BeFalse();
        }

        [Fact]
        public void HasChanges_AfterChange_ReturnTrue()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Create<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            repository.AddFolder(dataDirectory);

            catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");
            repository.HasChanges().Should().BeTrue();
        }

        [Fact]
        public void HasChanges_AfterSave_ReturnFalse()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();

            repository.AddFolder(dataDirectory);

            catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");
            repository.SaveCatalog(null);
            repository.HasChanges().Should().BeFalse();
        }

        [Fact]
        public void IsAssetCatalogued_AssetNotInCatalog_ReturnFalse()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();

            repository.IsAssetCatalogued(dataDirectory, "Image 2.jpg").Should().BeFalse();
        }

        [Fact]
        public void IsAssetCatalogued_AssetInCatalog_ReturnTrue()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();

            repository.IsAssetCatalogued(dataDirectory, "Image 2.jpg").Should().BeFalse();
            repository.AddFolder(dataDirectory);
            catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");
            repository.IsAssetCatalogued(dataDirectory, "Image 2.jpg").Should().BeTrue();
        }

        [Fact]
        public void IsAssetCatalogued_DeleteNonExistingAsset_ReturnFalse()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();

            repository.AddFolder(dataDirectory);

            string imagePath = Path.Combine(dataDirectory, "Non Existing Image.jpg");
            File.Exists(imagePath).Should().BeFalse();

            repository.DeleteAsset(dataDirectory, "Non Existing Image.jpg");
            repository.IsAssetCatalogued(dataDirectory, "Non Existing Image.jpg").Should().BeFalse();

            repository.DeleteAsset(dataDirectory, "Non Existing Image.jpg");
            repository.IsAssetCatalogued(dataDirectory, "Non Existing Image.jpg").Should().BeFalse();
        }

        [Fact]
        public void IsAssetCatalogued_DeleteExistingAsset_ReturnFalse()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();

            repository.AddFolder(dataDirectory);

            string imagePath = Path.Combine(dataDirectory, "Image 3.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 3.jpg");

            // The asset should no longer be catalogued, but the image should still be in the filesystem.
            repository.DeleteAsset(dataDirectory, "Image 3.jpg");
            repository.IsAssetCatalogued(dataDirectory, "Image 3.jpg").Should().BeFalse();
            File.Exists(imagePath).Should().BeTrue();
        }

        [Fact]
        public void GetSyncAssetsConfiguration_ReturnArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
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
            repository.SaveCatalog(null);

            syncConfiguration = repository.GetSyncAssetsConfiguration();

            syncConfiguration.Definitions.Should().HaveCount(2);
            syncConfiguration.Definitions[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            syncConfiguration.Definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            syncConfiguration.Definitions[1].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            syncConfiguration.Definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
        }

        [Theory]
        [InlineData(true, false)]
        [InlineData(false, true)]
        public void ShouldWriteBackup_WhenNullArray_ReturnBackupExists(bool backupExists, bool expectedResult)
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            mock.Mock<IDatabase>()
                .Setup(m => m.GetBackupDates())
                .Returns((DateTime[])null);

            mock.Mock<IDatabase>()
                .Setup(m => m.BackupExists(It.IsAny<DateTime>()))
                .Returns(backupExists);

            var repository = mock.Container.Resolve<IAssetRepository>();
            var shouldWrite = repository.ShouldWriteBackup(new DateTime(2022, 7, 3));
            shouldWrite.Should().Be(expectedResult);
        }

        [Theory]
        [InlineData(true, false)]
        [InlineData(false, true)]
        public void ShouldWriteBackup_WhenEmptyArray_ReturnEmptyArray(bool backupExists, bool expectedResult)
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            
            mock.Mock<IDatabase>()
                .Setup(m => m.GetBackupDates())
                .Returns(Array.Empty<DateTime>());

            mock.Mock<IDatabase>()
                .Setup(m => m.BackupExists(It.IsAny<DateTime>()))
                .Returns(backupExists);

            var repository = mock.Container.Resolve<IAssetRepository>();
            var shouldWrite = repository.ShouldWriteBackup(new DateTime(2022, 7, 3));
            shouldWrite.Should().Be(expectedResult);
        }

        [Theory]
        [InlineData(true)]
        [InlineData(false)]
        public void ShouldWriteBackup_WhenOneItemArrayBeforeBackupDate_ReturnFalse(bool backupExists)
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            mock.Mock<IDatabase>()
                .Setup(m => m.GetBackupDates())
                .Returns(new DateTime[] { new DateTime(2022, 7, 1) });

            mock.Mock<IDatabase>()
                .Setup(m => m.BackupExists(It.IsAny<DateTime>()))
                .Returns(backupExists);

            var repository = mock.Container.Resolve<IAssetRepository>();
            var shouldWrite = repository.ShouldWriteBackup(new DateTime(2022, 7, 3));
            shouldWrite.Should().BeFalse();
        }

        [Theory]
        [InlineData(true, false)]
        [InlineData(false, true)]
        public void ShouldWriteBackup_WhenOneItemArrayOnBackupDate_ReturnBackupDoesntExists(bool backupExists, bool expectedResult)
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            mock.Mock<IDatabase>()
                .Setup(m => m.GetBackupDates())
                .Returns(new DateTime[] { new DateTime(2022, 7, 1) });

            mock.Mock<IDatabase>()
                .Setup(m => m.BackupExists(It.IsAny<DateTime>()))
                .Returns(backupExists);

            var repository = mock.Container.Resolve<IAssetRepository>();
            var shouldWrite = repository.ShouldWriteBackup(new DateTime(2022, 7, 8));
            shouldWrite.Should().Be(expectedResult);
        }

        [Theory]
        [InlineData(true, false)]
        [InlineData(false, true)]
        public void ShouldWriteBackup_WhenOneItemArrayAfterBackupDate_ReturnBackupDoesntExists(bool backupExists, bool expectedResult)
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            mock.Mock<IDatabase>()
                .Setup(m => m.GetBackupDates())
                .Returns(new DateTime[] { new DateTime(2022, 7, 1) });

            mock.Mock<IDatabase>()
                .Setup(m => m.BackupExists(It.IsAny<DateTime>()))
                .Returns(backupExists);

            var repository = mock.Container.Resolve<IAssetRepository>();
            var shouldWrite = repository.ShouldWriteBackup(new DateTime(2022, 7, 12));
            shouldWrite.Should().Be(expectedResult);
        }

        [Theory]
        [InlineData(true)]
        [InlineData(false)]
        public void ShouldWriteBackup_WhenMultipleItemsArrayBeforeBackupDate_ReturnFalse(bool backupExists)
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            mock.Mock<IDatabase>()
                .Setup(m => m.GetBackupDates())
                .Returns(new DateTime[]
                {
                    new DateTime(2022, 6, 3),
                    new DateTime(2022, 6, 10),
                    new DateTime(2022, 6, 17),
                    new DateTime(2022, 6, 24),
                    new DateTime(2022, 7, 1)
                });

            mock.Mock<IDatabase>()
                .Setup(m => m.BackupExists(It.IsAny<DateTime>()))
                .Returns(backupExists);

            var repository = mock.Container.Resolve<IAssetRepository>();
            var shouldWrite = repository.ShouldWriteBackup(new DateTime(2022, 7, 3));
            shouldWrite.Should().BeFalse();
        }

        [Theory]
        [InlineData(true, false)]
        [InlineData(false, true)]
        public void ShouldWriteBackup_WhenMultipleItemsArrayOnBackupDate_ReturnBackupExists(bool backupExists, bool expectedResult)
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            mock.Mock<IDatabase>()
                .Setup(m => m.GetBackupDates())
                .Returns(new DateTime[]
                {
                    new DateTime(2022, 6, 3),
                    new DateTime(2022, 6, 10),
                    new DateTime(2022, 6, 17),
                    new DateTime(2022, 6, 24),
                    new DateTime(2022, 7, 1)
                });

            mock.Mock<IDatabase>()
                .Setup(m => m.BackupExists(It.IsAny<DateTime>()))
                .Returns(backupExists);

            var repository = mock.Container.Resolve<IAssetRepository>();
            var shouldWrite = repository.ShouldWriteBackup(new DateTime(2022, 7, 8));
            shouldWrite.Should().Be(expectedResult);
        }

        [Theory]
        [InlineData(true, false)]
        [InlineData(false, true)]
        public void ShouldWriteBackup_WhenMultipleItemsArrayAfterBackupDate_ReturnBackupExists(bool backupExists, bool expectedResult)
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });

            mock.Mock<IDatabase>()
                .Setup(m => m.GetBackupDates())
                .Returns(new DateTime[]
                {
                    new DateTime(2022, 6, 3),
                    new DateTime(2022, 6, 10),
                    new DateTime(2022, 6, 17),
                    new DateTime(2022, 6, 24),
                    new DateTime(2022, 7, 1)
                });

            mock.Mock<IDatabase>()
                .Setup(m => m.BackupExists(It.IsAny<DateTime>()))
                .Returns(backupExists);

            var repository = mock.Container.Resolve<IAssetRepository>();
            var shouldWrite = repository.ShouldWriteBackup(new DateTime(2022, 7, 12));
            shouldWrite.Should().Be(expectedResult);
        }
    }
}
