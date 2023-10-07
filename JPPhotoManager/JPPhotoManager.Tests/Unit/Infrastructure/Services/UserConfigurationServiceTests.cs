using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure.Services;
using JPPhotoManager.UI;
using Microsoft.Extensions.Configuration;
using Moq;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests.Unit.Infrastructure.Services
{
    public class UserConfigurationServiceTests
    {
        private string _dataDirectory;
        private IConfigurationRoot _configuration;

        public UserConfigurationServiceTests()
        {
            _dataDirectory = Path.GetDirectoryName(typeof(UserConfigurationServiceTests).Assembly.Location);
            _dataDirectory = Path.Combine(_dataDirectory, "TestFiles");

            Mock<IConfigurationRoot> configurationMock = new();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", _dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", _dataDirectory)
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:CatalogCooldownMinutes", "5")
                .MockGetValue("appsettings:BackupsToKeep", "2");

            _configuration = configurationMock.Object;
        }

        [Fact]
        public void GetPicturesDirectoryTest()
        {
            UserConfigurationService userConfigurationService = new(_configuration);
            string result = userConfigurationService.GetPicturesDirectory();
            result.Should().NotBeEmpty();
        }

        [Fact]
        public void GetAboutInformationTest()
        {
            UserConfigurationService userConfigurationService = new(_configuration);
            AboutInformation result = userConfigurationService.GetAboutInformation(typeof(App).Assembly);
            result.Product.Should().Be("JPPhotoManager");
            result.Version.Should().NotBeEmpty();
            result.Version.Should().StartWith("v");
        }

        [Fact]
        public void GetCatalogBatchSizeTest()
        {
            UserConfigurationService userConfigurationService = new(_configuration);
            int result = userConfigurationService.GetCatalogBatchSize();
            result.Should().Be(100);
        }

        [Fact]
        public void GetCatalogCooldownMinutesTest()
        {
            UserConfigurationService userConfigurationService = new(_configuration);
            int result = userConfigurationService.GetCatalogCooldownMinutes();
            result.Should().Be(5);
        }

        [Fact]
        public void GetInitialFolderConfiguredTest()
        {
            UserConfigurationService userConfigurationService = new(_configuration);
            string result = userConfigurationService.GetInitialFolder();
            result.Should().Be(_dataDirectory);
        }

        [Fact]
        public void GetInitialFolderNotConfiguredTest()
        {
            Mock<IConfigurationRoot> configurationMock = new();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", "")
                .MockGetValue("appsettings:ApplicationDataDirectory", _dataDirectory)
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:CatalogCooldownMinutes", "5");

            UserConfigurationService userConfigurationService = new(configurationMock.Object);
            string expected = userConfigurationService.GetPicturesDirectory();
            string result = userConfigurationService.GetInitialFolder();
            result.Should().Be(expected);
        }

        [Fact]
        public void GetApplicationDataFolderConfiguredTest()
        {
            Mock<IConfigurationRoot> configurationMock = new();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", _dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", _dataDirectory)
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:CatalogCooldownMinutes", "5");

            UserConfigurationService userConfigurationService = new(configurationMock.Object);
            string result = userConfigurationService.GetApplicationDataFolder();
            result.Should().NotBeEmpty();
        }

        [Fact]
        public void GetApplicationDataFolderNotConfiguredTest()
        {
            Mock<IConfigurationRoot> configurationMock = new();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", _dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", "")
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:CatalogCooldownMinutes", "5");

            UserConfigurationService userConfigurationService = new(configurationMock.Object);
            string result = userConfigurationService.GetApplicationDataFolder();
            result.Should().NotBeEmpty();
        }
    }
}
