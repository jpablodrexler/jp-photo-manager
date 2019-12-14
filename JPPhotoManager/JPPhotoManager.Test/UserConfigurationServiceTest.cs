using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Moq;
using System.IO;
using Xunit;

namespace JPPhotoManager.Test
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
                .MockGetValue("appsettings:CatalogBatchSize", "100");

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
            AboutInformation result = userConfigurationService.GetAboutInformation(typeof(UserConfigurationService).Assembly);
            Assert.Equal("JPPhotoManager", result.Product);
            Assert.Equal("JPPhotoManager", result.Author);
            Assert.NotEmpty(result.Version);
            Assert.StartsWith("Version ", result.Version);
        }
    }
}
