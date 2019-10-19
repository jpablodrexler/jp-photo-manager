using Microsoft.VisualStudio.TestTools.UnitTesting;
using JPPhotoManager.Infrastructure;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using JPPhotoManager.Domain;
using System.IO;
using Microsoft.Extensions.Configuration;
using Moq;

namespace JPPhotoManager.Test
{
    [TestClass]
    public class StorageServiceTest
    {
        private static string dataDirectory;
        private static IConfigurationRoot configuration;

        [ClassInitialize]
        public static void AssetRepositoryTestInitialize(TestContext testContext)
        {
            dataDirectory = Path.GetDirectoryName(typeof(AssetRepositoryTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");

            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", dataDirectory)
                .MockGetValue("appsettings:CatalogBatchSize", "100");

            configuration = configurationMock.Object;
        }

        [TestMethod]
        public void ResolveDataDirectoryTest1()
        {
            string directory = @"C:\Data\JPPhotoManager";
            string expected = @"C:\Data\JPPhotoManager\AssetCatalog.json";

            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string result = storageService.ResolveCatalogPath(directory);

            Assert.AreEqual(expected, result);
        }

        [TestMethod]
        public void ResolveDataDirectoryTest2()
        {
            string directory = "";
            string expected = "AssetCatalog.json";

            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string result = storageService.ResolveCatalogPath(directory);

            Assert.AreEqual(expected, result);
        }

        [TestMethod]
        public void ResolveDataDirectoryTest3()
        {
            string directory = null;
            string expected = "AssetCatalog.json";

            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string result = storageService.ResolveCatalogPath(directory);

            Assert.AreEqual(expected, result);
        }

        [TestMethod]
        public void ResolveCatalogPathTest1()
        {
            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", "{ApplicationData}\\JPPhotoManager")
                .MockGetValue("appsettings:ApplicationDataDirectory", "{ApplicationData}\\JPPhotoManager")
                .MockGetValue("appsettings:CatalogBatchSize", "100");
            
            string expected = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "JPPhotoManager");
            
            IStorageService storageService = new StorageService(new UserConfigurationService(configurationMock.Object));
            string result = storageService.ResolveDataDirectory();

            Assert.AreEqual(expected, result);
        }

        [TestMethod]
        public void ResolveCatalogPathTest2()
        {
            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", "{ApplicationData}\\JPPhotoManager")
                .MockGetValue("appsettings:ApplicationDataDirectory", "{ApplicationData}\\JPPhotoManager")
                .MockGetValue("appsettings:CatalogBatchSize", "100");

            string expected = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "JPPhotoManager");
            
            IStorageService storageService = new StorageService(new UserConfigurationService(configurationMock.Object));
            string result = storageService.ResolveDataDirectory();

            Assert.AreEqual(expected, result);
        }

        [TestMethod]
        public void GetFileNamesTest()
        {
            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string[] fileNames = storageService.GetFileNames(dataDirectory);

            Assert.IsTrue(fileNames.Length >= 2);
            Assert.IsTrue(fileNames.Contains("Image 2.jpg"));
            Assert.IsTrue(fileNames.Contains("Image 1.jpg"));
        }
    }
}
