﻿using System;
using System.Collections.Generic;
using System.IO;
using AssetManager.Application;
using AssetManager.Domain;
using AssetManager.Infrastructure;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Moq;

namespace AssetManager.Test
{
    [TestClass]
    public class AssetManagerApplicationTest
    {
        private static string dataDirectory;
        private static string assetDataSetPath;

        [ClassInitialize]
        public static void AssetRepositoryTestInitialize(TestContext testContext)
        {
            dataDirectory = Path.GetDirectoryName(typeof(AssetRepositoryTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");
            assetDataSetPath = Path.Combine(dataDirectory, "AssetCatalog.json");
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

            UserConfigurationService userConfigurationService = new UserConfigurationService();

            AssetManagerApplication app = new AssetManagerApplication(
                new CatalogAssetsService(
                    mockRepository.Object,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService()),
                new FindDuplicatedAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService)),
                mockRepository.Object,
                new UserConfigurationService(),
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
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            Mock<IAssetRepository> mockRepository = new Mock<IAssetRepository>();
            AssetManagerApplication app = new AssetManagerApplication(
                new CatalogAssetsService(
                    mockRepository.Object,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService()),
                new FindDuplicatedAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService)),
                mockRepository.Object,
                new UserConfigurationService(),
                new StorageService(userConfigurationService));
            Asset[] assets = app.GetAssets(directory);
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentException))]
        public void GetImagesOnNullStringTest()
        {
            string directory = null;
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            Mock<IAssetRepository> mockRepository = new Mock<IAssetRepository>();
            AssetManagerApplication app = new AssetManagerApplication(
                new CatalogAssetsService(
                    mockRepository.Object,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService()),
                new FindDuplicatedAssetsService(
                    mockRepository.Object, new StorageService(userConfigurationService)),
                mockRepository.Object,
                new UserConfigurationService(),
                new StorageService(userConfigurationService));
            Asset[] assets = app.GetAssets(directory);
        }

        [TestMethod]
        public void GetDuplicatedAssetsWithDuplicatesTest()
        {
            Dictionary<string, byte[]> thumbnails = new Dictionary<string, byte[]>();
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            MockAssetRepository repository = new MockAssetRepository(thumbnails, new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService());

            AssetManagerApplication app = new AssetManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.AreEqual(1, duplicatedAssetSets.Count);

            List<Asset> duplicatedAssets = duplicatedAssetSets[0];
            Assert.AreEqual(2, duplicatedAssets.Count);
            Assert.AreEqual("Image 2.jpg", duplicatedAssets[0].FileName);
            Assert.AreEqual("Image 2 duplicated.jpg", duplicatedAssets[1].FileName);
        }

        [TestMethod]
        public void GetDuplicatedAssetsWithoutDuplicatesTest()
        {
            Dictionary<string, byte[]> thumbnails = new Dictionary<string, byte[]>();
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService());

            AssetManagerApplication app = new AssetManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

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
            Dictionary<string, byte[]> thumbnails = new Dictionary<string, byte[]>();
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            MockAssetRepository repository = new MockAssetRepository(thumbnails, new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService());

            AssetManagerApplication app = new AssetManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
            Assert.IsFalse(File.Exists(imagePath));

            Asset inexistentAsset = new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = asset.Folder,
                FolderId = asset.FolderId,
                Hash = asset.Hash
            };

            repository.AddFakeAsset(inexistentAsset);
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
            Dictionary<string, byte[]> thumbnails = new Dictionary<string, byte[]>();
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            MockAssetRepository repository = new MockAssetRepository(thumbnails, new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);
            Folder folder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService());

            AssetManagerApplication app = new AssetManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
            Assert.IsFalse(File.Exists(imagePath));

            repository.AddFakeAsset(new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = folder,
                FolderId = folder.FolderId,
                Hash = "0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078"
            });

            imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.AreEqual(0, duplicatedAssetSets.Count);
        }

        [TestMethod]
        public void GetDuplicatedAssetsWithDuplicatesHashCollisionWithDuplicatedTest()
        {
            Dictionary<string, byte[]> thumbnails = new Dictionary<string, byte[]>();
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            MockAssetRepository repository = new MockAssetRepository(thumbnails, new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService());

            AssetManagerApplication app = new AssetManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                userConfigurationService,
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.AreEqual(1, duplicatedAssetSets.Count);

            List<Asset> duplicatedAssets = duplicatedAssetSets[0];
            Assert.AreEqual(2, duplicatedAssets.Count);
            Assert.AreEqual("Image 2.jpg", duplicatedAssets[0].FileName);
            Assert.AreEqual("Image 2 duplicated.jpg", duplicatedAssets[1].FileName);
        }

        [TestMethod]
        public void GetDuplicatedAssetsWithDuplicatesHashCollisionWithNoDuplicatedTest()
        {
            Dictionary<string, byte[]> thumbnails = new Dictionary<string, byte[]>();
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService());

            AssetManagerApplication app = new AssetManagerApplication(
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            Assert.AreEqual(0, duplicatedAssetSets.Count);
        }
    }
}
