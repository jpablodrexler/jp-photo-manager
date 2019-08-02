using Microsoft.VisualStudio.TestTools.UnitTesting;
using AssetManager.Domain;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using AssetManager.Infrastructure;
using System.IO;

namespace AssetManager.Test
{
    [TestClass]
    public class CatalogAssetsServiceTest
    {
        private static string dataDirectory;
        private static string assetDataSetPath;

        [ClassInitialize]
        public static void AssetRepositoryTestInitialize(TestContext testContext)
        {
            dataDirectory = Path.GetDirectoryName(typeof(CatalogAssetsServiceTest).Assembly.Location);
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
        public void CreateThumbnailTest1()
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

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            Assert.AreEqual("Image 2.jpg", asset.FileName);
            Assert.AreEqual(30197, asset.FileSize);
            Assert.AreEqual(dataDirectory, asset.Folder.Path);
            Assert.AreEqual(imagePath, asset.FullPath);
            Assert.AreEqual(720, asset.PixelHeight);
            Assert.AreEqual(1280, asset.PixelWidth);
            Assert.AreEqual("0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078", asset.Hash);
            Assert.AreNotEqual(DateTime.MinValue, asset.ThumbnailCreationDateTime);
        }

        [TestMethod]
        public void CreateThumbnailTest2()
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

            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            Assert.AreEqual("Image 1.jpg", asset.FileName);
            Assert.AreEqual(29857, asset.FileSize);
            Assert.AreEqual(dataDirectory, asset.Folder.Path);
            Assert.AreEqual(imagePath, asset.FullPath);
            Assert.AreEqual(720, asset.PixelHeight);
            Assert.AreEqual(1280, asset.PixelWidth);
            Assert.AreEqual("1fafae17c3c5c38d1205449eebdb9f5976814a5e54ec5797270c8ec467fe6d6d1190255cbaac11d9057c4b2697d90bc7116a46ed90c5ffb71e32e569c3b47fb9", asset.Hash);
            Assert.AreNotEqual(DateTime.MinValue, asset.ThumbnailCreationDateTime);
        }

        [TestMethod]
        public void CreateThumbnailOfDuplicatedFilesCompareHashesTest()
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

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            Assert.AreNotSame(asset.FileName, duplicatedAsset.FileName);
            Assert.AreEqual(asset.Hash, duplicatedAsset.Hash);
        }

        [TestMethod]
        public void CreateThumbnailOfDifferentFilesCompareHashesTest()
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

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateThumbnail(thumbnails, imagePath);

            Assert.AreNotSame(asset.FileName, duplicatedAsset.FileName);
            Assert.AreNotSame(asset.Hash, duplicatedAsset.Hash);
        }
    }
}