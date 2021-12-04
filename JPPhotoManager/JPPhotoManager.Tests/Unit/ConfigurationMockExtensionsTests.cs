using FluentAssertions;
using Microsoft.Extensions.Configuration;
using Moq;
using Xunit;

namespace JPPhotoManager.Tests.Unit
{
    public class ConfigurationMockExtensionsTests
    {
        [Fact]
        public void ConfigurationMockTest()
        {
            Mock<IConfigurationRoot> configurationMock = new();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", "dataDirectory1")
                .MockGetValue("appsettings:ApplicationDataDirectory", "dataDirectory2")
                .MockGetValue("appsettings:CatalogBatchSize", "100");

            IConfigurationRoot configuration = configurationMock.Object;
            configuration.GetValue<string>("appsettings:InitialDirectory").Should().Be("dataDirectory1");
            configuration.GetValue<string>("appsettings:ApplicationDataDirectory").Should().Be("dataDirectory2");
            configuration.GetValue<string>("appsettings:CatalogBatchSize").Should().Be("100");
        }
    }
}
