using System;
using System.Collections.Generic;
using System.IO;
using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Moq;

namespace JPPhotoManager.Test
{
    [TestClass]
    public class JPPhotoManagerApplicationTest
    {
        private static string dataDirectory;
        private static string assetDataSetPath;
        private static IConfigurationRoot configuration;

        [ClassInitialize]
        public static void AssetRepositoryTestInitialize(TestContext testContext)
        {
            dataDirectory = Path.GetDirectoryName(typeof(AssetRepositoryTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");
            assetDataSetPath = Path.Combine(dataDirectory, "AssetCatalog.json");

            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", dataDirectory)
                .MockGetValue("appsettings:CatalogBatchSize", "100");

            configuration = configurationMock.Object;
        }

        [TestInitialize()]
        public void AssetRepositoryTestInitialize()
        {
            if (File.Exists(assetDataSetPath))
            {
                File.Delete(assetDataSetPath);
            }
        }

        [TestMethod]
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

            JPPhotoManagerApplication app = new JPPhotoManagerApplication(
                new CatalogAssetsService(
                    mockRepository.Object,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration)),
                new FindDuplicatedAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService)),
                mockRepository.Object,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));
            Asset[] assets = app.GetAssets(directory);
            Assert.AreEqual(expectedResult, assets);

            mockRepository.VerifyAll();
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentException))]
        public void GetImagesOnEmptyStringTest()
        {
            string directory = string.Empty;
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            Mock<IAssetRepository> mockRepository = new Mock<IAssetRepository>();
            JPPhotoManagerApplication app = new JPPhotoManagerApplication(
                new CatalogAssetsService(
                    mockRepository.Object,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration)),
                new FindDuplicatedAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService)),
                mockRepository.Object,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));
            Asset[] assets = app.GetAssets(directory);
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentException))]
        public void GetImagesOnNullStringTest()
        {
            string directory = null;
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            Mock<IAssetRepository> mockRepository = new Mock<IAssetRepository>();
            JPPhotoManagerApplication app = new JPPhotoManagerApplication(
                new CatalogAssetsService(
                    mockRepository.Object,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration)),
                new FindDuplicatedAssetsService(
                    mockRepository.Object, new StorageService(userConfigurationService)),
                mockRepository.Object,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));
            Asset[] assets = app.GetAssets(directory);
        }

        [TestMethod]
        public void GetDuplicatedAssetsWithDuplicatesTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
            Folder folder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            JPPhotoManagerApplication app = new JPPhotoManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

            repository.SaveCatalog(folder);

            repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
            repository.AddFolder(dataDirectory);

            catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            app = new JPPhotoManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.AreEqual(1, duplicatedAssetSets.Count);

            List<Asset> duplicatedAssets = duplicatedAssetSets[0];
            Assert.AreEqual(2, duplicatedAssets.Count);
            Assert.AreEqual("Image 2.jpg", duplicatedAssets[0].FileName);
            Assert.AreEqual("Image 2 duplicated.jpg", duplicatedAssets[1].FileName);

            Assert.IsTrue(repository.ContainsThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName));
            Assert.IsTrue(repository.ContainsThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName));
            Assert.IsNotNull(repository.LoadThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName));
            Assert.IsNotNull(repository.LoadThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName));
        }

        [TestMethod]
        public void GetDuplicatedAssetsWithoutDuplicatesTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            JPPhotoManagerApplication app = new JPPhotoManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.AreEqual(0, duplicatedAssetSets.Count);
        }

        /// <summary>
        /// Tests an scenario when the user searches for duplicates before an
        /// old entry gets deleted from the catalog.
        /// </summary>
        [TestMethod]
        public void GetDuplicatedAssetsWithInexistingImageTest1()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            JPPhotoManagerApplication app = new JPPhotoManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
            Assert.IsFalse(File.Exists(imagePath));

            Asset inexistentAsset = new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = asset.Folder,
                FolderId = asset.FolderId,
                Hash = asset.Hash
            };

            repository.AddAsset(inexistentAsset, null);
            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.AreEqual(0, duplicatedAssetSets.Count);
        }

        /// <summary>
        /// Tests an scenario when the user searches for duplicates before an
        /// old entry gets deleted from the catalog.
        /// </summary>
        [TestMethod]
        public void GetDuplicatedAssetsWithInexistingImageTest2()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
            Folder folder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            JPPhotoManagerApplication app = new JPPhotoManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
            Assert.IsFalse(File.Exists(imagePath));

            repository.AddAsset(new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = folder,
                FolderId = folder.FolderId,
                Hash = "0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078"
            }, null);

            imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.AreEqual(0, duplicatedAssetSets.Count);
        }

        [TestMethod]
        public void GetDuplicatedAssetsWithDuplicatesHashCollisionWithDuplicatedTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
            Folder folder = repository.AddFolder(dataDirectory);

            Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();
            hashCalculator.Setup(h => h.CalculateHash(It.IsAny<byte[]>())).Returns("abcd1234");

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    hashCalculator.Object,
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            JPPhotoManagerApplication app = new JPPhotoManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                userConfigurationService,
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

            repository.SaveCatalog(folder);

            repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
            repository.AddFolder(dataDirectory);

            catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            app = new JPPhotoManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.AreEqual(1, duplicatedAssetSets.Count);

            List<Asset> duplicatedAssets = duplicatedAssetSets[0];
            Assert.AreEqual(2, duplicatedAssets.Count);
            Assert.AreEqual("Image 2.jpg", duplicatedAssets[0].FileName);
            Assert.AreEqual("Image 2 duplicated.jpg", duplicatedAssets[1].FileName);

            Assert.IsTrue(repository.ContainsThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName));
            Assert.IsTrue(repository.ContainsThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName));
            Assert.IsNotNull(repository.LoadThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName));
            Assert.IsNotNull(repository.LoadThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName));
        }

        [TestMethod]
        public void GetDuplicatedAssetsWithDuplicatesHashCollisionWithNoDuplicatedTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
            repository.AddFolder(dataDirectory);

            Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();
            hashCalculator.Setup(h => h.CalculateHash(It.IsAny<byte[]>())).Returns("abcd1234");

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    hashCalculator.Object,
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            JPPhotoManagerApplication app = new JPPhotoManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.AreEqual(0, duplicatedAssetSets.Count);
        }
    }
}
