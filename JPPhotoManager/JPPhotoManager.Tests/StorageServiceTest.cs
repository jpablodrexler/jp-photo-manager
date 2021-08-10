using FluentAssertions;
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
        public void ResolveCatalogPathTest1()
        {
            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", "{ApplicationData}\\JPPhotoManager")
                .MockGetValue("appsettings:ApplicationDataDirectory", "{ApplicationData}\\JPPhotoManager")
                .MockGetValue("appsettings:CatalogBatchSize", "100");
            
            string expected = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "JPPhotoManager", "v3");
            
            IStorageService storageService = new StorageService(new UserConfigurationService(configurationMock.Object));
            string result = storageService.ResolveDataDirectory(3);

            result.Should().Be(expected);
        }

        [Fact]
        public void ResolveCatalogPathTest2()
        {
            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", "{ApplicationData}\\JPPhotoManager")
                .MockGetValue("appsettings:ApplicationDataDirectory", "{ApplicationData}\\JPPhotoManager")
                .MockGetValue("appsettings:CatalogBatchSize", "100");

            string expected = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "JPPhotoManager", "v3");
            
            IStorageService storageService = new StorageService(new UserConfigurationService(configurationMock.Object));
            string result = storageService.ResolveDataDirectory(3);

            result.Should().Be(expected);
        }

        [Fact]
        public void GetFileNamesTest()
        {
            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string[] fileNames = storageService.GetFileNames(dataDirectory);

            fileNames.Should().HaveCountGreaterOrEqualTo(2);
            fileNames.Should().Contain("Image 2.jpg");
            fileNames.Should().Contain("Image 1.jpg");
        }

        [Fact]
        public void GetDrivesTest()
        {
            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            Folder[] drives = storageService.GetDrives();
            drives.Should().NotBeEmpty();
        }

        [Fact]
        public void GetSubDirectoriesTest()
        {
            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string parentPath = Path.Combine(dataDirectory, "TestFolder");
            List<DirectoryInfo> directories = storageService.GetSubDirectories(parentPath);

            directories.Should().HaveCount(3);
            directories[0].Name.Should().Be("TestHiddenSubFolder");
            directories[1].Name.Should().Be("TestSubFolder1");
            directories[2].Name.Should().Be("TestSubFolder2");
        }

        [Fact]
        public void GetRecursiveSubDirectoriesTest()
        {
            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            string parentPath = Path.Combine(dataDirectory, "TestFolder");
            List<DirectoryInfo> directories = storageService.GetRecursiveSubDirectories(parentPath);

            directories.Should().HaveCount(4);
            directories[0].FullName.Should().EndWith("\\TestHiddenSubFolder");
            directories[1].FullName.Should().EndWith("\\TestSubFolder1");
            directories[2].FullName.Should().EndWith("\\TestSubFolder2");
            directories[3].FullName.Should().EndWith("\\TestSubFolder2\\TestSubFolder3");
        }

        [Fact]
        public void WriteReadJsonTest()
        {
            List<string> writtenList = new List<string> { "Value 1", "Value 2" };
            string jsonPath = Path.Combine(dataDirectory, "test.json");

            IStorageService storageService = new StorageService(new UserConfigurationService(configuration));
            storageService.WriteObjectToJson(writtenList, jsonPath);
            List<string> readList = storageService.ReadObjectFromJson<List<string>>(jsonPath);

            readList.Should().HaveSameCount(writtenList);
            readList[0].Should().Be(writtenList[0]);
            readList[1].Should().Be(writtenList[1]);
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

            result.Should().Be(expected);
        }
    }
}
