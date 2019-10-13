using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Moq;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace JPPhotoManager.Test
{
    [TestClass]
    public class AssetRepositoryTest
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
        public void FolderExistsTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
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

            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
            Assert.IsFalse(repository.HasChanges());
        }

        [TestMethod]
        public void HasChangesTrueAfterChangeTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));

            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(repository.HasChanges());
        }

        [TestMethod]
        public void HasChangesFalseAfterSaveTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.IsTrue(File.Exists(imagePath));

            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");
            repository.SaveCatalog(null);
            Assert.IsFalse(repository.HasChanges());
        }

        [TestMethod]
        public void IsAssetCataloguedImageInCatalogTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(File.Exists(imagePath));

            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration));

            bool isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Image 2.jpg");
            Assert.IsFalse(isCatalogued);
            repository.AddFolder(dataDirectory);
            catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");
            isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Image 2.jpg");
            Assert.IsTrue(isCatalogued);
        }

        [TestMethod]
        public void DeleteNonExistingAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize();
            repository.AddFolder(dataDirectory);

            string imagePath = Path.Combine(dataDirectory, "Non Existing Image.jpg");
            Assert.IsFalse(File.Exists(imagePath));
            
            repository.DeleteAsset(dataDirectory, "Non Existing Image.jpg");
            bool isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Non Existing Image.jpg");
            Assert.IsFalse(isCatalogued);
            
            repository.DeleteAsset(dataDirectory, "Non Existing Image.jpg");
            isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Non Existing Image.jpg");
            Assert.IsFalse(isCatalogued);
        }

        [TestMethod]
        public void DeleteExistingAssetTest()
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

            string imagePath = Path.Combine(dataDirectory, "Image 3.jpg");
            Assert.IsTrue(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 3.jpg");

            // The asset should no longer be catalogued, but the image should still be in the filesystem.
            repository.DeleteAsset(dataDirectory, "Image 3.jpg");
            bool isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Image 3.jpg");
            Assert.IsFalse(isCatalogued);
            Assert.IsTrue(File.Exists(imagePath));
        }
    }
}
