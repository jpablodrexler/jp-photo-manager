using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI;
using Microsoft.Extensions.Configuration;
using Moq;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class UserConfigurationServiceTest
    {
        private string dataDirectory;
        private IConfigurationRoot configuration;

        public UserConfigurationServiceTest()
        {
            dataDirectory = Path.GetDirectoryName(typeof(CatalogAssetsServiceTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");

            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", dataDirectory)
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:CatalogCooldownMinutes", "5");

            configuration = configurationMock.Object;
        }

        [Fact]
        public void GetPicturesDirectoryTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            string result = userConfigurationService.GetPicturesDirectory();
            Assert.False(string.IsNullOrEmpty(result));
        }

        [Fact]
        public void GetAboutInformationTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AboutInformation result = userConfigurationService.GetAboutInformation(typeof(App).Assembly);
            Assert.Equal("JPPhotoManager", result.Product);
            Assert.NotEmpty(result.Version);
            Assert.StartsWith("Version ", result.Version);
        }

        [Fact]
        public void GetCatalogBatchSizeTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            int result = userConfigurationService.GetCatalogBatchSize();
            Assert.Equal(100, result);
        }

        [Fact]
        public void GetCatalogCooldownMinutesTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            int result = userConfigurationService.GetCatalogCooldownMinutes();
            Assert.Equal(5, result);
        }

        [Fact]
        public void GetInitialFolderConfiguredTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            string result = userConfigurationService.GetInitialFolder();
            Assert.Equal(dataDirectory, result);
        }

        [Fact]
        public void GetInitialFolderNotConfiguredTest()
        {
            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", "")
                .MockGetValue("appsettings:ApplicationDataDirectory", dataDirectory)
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:CatalogCooldownMinutes", "5");

            UserConfigurationService userConfigurationService = new UserConfigurationService(configurationMock.Object);
            string expected = userConfigurationService.GetPicturesDirectory();
            string result = userConfigurationService.GetInitialFolder();
            Assert.Equal(expected, result);
        }

        [Fact]
        public void GetApplicationDataFolderConfiguredTest()
        {
            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", dataDirectory)
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:CatalogCooldownMinutes", "5");

            UserConfigurationService userConfigurationService = new UserConfigurationService(configurationMock.Object);
            string result = userConfigurationService.GetApplicationDataFolder();
            Assert.NotEmpty(result);
        }

        [Fact]
        public void GetApplicationDataFolderNotConfiguredTest()
        {
            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", "")
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:CatalogCooldownMinutes", "5");

            UserConfigurationService userConfigurationService = new UserConfigurationService(configurationMock.Object);
            string result = userConfigurationService.GetApplicationDataFolder();
            Assert.NotEmpty(result);
        }
    }
}
