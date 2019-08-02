using Microsoft.VisualStudio.TestTools.UnitTesting;
using AssetManager.Infrastructure;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using AssetManager.Domain;
using System.IO;

namespace AssetManager.Test
{
    [TestClass]
    public class StorageServiceTest
    {
        private static string dataDirectory;

        [ClassInitialize]
        public static void AssetRepositoryTestInitialize(TestContext testContext)
        {
            dataDirectory = Path.GetDirectoryName(typeof(AssetRepositoryTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");
        }

        [TestMethod]
        public void ResolveDataDirectoryTest1()
        {
            string directory = @"C:\Data\AssetManager";
            string expected = @"C:\Data\AssetManager\AssetCatalog.json";

            IStorageService storageService = new StorageService(new UserConfigurationService());
            string result = storageService.ResolveCatalogPath(directory);

            Assert.AreEqual(expected, result);
        }

        [TestMethod]
        public void ResolveDataDirectoryTest2()
        {
            string directory = "";
            string expected = "AssetCatalog.json";

            IStorageService storageService = new StorageService(new UserConfigurationService());
            string result = storageService.ResolveCatalogPath(directory);

            Assert.AreEqual(expected, result);
        }

        [TestMethod]
        public void ResolveDataDirectoryTest3()
        {
            string directory = null;
            string expected = "AssetCatalog.json";

            IStorageService storageService = new StorageService(new UserConfigurationService());
            string result = storageService.ResolveCatalogPath(directory);

            Assert.AreEqual(expected, result);
        }

        [TestMethod]
        public void ResolveCatalogPathTest1()
        {
            string expected = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "AssetManager");
            string directory = "";

            IStorageService storageService = new StorageService(new UserConfigurationService());
            string result = storageService.ResolveDataDirectory(directory);

            Assert.AreEqual(expected, result);
        }

        [TestMethod]
        public void ResolveCatalogPathTest2()
        {
            string expected = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "AssetManager");
            string directory = null;

            IStorageService storageService = new StorageService(new UserConfigurationService());
            string result = storageService.ResolveDataDirectory(directory);

            Assert.AreEqual(expected, result);
        }

        [TestMethod]
        public void ResolveCatalogPathTest3()
        {
            string expected = @"C:\Data\AssetManager";

            IStorageService storageService = new StorageService(new UserConfigurationService());
            string result = storageService.ResolveDataDirectory(expected);

            Assert.AreEqual(expected, result);
        }

        [TestMethod]
        public void GetFileNamesTest()
        {
            IStorageService storageService = new StorageService(new UserConfigurationService());
            string[] fileNames = storageService.GetFileNames(dataDirectory);

            Assert.IsTrue(fileNames.Length >= 2);
            Assert.IsTrue(fileNames.Contains("Image 2.jpg"));
            Assert.IsTrue(fileNames.Contains("Image 1.jpg"));
        }
    }
}
