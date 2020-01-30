using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Moq;
using System;
using System.Collections.Generic;
using System.IO;
using System.Windows.Media.Imaging;
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

            string hiddenFolderPath = Path.Combine(dataDirectory, "TestFolder", "TestHiddenSubFolder");
            File.SetAttributes(hiddenFolderPath, File.GetAttributes(hiddenFolderPath) | FileAttributes.Hidden);

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

        [Fact]
        public void GetDrivesTest()
        {
            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            Folder[] drives = storageService.GetDrives();
            Assert.NotEmpty(drives);
        }

        [Fact]
        public void GetFoldersWithoutHiddenTest()
        {
            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string parentPath = Path.Combine(dataDirectory, "TestFolder");
            Folder[] folders = storageService.GetFolders(new Folder { Path = parentPath }, false);

            Assert.Equal(2, folders.Length);
            Assert.Equal("TestSubFolder1", folders[0].Name);
            Assert.Equal("TestSubFolder2", folders[1].Name);
        }

        [Fact]
        public void GetFoldersWithHiddenTest()
        {
            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string parentPath = Path.Combine(dataDirectory, "TestFolder");
            Folder[] folders = storageService.GetFolders(new Folder { Path = parentPath }, true);

            Assert.Equal(3, folders.Length);
            Assert.Equal("TestHiddenSubFolder", folders[0].Name);
            Assert.Equal("TestSubFolder1", folders[1].Name);
            Assert.Equal("TestSubFolder2", folders[2].Name);
        }

        [Fact]
        public void GetSubDirectoriesTest()
        {
            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string parentPath = Path.Combine(dataDirectory, "TestFolder");
            List<DirectoryInfo> directories = storageService.GetSubDirectories(parentPath);

            Assert.Equal(3, directories.Count);
            Assert.Equal("TestHiddenSubFolder", directories[0].Name);
            Assert.Equal("TestSubFolder1", directories[1].Name);
            Assert.Equal("TestSubFolder2", directories[2].Name);
        }

        [Fact]
        public void WriteReadJsonTest()
        {
            List<string> writtenList = new List<string> { "Value 1", "Value 2" };
            string jsonPath = Path.Combine(dataDirectory, "test.json");

            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            storageService.WriteObjectToJson(writtenList, jsonPath);
            List<string> readList = storageService.ReadObjectFromJson<List<string>>(jsonPath);

            Assert.Equal(writtenList.Count, readList.Count);
            Assert.Equal(writtenList[0], readList[0]);
            Assert.Equal(writtenList[1], readList[1]);
        }

        [Theory]
        [InlineData(0, Rotation.Rotate0)]
        [InlineData(1, Rotation.Rotate0)]
        [InlineData(2, Rotation.Rotate0)]
        [InlineData(3, Rotation.Rotate180)]
        [InlineData(4, Rotation.Rotate180)]
        [InlineData(5, Rotation.Rotate90)]
        [InlineData(6, Rotation.Rotate90)]
        [InlineData(7, Rotation.Rotate270)]
        [InlineData(8, Rotation.Rotate270)]
        [InlineData(9, Rotation.Rotate0)]
        [InlineData(10, Rotation.Rotate0)]
        [InlineData(ushort.MinValue, Rotation.Rotate0)]
        [InlineData(ushort.MaxValue, Rotation.Rotate0)]
        public void GetImageRotationTest(ushort exifOrientation, Rotation expected)
        {
            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            Rotation result = storageService.GetImageRotation(exifOrientation);

            Assert.Equal(expected, result);
        }
    }
}
