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
using Xunit;

namespace JPPhotoManager.Tests
{
    public class ApplicationTest
    {
        private string dataDirectory;
        private IConfigurationRoot configuration;

        public ApplicationTest()
        {
            dataDirectory = Path.GetDirectoryName(typeof(AssetRepositoryTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");

            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", Path.Combine(dataDirectory, Guid.NewGuid().ToString()))
                .MockGetValue("appsettings:CatalogBatchSize", "100");
                
            configuration = configurationMock.Object;
        }

        [Fact]
        public void GetAssets_ValidDirectory_ReturnAssetsArray()
        {
            string directory = @"D:\Imágenes";
            Asset[] expectedResult = new Asset[]
                {
                    new Asset
                    {
                        FileName = "dbzrrou-1d391dff-a336-4395-81a5-885a98685d93.jpg",
                        Folder = new Folder { Path = @"D:\Imágenes\" }
                    },
                    new Asset
                    {
                        FileName = "dbxb0an-d90d6335-7b9c-4a7b-84aa-71501c73f63b.jpg",
                        Folder = new Folder { Path = @"D:\Imágenes\" }
                    }
                };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IAssetRepository>().Setup(m => m.GetAssets(directory)).Returns(expectedResult);

                var app = mock.Container.Resolve<Application.Application>();

                Asset[] assets = app.GetAssets(directory);
                assets.Should().BeEquivalentTo(expectedResult);

                mock.Mock<IAssetRepository>().VerifyAll();
            }
        }

        [Theory]
        [InlineData(null)]
        [InlineData("")]
        [InlineData(" ")]
        public void GetAssets_InvalidDirectory_Test(string directory)
        {
            using (var mock = AutoMock.GetLoose())
            {
                var app = mock.Container.Resolve<Application.Application>();

                Func<Asset[]> function = () => app.GetAssets(directory);
                function.Should().Throw<ArgumentException>();
            }
        }

        // TODO: MOVE TO INTEGRATION TESTS PROJECT
        [Fact]
        public void GetDuplicatedAssets_WithDuplicates_ReturnArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            
            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<Database>().As<IDatabase>().SingleInstance();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                var database = mock.Container.Resolve<IDatabase>();
                var repository = mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
                var storageService = mock.Container.Resolve<IStorageService>();
                var app = mock.Container.Resolve<Application.Application>();

                Folder folder = repository.AddFolder(dataDirectory);

                string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
                Assert.True(File.Exists(imagePath));
                Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

                imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
                Assert.True(File.Exists(imagePath));
                Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

                imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
                Assert.True(File.Exists(imagePath));
                Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

                Console.WriteLine("database.DataDirectory: " + database.DataDirectory);
                Console.WriteLine("database.Separator: " + database.Separator);

                repository.SaveCatalog(folder);

                List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
                duplicatedAssetSets.Should().ContainSingle();

                DuplicatedAssetCollection duplicatedAssets = duplicatedAssetSets[0];
                duplicatedAssets.Should().HaveCount(2);
                duplicatedAssets[0].FileName.Should().Be("Image 2.jpg");
                duplicatedAssets[1].FileName.Should().Be("Image 2 duplicated.jpg");
                duplicatedAssets.Description.Should().Be("Image 2.jpg (2 duplicates)");

                repository.ContainsThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName).Should().BeTrue();
                repository.ContainsThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName).Should().BeTrue();
                repository.LoadThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName, duplicatedAssets[0].ThumbnailPixelWidth, duplicatedAssets[0].ThumbnailPixelHeight).Should().NotBeNull();
                repository.LoadThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName, duplicatedAssets[1].ThumbnailPixelWidth, duplicatedAssets[1].ThumbnailPixelHeight).Should().NotBeNull();
            }
        }

        // TODO: MOVE TO INTEGRATION TESTS PROJECT
        [Fact]
        public void GetDuplicatedAssets_WithoutDuplicates_ReturnEmptyArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<Database>().As<IDatabase>().SingleInstance();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                var repository = mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
                var app = mock.Container.Resolve<Application.Application>();

                repository.AddFolder(dataDirectory);

                string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

                imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

                List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
                duplicatedAssetSets.Should().BeEmpty();
            }
        }

        // TODO: MOVE TO INTEGRATION TESTS PROJECT
        /// <summary>
        /// Tests an scenario when the user searches for duplicates before an
        /// old entry gets deleted from the catalog.
        /// </summary>
        [Fact]
        public void GetDuplicatedAssets_WithInexistingDuplicatedAsset_ReturnEmptyArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<Database>().As<IDatabase>().SingleInstance();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                var repository = mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
                var app = mock.Container.Resolve<Application.Application>();

                repository.AddFolder(dataDirectory);

                string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

                imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

                imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
                File.Exists(imagePath).Should().BeFalse();

                Asset inexistentAsset = new Asset
                {
                    FileName = "Inexistent Image.jpg",
                    Folder = asset.Folder,
                    FolderId = asset.FolderId,
                    Hash = asset.Hash
                };

                repository.AddAsset(inexistentAsset, null);
                List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
                duplicatedAssetSets.Should().BeEmpty();
            }
        }

        // TODO: MOVE TO INTEGRATION TESTS PROJECT
        /// <summary>
        /// Tests an scenario when the user searches for duplicates before an
        /// old entry gets deleted from the catalog.
        /// </summary>
        [Fact]
        public void GetDuplicatedAssets_WithInexistingNotDuplicatedAsset_ReturnEmptyArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<Database>().As<IDatabase>().SingleInstance();
                    cfg.RegisterType<AssetHashCalculatorService>().As<IAssetHashCalculatorService>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                var repository = mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
                var app = mock.Container.Resolve<Application.Application>();

                Folder folder = repository.AddFolder(dataDirectory);

                string imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
                File.Exists(imagePath).Should().BeFalse();

                repository.AddAsset(new Asset
                {
                    FileName = "Inexistent Image.jpg",
                    Folder = folder,
                    FolderId = folder.FolderId,
                    Hash = "0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078"
                }, null);

                imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

                imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

                List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
                duplicatedAssetSets.Should().BeEmpty();
            }
        }

        // TODO: MOVE TO INTEGRATION TESTS PROJECT
        [Fact]
        public void GetDuplicatedAssets_WithDuplicatesHashCollisionWithDuplicated_ReturnArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<Database>().As<IDatabase>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                mock.Mock<IAssetHashCalculatorService>().Setup(h => h.CalculateHash(It.IsAny<byte[]>())).Returns("abcd1234");

                var repository = mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
                var app = mock.Container.Resolve<Application.Application>();

                Folder folder = repository.AddFolder(dataDirectory);

                string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

                imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

                imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

                repository.SaveCatalog(folder);

                List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
                duplicatedAssetSets.Should().ContainSingle();

                List<Asset> duplicatedAssets = duplicatedAssetSets[0];
                duplicatedAssets.Should().HaveCount(2);
                duplicatedAssets[0].FileName.Should().Be("Image 2.jpg");
                duplicatedAssets[1].FileName.Should().Be("Image 2 duplicated.jpg");

                repository.ContainsThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName).Should().BeTrue();
                repository.ContainsThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName).Should().BeTrue();
                repository.LoadThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName, duplicatedAssets[0].ThumbnailPixelWidth, duplicatedAssets[0].ThumbnailPixelHeight).Should().NotBeNull();
                repository.LoadThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName, duplicatedAssets[1].ThumbnailPixelWidth, duplicatedAssets[1].ThumbnailPixelHeight).Should().NotBeNull();
            }
        }

        // TODO: MOVE TO INTEGRATION TESTS PROJECT
        [Fact]
        public void GetDuplicatedAssets_WithDuplicatesHashCollisionWithNoDuplicated_ReturnEmptyArray()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<Database>().As<IDatabase>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                mock.Mock<IAssetHashCalculatorService>().Setup(h => h.CalculateHash(It.IsAny<byte[]>())).Returns("abcd1234");

                var repository = mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
                var app = mock.Container.Resolve<Application.Application>();

                repository.AddFolder(dataDirectory);

                string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

                imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

                List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
                duplicatedAssetSets.Should().BeEmpty();
            }
        }

        // TODO: MOVE TO INTEGRATION TESTS PROJECT
        [Fact]
        public void AddAssets_ToNonExistingFolder_AddFolderToCatalog()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<Database>().As<IDatabase>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<AssetRepository>().As<IAssetRepository>().SingleInstance();
                }))
            {
                var repository = mock.Container.Resolve<IAssetRepository>();

                Folder folder = new Folder { FolderId = "1", Path = "C:\\Inexistent Folder" };

                string imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
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
        }

        // TODO: MOVE TO INTEGRATION TESTS PROJECT
        [Fact]
        public void GetAssets_WithThumbnailNotFound_ReturnArrayIncludingAssetWithNoThumbnail()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using (var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<Database>().As<IDatabase>().SingleInstance();
                    cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                    cfg.RegisterType<StorageService>().As<IStorageService>().SingleInstance();
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                    cfg.RegisterType<UnencapsulatedAssetRepository>().As<IAssetRepository>().SingleInstance();
                    cfg.RegisterType<FindDuplicatedAssetsService>().As<IFindDuplicatedAssetsService>().SingleInstance();
                    cfg.RegisterType<CatalogAssetsService>().As<ICatalogAssetsService>().SingleInstance();
                }))
            {
                UnencapsulatedAssetRepository repository = (UnencapsulatedAssetRepository)mock.Container.Resolve<IAssetRepository>();
                var catalogAssetsService = mock.Container.Resolve<ICatalogAssetsService>();
                var app = mock.Container.Resolve<Application.Application>();

                Folder folder = repository.AddFolder(dataDirectory);
                Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();

                string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

                imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

                imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
                File.Exists(imagePath).Should().BeTrue();
                Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

                repository.SaveCatalog(folder);
                repository.RemoveThumbnail(folder.Path, "Image 2 duplicated.jpg");
                repository.SaveCatalog(folder);

                Asset[] assets = app.GetAssets(dataDirectory);
                assets.Should().NotBeEmpty();

                repository.GetAssets(dataDirectory).Should().Contain(a => a.FileName == "Image 2.jpg");
                repository.GetAssets(dataDirectory).Should().NotContain(a => a.FileName == "Image 2 duplicated.jpg");
                repository.ContainsThumbnail(dataDirectory, "Image 2.jpg").Should().BeTrue();
                repository.ContainsThumbnail(dataDirectory, "Image 2 duplicated.jpg").Should().BeFalse();
                repository.LoadThumbnail(dataDirectory, asset.FileName, asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight).Should().NotBeNull();
                repository.LoadThumbnail(dataDirectory, duplicatedAsset.FileName, duplicatedAsset.ThumbnailPixelWidth, duplicatedAsset.ThumbnailPixelHeight).Should().BeNull();
            }
        }
    }

    class UnencapsulatedAssetRepository : AssetRepository
    {
        public UnencapsulatedAssetRepository(IDatabase database, IStorageService storageService, IUserConfigurationService userConfigurationService) : base(database, storageService, userConfigurationService)
        {

        }

        internal void RemoveThumbnail(string directoryName, string fileName)
        {
            if (Thumbnails.ContainsKey(directoryName) && Thumbnails[directoryName].ContainsKey(fileName))
            {
                Thumbnails[directoryName].Remove(fileName);
            }
        }
    }
}
