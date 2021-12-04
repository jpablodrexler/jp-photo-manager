using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain.Interfaces;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Moq;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests.Integration
{
    public class ReleaseAvailabilityServiceTests
    {
        private string dataDirectory;
        private IConfigurationRoot configuration;

        public ReleaseAvailabilityServiceTests()
        {
            dataDirectory = Path.GetDirectoryName(typeof(ApplicationTests).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");

            Mock<IConfigurationRoot> configurationMock = new();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", Path.Combine(dataDirectory, "ApplicationData", Guid.NewGuid().ToString()))
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:Repository:Owner", "jpablodrexler")
                .MockGetValue("appsettings:Repository:Name", "jp-photo-manager");

            configuration = configurationMock.Object;
        }

        [Fact]
        public async void GetLatestRelease_ReturnReleaseData()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            using var mock = AutoMock.GetLoose(
               cfg =>
               {
                   cfg.RegisterInstance(userConfigurationService).As<IUserConfigurationService>();
                   cfg.RegisterType<GitHubReleaseAvailabilityService>().As<IReleaseAvailabilityService>().SingleInstance();
               });
            var releaseAvailabilityService = mock.Create<IReleaseAvailabilityService>();
            var latestRelease = await releaseAvailabilityService.GetLatestRelease();

            latestRelease.Should().NotBeNull();
            latestRelease.Name.Should().NotBeNullOrWhiteSpace();
            latestRelease.PublishedOn.Should().NotBeNull();
            latestRelease.PublishedOn.Should().BeAfter(DateTime.MinValue);
            latestRelease.PublishedOn.Should().BeBefore(DateTime.UtcNow);
            latestRelease.DownloadUrl.Should().NotBeNullOrWhiteSpace();
        }
    }
}
