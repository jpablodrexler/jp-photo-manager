using JPPhotoManager.Infrastructure;
using System;
using JPPhotoManager.Domain;
using System.IO;
using Microsoft.Extensions.Configuration;
using Moq;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class StorageServiceTest
    {
        private string dataDirectory;
        private IConfigurationRoot configuration;

        public StorageServiceTest()
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

        [Fact]
        public void ResolveDataDirectoryTest1()
        {
            string directory = @"C:\Data\JPPhotoManager";
            string expected = @"C:\Data\JPPhotoManager\Tables\assets.db";

            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string result = storageService.ResolveTableFilePath(directory, "assets");

            Assert.Equal(expected, result);
        }

        [Fact]
        public void ResolveDataDirectoryTest2()
        {
            string directory = "";
            string expected = @"Tables\assets.db";

            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string result = storageService.ResolveTableFilePath(directory, "assets");

            Assert.Equal(expected, result);
        }

        [Fact]
        public void ResolveDataDirectoryTest3()
        {
            string directory = null;
            string expected = @"Tables\assets.db";

            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string result = storageService.ResolveTableFilePath(directory, "assets");

            Assert.Equal(expected, result);
        }

        [Fact]
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

            Assert.Equal(expected, result);
        }

        [Fact]
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

            Assert.Equal(expected, result);
        }

        [Fact]
        public void GetFileNamesTest()
        {
            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string[] fileNames = storageService.GetFileNames(dataDirectory);

            Assert.True(fileNames.Length >= 2);
            Assert.Contains("Image 2.jpg", fileNames);
            Assert.Contains("Image 1.jpg", fileNames);
        }
    }
}
