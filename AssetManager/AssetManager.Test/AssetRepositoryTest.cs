using AssetManager.Domain;
using AssetManager.Infrastructure;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AssetManager.Test
{
    [TestClass]
    public class AssetRepositoryTest
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
        public void FolderExistsTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);
            bool folderExists = repository.FolderExists(dataDirectory);
            Assert.IsFalse(folderExists);
            repository.AddFolder(dataDirectory);
            folderExists = repository.FolderExists(dataDirectory);
            Assert.IsTrue(folderExists);
        }

        [TestMethod]
        public void HasChangesInitiallyFalseTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));

            Dictionary<string, byte[]> thumbnails = new Dictionary<string, byte[]>();
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);
            Assert.IsFalse(repository.HasChanges());
        }

        [TestMethod]
        public void HasChangesTrueAfterChangeTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));

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

            catalogAssetsService.CreateAsset(thumbnails, dataDirectory, "Image 1.jpg");
            Assert.IsTrue(repository.HasChanges());
        }

        [TestMethod]
        public void HasChangesFalseAfterSaveTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));

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

            catalogAssetsService.CreateAsset(thumbnails, dataDirectory, "Image 1.jpg");
            repository.SaveCatalog(null, null);
            Assert.IsFalse(repository.HasChanges());
        }

        [TestMethod]
        public void IsAssetCataloguedImageInCatalogTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));

            Dictionary<string, byte[]> thumbnails = new Dictionary<string, byte[]>();
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService());

            bool isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Image 2.jpg");
            Assert.IsFalse(isCatalogued);
            repository.AddFolder(dataDirectory);
            catalogAssetsService.CreateAsset(thumbnails, dataDirectory, "Image 2.jpg");
            isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(isCatalogued);
        }

        [TestMethod]
        public void DeleteNonExistingAssetTest()
        {
            Dictionary<string, byte[]> thumbnails = new Dictionary<string, byte[]>();
            UserConfigurationService userConfigurationService = new UserConfigurationService();
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService), userConfigurationService);
            repository.Initialize(dataDirectory);
            repository.AddFolder(dataDirectory);

            string imagePath = Path.Combine(dataDirectory, "Non Existing Image.jpg");
            Assert.IsFalse(File.Exists(imagePath));
            
            repository.DeleteAsset(dataDirectory, "Non Existing Image.jpg", deleteFile: false);
            bool isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Non Existing Image.jpg");
            Assert.IsFalse(isCatalogued);
            
            repository.DeleteAsset(dataDirectory, "Non Existing Image.jpg", deleteFile: true);
            isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Non Existing Image.jpg");
            Assert.IsFalse(isCatalogued);
        }

        [TestMethod]
        public void DeleteExistingAssetTest()
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

            string imagePath = Path.Combine(dataDirectory, "Image 3.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(thumbnails, dataDirectory, "Image 3.jpg");

            // The asset should no longer be catalogued, but the image should still be in the filesystem.
            repository.DeleteAsset(dataDirectory, "Image 3.jpg", deleteFile: false);
            bool isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Image 3.jpg");
            Assert.IsFalse(isCatalogued);
            Assert.IsTrue(File.Exists(imagePath));

            asset = catalogAssetsService.CreateAsset(thumbnails, dataDirectory, "Image 3.jpg");

            // The asset should no longer be catalogued, and the image should no longer be in the filesystem.
            repository.DeleteAsset(dataDirectory, "Image 3.jpg", deleteFile: true);
            isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Image 3.jpg");
            Assert.IsFalse(isCatalogued);
            Assert.IsFalse(File.Exists(imagePath));
        }
    }
}
