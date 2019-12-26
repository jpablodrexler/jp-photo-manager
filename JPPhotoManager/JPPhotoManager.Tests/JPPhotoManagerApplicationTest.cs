using System;
using System.Collections.Generic;
using System.IO;
using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Moq;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class JPPhotoManagerApplicationTest
    {
        private string dataDirectory;
        private string assetDataSetPath;
        private IConfigurationRoot configuration;

        public JPPhotoManagerApplicationTest()
        {
            dataDirectory = Path.GetDirectoryName(typeof(AssetRepositoryTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");
            assetDataSetPath = Path.Combine(dataDirectory, $"AssetCatalog{Guid.NewGuid()}.json");

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
        }

        [Fact]
        public void GetImagesTest()
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

            Mock<IAssetRepository> mockRepository = new Mock<IAssetRepository>();
            mockRepository.Setup(m => m.GetAssets(directory)).Returns(expectedResult);

            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                new CatalogAssetsService(
                    mockRepository.Object,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer()),
                new FindDuplicatedAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService)),
                mockRepository.Object,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));
            Asset[] assets = app.GetAssets(directory);
            Assert.Equal(expectedResult, assets);

            mockRepository.VerifyAll();
        }

        [Fact]
        public void GetImagesOnEmptyStringTest()
        {
            string directory = string.Empty;
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            Mock<IAssetRepository> mockRepository = new Mock<IAssetRepository>();
            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                new CatalogAssetsService(
                    mockRepository.Object,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer()),
                new FindDuplicatedAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService)),
                mockRepository.Object,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));
            Assert.Throws<ArgumentException>(() =>
                app.GetAssets(directory));
        }

        [Fact]
        public void GetImagesOnNullStringTest()
        {
            string directory = null;
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            Mock<IAssetRepository> mockRepository = new Mock<IAssetRepository>();
            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                new CatalogAssetsService(
                    mockRepository.Object,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer()),
                new FindDuplicatedAssetsService(
                    mockRepository.Object, new StorageService(userConfigurationService)),
                mockRepository.Object,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));
            Assert.Throws<ArgumentException>(() =>
                app.GetAssets(directory));
        }

        [Fact]
        public void GetDuplicatedAssetsWithDuplicatesTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            Folder folder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            Assert.True(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

            repository.SaveCatalog(folder);

            repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.Single(duplicatedAssetSets);

            List<Asset> duplicatedAssets = duplicatedAssetSets[0];
            Assert.Equal(2, duplicatedAssets.Count);
            Assert.Equal("Image 2.jpg", duplicatedAssets[0].FileName);
            Assert.Equal("Image 2 duplicated.jpg", duplicatedAssets[1].FileName);

            Assert.True(repository.ContainsThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName));
            Assert.True(repository.ContainsThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName));
            Assert.NotNull(repository.LoadThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName));
            Assert.NotNull(repository.LoadThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName));
        }

        [Fact]
        public void GetDuplicatedAssetsWithoutDuplicatesTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.Empty(duplicatedAssetSets);
        }

        /// <summary>
        /// Tests an scenario when the user searches for duplicates before an
        /// old entry gets deleted from the catalog.
        /// </summary>
        [Fact]
        public void GetDuplicatedAssetsWithInexistingImageTest1()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
            Assert.False(File.Exists(imagePath));

            Asset inexistentAsset = new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = asset.Folder,
                FolderId = asset.FolderId,
                Hash = asset.Hash
            };

            repository.AddAsset(inexistentAsset, null);
            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.Empty(duplicatedAssetSets);
        }

        /// <summary>
        /// Tests an scenario when the user searches for duplicates before an
        /// old entry gets deleted from the catalog.
        /// </summary>
        [Fact]
        public void GetDuplicatedAssetsWithInexistingImageTest2()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            Folder folder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
            Assert.False(File.Exists(imagePath));

            repository.AddAsset(new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = folder,
                FolderId = folder.FolderId,
                Hash = "0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078"
            }, null);

            imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.Empty(duplicatedAssetSets);
        }

        [Fact]
        public void GetDuplicatedAssetsWithDuplicatesHashCollisionWithDuplicatedTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            Folder folder = repository.AddFolder(dataDirectory);

            Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();
            hashCalculator.Setup(h => h.CalculateHash(It.IsAny<byte[]>())).Returns("abcd1234");

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    hashCalculator.Object,
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                userConfigurationService,
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            Assert.True(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

            repository.SaveCatalog(folder);

            repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.Single(duplicatedAssetSets);

            List<Asset> duplicatedAssets = duplicatedAssetSets[0];
            Assert.Equal(2, duplicatedAssets.Count);
            Assert.Equal("Image 2.jpg", duplicatedAssets[0].FileName);
            Assert.Equal("Image 2 duplicated.jpg", duplicatedAssets[1].FileName);

            Assert.True(repository.ContainsThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName));
            Assert.True(repository.ContainsThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName));
            Assert.NotNull(repository.LoadThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName));
            Assert.NotNull(repository.LoadThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName));
        }

        [Fact]
        public void GetDuplicatedAssetsWithDuplicatesHashCollisionWithNoDuplicatedTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();
            hashCalculator.Setup(h => h.CalculateHash(It.IsAny<byte[]>())).Returns("abcd1234");

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    hashCalculator.Object,
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.Empty(duplicatedAssetSets);
        }

        [Fact]
        public void AddAssetsToNonExistingFolderAddsFolderToCatalogTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            Folder folder = new Folder { FolderId = "1", Path = "C:\\Inexistent Folder" };

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
            Assert.False(File.Exists(imagePath));

            repository.AddAsset(new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = folder,
                FolderId = folder.FolderId,
                Hash = "0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078"
            }, null);

            folder = repository.GetFolderByPath("C:\\Inexistent Folder");
            Assert.NotNull(folder);
            Assert.Equal("C:\\Inexistent Folder", folder.Path);
            Assert.Equal("Inexistent Folder", folder.Name);
        }
    }
}
