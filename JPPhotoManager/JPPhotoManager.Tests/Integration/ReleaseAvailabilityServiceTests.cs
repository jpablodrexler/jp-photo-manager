using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain.Interfaces.Services;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.Infrastructure.Services;
using Microsoft.Extensions.Configuration;
using Moq;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests.Integration
{
    public class ReleaseAvailabilityServiceTests
    {
        private string _dataDirectory;
        private IConfigurationRoot _configuration;

        public ReleaseAvailabilityServiceTests()
        {
            _dataDirectory = Path.GetDirectoryName(typeof(ApplicationTests).Assembly.Location);
            _dataDirectory = Path.Combine(_dataDirectory, "TestFiles");

            Mock<IConfigurationRoot> configurationMock = new();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", _dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", Path.Combine(_dataDirectory, "ApplicationData", Guid.NewGuid().ToString()))
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:Repository:Owner", "jpablodrexler")
                .MockGetValue("appsettings:Repository:Name", "jp-photo-manager");

            _configuration = configurationMock.Object;
        }

        [Fact(Skip = "GitHub API integration test")]
        public async void GetLatestRelease_ReturnReleaseData()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(_configuration);

            using var mock = AutoMock.GetLoose(
               cfg =>
               {
                   cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                   cfg.RegisterType<GitHubReleaseAvailabilityService>().As<IReleaseAvailabilityService>().SingleInstance();
               });
            var releaseAvailabilityService = mock.Create<IReleaseAvailabilityService>();
            var latestRelease = await releaseAvailabilityService.GetLatestReleaseAsync();

            latestRelease.Should().NotBeNull();
            latestRelease.Name.Should().NotBeNullOrWhiteSpace();
            latestRelease.PublishedOn.Should().NotBeNull();
            latestRelease.PublishedOn.Should().BeAfter(DateTime.MinValue);
            latestRelease.PublishedOn.Should().BeBefore(DateTime.UtcNow);
            latestRelease.DownloadUrl.Should().NotBeNullOrWhiteSpace();
        }
    }
}
