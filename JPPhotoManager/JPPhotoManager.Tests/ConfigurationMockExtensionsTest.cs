using Microsoft.Extensions.Configuration;
using Moq;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class ConfigurationMockExtensionsTest
    {
        [Fact]
        public void ConfigurationMockTest()
        {
            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", "dataDirectory1")
                .MockGetValue("appsettings:ApplicationDataDirectory", "dataDirectory2")
                .MockGetValue("appsettings:CatalogBatchSize", "100");

            IConfigurationRoot configuration = configurationMock.Object;
            Assert.Equal("dataDirectory1", configuration.GetValue<string>("appsettings:InitialDirectory"));
            Assert.Equal("dataDirectory2", configuration.GetValue<string>("appsettings:ApplicationDataDirectory"));
            Assert.Equal("100", configuration.GetValue<string>("appsettings:CatalogBatchSize"));
        }
    }
}
