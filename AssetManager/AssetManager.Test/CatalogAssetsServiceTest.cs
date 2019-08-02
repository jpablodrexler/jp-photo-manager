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
        private static string imageDestinationDirectory;

        [ClassInitialize]
        public static void CatalogAssetsServiceTestInitialize(TestContext testContext)
        {
            dataDirectory = Path.GetDirectoryName(typeof(CatalogAssetsServiceTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");
            assetDataSetPath = Path.Combine(dataDirectory, "AssetCatalog.json");
            imageDestinationDirectory = Path.Combine(dataDirectory, "NewFolder");
        }

        [TestInitialize()]
        public void CatalogAssetsServiceTestInitialize()
        {
            if (File.Exists(assetDataSetPath))
            {
                File.Delete(assetDataSetPath);
            }

            if (Directory.Exists(imageDestinationDirectory))
            {
                Directory.Delete(imageDestinationDirectory, true);
            }
        }

        [TestMethod]
        public void CreateAssetTest1()
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
            Asset asset = catalogAssetsService.CreateAsset(thumbnails, dataDirectory, "Image 2.jpg");

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
        public void CreateAssetTest2()
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
            Asset asset = catalogAssetsService.CreateAsset(thumbnails, dataDirectory, "Image 1.jpg");

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
        public void CreateAssetOfDuplicatedFilesCompareHashesTest()
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
            Asset asset = catalogAssetsService.CreateAsset(thumbnails, dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(thumbnails, dataDirectory, "Image 2 duplicated.jpg");

            Assert.AreNotSame(asset.FileName, duplicatedAsset.FileName);
            Assert.AreEqual(asset.Hash, duplicatedAsset.Hash);
        }

        [TestMethod]
        public void CreateAssetOfDifferentFilesCompareHashesTest()
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
            Asset asset = catalogAssetsService.CreateAsset(thumbnails, dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(thumbnails, dataDirectory, "Image 1.jpg");

            Assert.AreNotSame(asset.FileName, duplicatedAsset.FileName);
            Assert.AreNotSame(asset.Hash, duplicatedAsset.Hash);
        }

        [TestMethod]
        public void MoveExistingAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            var thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out bool isNewFile);
            var thumbnailsDestinationFolder = repository.GetThumbnails(destinationFolder.ThumbnailsFilename, out isNewFile);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 4.jpg");
            string destinationImagePath = Path.Combine(imageDestinationDirectory, "Image 4.jpg");
            Assert.IsTrue(File.Exists(sourceImagePath));
            Assert.IsFalse(File.Exists(destinationImagePath));

            Asset asset = catalogAssetsService.CreateAsset(thumbnailsSourceFolder, dataDirectory, "Image 4.jpg");
            repository.SaveCatalog(thumbnailsSourceFolder, sourceFolder.ThumbnailsFilename);
            repository.SaveCatalog(thumbnailsDestinationFolder, destinationFolder.ThumbnailsFilename);

            Assert.IsTrue(thumbnailsSourceFolder.ContainsKey(asset.FileName));
            Assert.IsFalse(thumbnailsDestinationFolder.ContainsKey(asset.FileName));

            bool result = catalogAssetsService.MoveAsset(asset, sourceFolder, destinationFolder, preserveOriginalFile: false);

            thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out isNewFile);
            thumbnailsDestinationFolder = repository.GetThumbnails(destinationFolder.ThumbnailsFilename, out isNewFile);

            Assert.IsTrue(result);
            Assert.IsFalse(File.Exists(sourceImagePath));
            Assert.IsTrue(File.Exists(destinationImagePath));

            Assert.IsFalse(thumbnailsSourceFolder.ContainsKey(asset.FileName));
            Assert.IsTrue(thumbnailsDestinationFolder.ContainsKey(asset.FileName));

            // Validates if the catalogued assets for the source folder are updated properly.
            var assets = repository.GetCataloguedAssets(sourceFolder.Path);
            int count = assets.Count(a => a.FileName == "Image 4.jpg");
            Assert.AreEqual(0, count);

            // Validates if the catalogued assets for the destination folder are updated properly.
            assets = repository.GetCataloguedAssets(destinationFolder.Path);
            count = assets.Count(a => a.FileName == "Image 4.jpg");
            Assert.AreEqual(1, count);
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentException))]
        public void MoveNonExistingAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            var thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out bool isNewFile);
            var thumbnailsDestinationFolder = repository.GetThumbnails(destinationFolder.ThumbnailsFilename, out isNewFile);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Nonexistent Image.jpg");
            string destinationImagePath = Path.Combine(imageDestinationDirectory, "Nonexistent Image.jpg");
            Assert.IsFalse(File.Exists(sourceImagePath));
            Assert.IsFalse(File.Exists(destinationImagePath));

            Asset asset = new Asset
            {
                FileName = "Nonexistent Image.jpg",
                Folder = sourceFolder,
                FolderId = sourceFolder.FolderId
            };

            Assert.AreEqual(sourceFolder, asset.Folder);
            Assert.AreNotEqual(destinationFolder, asset.Folder);
            
            catalogAssetsService.MoveAsset(asset, sourceFolder, destinationFolder, preserveOriginalFile: false);
        }

        [TestMethod]
        public void MoveAssetToSamePathTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            Folder sourceFolder = repository.AddFolder(dataDirectory);

            var thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out bool isNewFile);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 5.jpg");
            Assert.IsTrue(File.Exists(sourceImagePath));

            Asset asset = catalogAssetsService.CreateAsset(thumbnailsSourceFolder, dataDirectory, "Image 5.jpg");
            repository.SaveCatalog(thumbnailsSourceFolder, sourceFolder.ThumbnailsFilename);

            Assert.IsTrue(thumbnailsSourceFolder.ContainsKey(asset.FileName));

            bool result = catalogAssetsService.MoveAsset(asset, sourceFolder, sourceFolder, preserveOriginalFile: false);

            thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out isNewFile);

            Assert.IsFalse(result);
            Assert.IsTrue(File.Exists(sourceImagePath));

            Assert.IsTrue(thumbnailsSourceFolder.ContainsKey(asset.FileName));
        }

        [TestMethod]
        public void CopyExistingAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            var thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out bool isNewFile);
            var thumbnailsDestinationFolder = repository.GetThumbnails(destinationFolder.ThumbnailsFilename, out isNewFile);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 5.jpg");
            string destinationImagePath = Path.Combine(imageDestinationDirectory, "Image 5.jpg");
            Assert.IsTrue(File.Exists(sourceImagePath));
            Assert.IsFalse(File.Exists(destinationImagePath));

            Asset asset = catalogAssetsService.CreateAsset(thumbnailsSourceFolder, dataDirectory, "Image 5.jpg");
            repository.SaveCatalog(thumbnailsSourceFolder, sourceFolder.ThumbnailsFilename);
            repository.SaveCatalog(thumbnailsDestinationFolder, destinationFolder.ThumbnailsFilename);

            Assert.IsTrue(thumbnailsSourceFolder.ContainsKey(asset.FileName));
            Assert.IsFalse(thumbnailsDestinationFolder.ContainsKey(asset.FileName));

            bool result = catalogAssetsService.MoveAsset(asset, sourceFolder, destinationFolder, preserveOriginalFile: true);

            thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out isNewFile);
            thumbnailsDestinationFolder = repository.GetThumbnails(destinationFolder.ThumbnailsFilename, out isNewFile);

            Assert.IsTrue(result);
            Assert.IsTrue(File.Exists(sourceImagePath));
            Assert.IsTrue(File.Exists(destinationImagePath));

            Assert.IsTrue(thumbnailsSourceFolder.ContainsKey(asset.FileName));
            Assert.IsTrue(thumbnailsDestinationFolder.ContainsKey(asset.FileName));

            // Validates if the catalogued assets for the source folder are updated properly.
            var assets = repository.GetCataloguedAssets(sourceFolder.Path);
            int count = assets.Count(a => a.FileName == "Image 5.jpg");
            Assert.AreEqual(1, count);

            // Validates if the catalogued assets for the destination folder are updated properly.
            assets = repository.GetCataloguedAssets(destinationFolder.Path);
            count = assets.Count(a => a.FileName == "Image 5.jpg");
            Assert.AreEqual(1, count);
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentException))]
        public void CopyNonExistingAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            var thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out bool isNewFile);
            var thumbnailsDestinationFolder = repository.GetThumbnails(destinationFolder.ThumbnailsFilename, out isNewFile);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Nonexistent Image.jpg");
            string destinationImagePath = Path.Combine(imageDestinationDirectory, "Nonexistent Image.jpg");
            Assert.IsFalse(File.Exists(sourceImagePath));
            Assert.IsFalse(File.Exists(destinationImagePath));

            Asset asset = new Asset
            {
                FileName = "Nonexistent Image.jpg",
                Folder = sourceFolder,
                FolderId = sourceFolder.FolderId
            };

            Assert.AreEqual(sourceFolder, asset.Folder);
            Assert.AreNotEqual(destinationFolder, asset.Folder);

            catalogAssetsService.MoveAsset(asset, sourceFolder, destinationFolder, preserveOriginalFile: true);
        }

        [TestMethod]
        public void CopyAssetToSamePathTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            
            var thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out bool isNewFile);
            
            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 5.jpg");
            Assert.IsTrue(File.Exists(sourceImagePath));
            
            Asset asset = catalogAssetsService.CreateAsset(thumbnailsSourceFolder, dataDirectory, "Image 5.jpg");
            repository.SaveCatalog(thumbnailsSourceFolder, sourceFolder.ThumbnailsFilename);
            
            Assert.IsTrue(thumbnailsSourceFolder.ContainsKey(asset.FileName));
            
            bool result = catalogAssetsService.MoveAsset(asset, sourceFolder, sourceFolder, preserveOriginalFile: true);

            thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out isNewFile);
            
            Assert.IsFalse(result);
            Assert.IsTrue(File.Exists(sourceImagePath));
            
            Assert.IsTrue(thumbnailsSourceFolder.ContainsKey(asset.FileName));
        }
    }
}