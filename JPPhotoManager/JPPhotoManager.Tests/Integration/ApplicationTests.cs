﻿using Autofac;
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
    public class ApplicationTests
    {
        private string _dataDirectory;
        private IConfigurationRoot _configuration;

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
        }

        [Fact]
        public void GetDuplicatedAssets_WithDuplicates_ReturnArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<SpdbAssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var database = mock.Container.Resolve<IDatabase>();
            var repository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var storageService = mock.Container.Resolve<IStorageService>();
            var app = mock.Container.Resolve<Application.Application>();

            Folder folder = repository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 2 duplicated.jpg");
            Assert.True(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2 duplicated.jpg");

            Console.WriteLine("database.DataDirectory: " + database.DataDirectory);
            Console.WriteLine("database.Separator: " + database.Separator);

            repository.SaveCatalog(folder);

            var duplicatedAssetSets = app.GetDuplicatedAssets();
            duplicatedAssetSets.Should().ContainSingle();

            var duplicatedAssets = duplicatedAssetSets[0];
            duplicatedAssets.Should().HaveCount(2);
            duplicatedAssets[0].FileName.Should().Be("Image 2.jpg");
            duplicatedAssets[1].FileName.Should().Be("Image 2 duplicated.jpg");

            repository.ContainsThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName).Should().BeTrue();
            repository.ContainsThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName).Should().BeTrue();
            repository.LoadThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName, duplicatedAssets[0].ThumbnailPixelWidth, duplicatedAssets[0].ThumbnailPixelHeight).Should().NotBeNull();
            repository.LoadThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName, duplicatedAssets[1].ThumbnailPixelWidth, duplicatedAssets[1].ThumbnailPixelHeight).Should().NotBeNull();
        }

        [Fact]
        public void GetDuplicatedAssets_WithoutDuplicates_ReturnEmptyArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<SpdbAssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();
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
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<SpdbAssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var app = mock.Container.Resolve<Application.Application>();

            repository.AddFolder(_dataDirectory);

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

            repository.AddAsset(inexistentAsset, null);
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
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<SpdbAssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var app = mock.Container.Resolve<Application.Application>();

            Folder folder = repository.AddFolder(_dataDirectory);

            string imagePath = Path.Combine(_dataDirectory, "Inexistent Image.jpg");
            File.Exists(imagePath).Should().BeFalse();

            repository.AddAsset(new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = folder,
                FolderId = folder.FolderId,
                Hash = "0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078"
            }, null);

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
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<SpdbAssetRepository>().As<IAssetRepository>().SingleInstance();
                });
            var repository = mock.Container.Resolve<IAssetRepository>();

            Folder folder = new() { FolderId = "1", Path = "C:\\Inexistent Folder" };

            string imagePath = Path.Combine(_dataDirectory, "Inexistent Image.jpg");
            File.Exists(imagePath).Should().BeFalse();

            repository.AddAsset(new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = folder,
                FolderId = folder.FolderId,
                Hash = "0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078"
            }, null);

            folder = repository.GetFolderByPath("C:\\Inexistent Folder");
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
                    cfg.RegisterSimplePortableDatabaseTypes();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<UnencapsulatedAssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                });
            UnencapsulatedAssetRepository repository = (UnencapsulatedAssetRepository)mock.Container.Resolve<IAssetRepository>();
            var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
            var app = mock.Container.Resolve<Application.Application>();

            Folder folder = repository.AddFolder(_dataDirectory);
            Mock<IAssetHashCalculatorService> hashCalculator = new();

            string imagePath = Path.Combine(_dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset anotherAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(_dataDirectory, "Image 2 duplicated.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(_dataDirectory, "Image 2 duplicated.jpg");

            repository.SaveCatalog(folder);
            repository.RemoveThumbnail(folder.Path, "Image 2 duplicated.jpg");
            repository.SaveCatalog(folder);

            var assets = app.GetAssets(_dataDirectory, 0);
            assets.Items.Should().NotBeEmpty();

            repository.GetAssets(_dataDirectory, 0).Items.Should().Contain(a => a.FileName == "Image 2.jpg");
            repository.GetAssets(_dataDirectory, 0).Items.Should().NotContain(a => a.FileName == "Image 2 duplicated.jpg");
            repository.ContainsThumbnail(_dataDirectory, "Image 2.jpg").Should().BeTrue();
            repository.ContainsThumbnail(_dataDirectory, "Image 2 duplicated.jpg").Should().BeFalse();
            repository.LoadThumbnail(_dataDirectory, asset.FileName, asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight).Should().NotBeNull();
            repository.LoadThumbnail(_dataDirectory, duplicatedAsset.FileName, duplicatedAsset.ThumbnailPixelWidth, duplicatedAsset.ThumbnailPixelHeight).Should().BeNull();
        }
    }

    class UnencapsulatedAssetRepository : SpdbAssetRepository
    {
        public UnencapsulatedAssetRepository(IDatabase database, IStorageService storageService, IUserConfigurationService userConfigurationService)
            : base(database, storageService, userConfigurationService)
        {
        }

        internal void RemoveThumbnail(string directoryName, string fileName)
        {
            var assets = GetAssets(directoryName, 0);
            var asset = assets.Items.First(a => a.FileName == fileName);

            DeleteThumbnail(asset);
        }
    }
}
