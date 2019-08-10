using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Moq;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace JPPhotoManager.Test
{
    [TestClass]
    public class CatalogAssetsServiceTest
    {
        private static string dataDirectory;
        private static string assetDataSetPath;
        private static string imageDestinationDirectory;
        private static string nonCataloguedImageDestinationDirectory;

        [ClassInitialize]
        public static void CatalogAssetsServiceTestInitialize(TestContext testContext)
        {
            dataDirectory = Path.GetDirectoryName(typeof(CatalogAssetsServiceTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");
            assetDataSetPath = Path.Combine(dataDirectory, "AssetCatalog.json");
            imageDestinationDirectory = Path.Combine(dataDirectory, "NewFolder");
            nonCataloguedImageDestinationDirectory = Path.Combine(dataDirectory, "NonCataloguedNewFolder");
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

            if (Directory.Exists(nonCataloguedImageDestinationDirectory))
            {
                Directory.Delete(nonCataloguedImageDestinationDirectory, true);
            }
        }

        [TestMethod]
        public void CatalogFolderTest()
        {
            Dictionary<string, byte[]> thumbnails = new Dictionary<string, byte[]>();

            Mock<IUserConfigurationService> userConfigurationService = new Mock<IUserConfigurationService>();
            userConfigurationService.Setup(conf => conf.GetPicturesDirectory()).Returns(dataDirectory);

            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService.Object), userConfigurationService.Object);
            repository.Initialize(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService.Object),
                    userConfigurationService.Object);

            string[] fileList = Directory.GetFiles(dataDirectory, "*.jpg")
                .Select(f => Path.GetFileName(f))
                .ToArray();

            var processedAssets = new List<Asset>();

            catalogAssetsService.CatalogImages(e =>
            {
                if (e.Asset != null)
                {
                    processedAssets.Add(e.Asset);
                }
            });

            var repositoryAssets = repository.GetAssets(dataDirectory);
            Assert.AreEqual(fileList.Length, processedAssets.Count);
            Assert.AreEqual(fileList.Length, repositoryAssets.Length);

            bool allProcessedAssetsInFileList = processedAssets.All(a => fileList.Contains(a.FileName));
            bool allProcessedAssetsInRepository = processedAssets.All(a => repositoryAssets.Contains(a));
            bool allRepositoryAssetsInProcessed = repositoryAssets.All(a => processedAssets.Contains(a));

            Assert.IsTrue(allProcessedAssetsInFileList);
            Assert.IsTrue(allProcessedAssetsInRepository);
            Assert.IsTrue(allRepositoryAssetsInProcessed);
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

            bool result = catalogAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: false);

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
            
            catalogAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: false);
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

            bool result = catalogAssetsService.MoveAsset(asset, sourceFolder, preserveOriginalFile: false);

            thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out isNewFile);

            Assert.IsFalse(result);
            Assert.IsTrue(File.Exists(sourceImagePath));

            Assert.IsTrue(thumbnailsSourceFolder.ContainsKey(asset.FileName));
        }

        [TestMethod]
        public void MoveAssetToNonCataloguedFolderTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            var thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out bool isNewFile);

            Folder destinationFolder = repository.GetFolderByPath(nonCataloguedImageDestinationDirectory);

            Assert.IsNull(destinationFolder);

            destinationFolder = new Folder { Path = nonCataloguedImageDestinationDirectory };
            
            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Image 7.jpg");
            Assert.IsTrue(File.Exists(sourceImagePath));

            string destinationImagePath = Path.Combine(nonCataloguedImageDestinationDirectory, "Image 7.jpg");
            Assert.IsFalse(File.Exists(destinationImagePath));

            Asset asset = catalogAssetsService.CreateAsset(thumbnailsSourceFolder, dataDirectory, "Image 7.jpg");
            repository.SaveCatalog(thumbnailsSourceFolder, sourceFolder.ThumbnailsFilename);

            Assert.IsTrue(thumbnailsSourceFolder.ContainsKey(asset.FileName));

            bool result = catalogAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: false);

            thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out isNewFile);

            Assert.IsTrue(result);
            Assert.IsFalse(File.Exists(sourceImagePath));
            Assert.IsTrue(File.Exists(destinationImagePath));
            Assert.IsFalse(thumbnailsSourceFolder.ContainsKey(asset.FileName));
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentNullException))]
        public void MoveNullAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            catalogAssetsService.MoveAsset(null, destinationFolder, preserveOriginalFile: false);
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentNullException))]
        public void MoveNullSourceFolderTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            catalogAssetsService.MoveAsset(new Asset { Folder = null }, destinationFolder, preserveOriginalFile: false);
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentNullException))]
        public void MoveNullDestinationFolderTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            Folder sourceFolder = repository.AddFolder(dataDirectory);
            Folder destinationFolder = repository.AddFolder(imageDestinationDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            catalogAssetsService.MoveAsset(new Asset { Folder = new Folder { } }, null, preserveOriginalFile: false);
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

            bool result = catalogAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: true);

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

            catalogAssetsService.MoveAsset(asset, destinationFolder, preserveOriginalFile: true);
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
            
            bool result = catalogAssetsService.MoveAsset(asset, sourceFolder, preserveOriginalFile: true);

            thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out isNewFile);
            
            Assert.IsFalse(result);
            Assert.IsTrue(File.Exists(sourceImagePath));
            Assert.IsTrue(thumbnailsSourceFolder.ContainsKey(asset.FileName));
        }

        [TestMethod]
        public void DeleteExistingImageTest()
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

            string sourceImagePath = Path.Combine(dataDirectory, "Image 6.jpg");
            Assert.IsTrue(File.Exists(sourceImagePath));

            Asset asset = catalogAssetsService.CreateAsset(thumbnailsSourceFolder, dataDirectory, "Image 6.jpg");
            repository.SaveCatalog(thumbnailsSourceFolder, sourceFolder.ThumbnailsFilename);

            Assert.IsTrue(thumbnailsSourceFolder.ContainsKey(asset.FileName));

            catalogAssetsService.DeleteAsset(asset, deleteFile: true);

            thumbnailsSourceFolder = repository.GetThumbnails(sourceFolder.ThumbnailsFilename, out isNewFile);

            Assert.IsFalse(File.Exists(sourceImagePath));
            Assert.IsFalse(thumbnailsSourceFolder.ContainsKey(asset.FileName));
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentException))]
        public void DeleteNonExistingImageTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            Folder sourceFolder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            string sourceImagePath = Path.Combine(dataDirectory, "Nonexistent Image.jpg");
            Assert.IsFalse(File.Exists(sourceImagePath));

            Asset asset = new Asset
            {
                FileName = "Nonexistent Image.jpg",
                Folder = sourceFolder,
                FolderId = sourceFolder.FolderId
            };

            catalogAssetsService.DeleteAsset(asset, deleteFile: true);
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentNullException))]
        public void DeleteNullAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            Folder sourceFolder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            catalogAssetsService.DeleteAsset(null, deleteFile: true);
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentNullException))]
        public void DeleteNullFolderTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    userConfigurationService);

            catalogAssetsService.DeleteAsset(new Asset { Folder = null }, deleteFile: true);
        }
    }
}
