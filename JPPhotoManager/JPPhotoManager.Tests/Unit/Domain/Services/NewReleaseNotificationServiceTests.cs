using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces.Services;
using JPPhotoManager.Domain.Services;
using Moq;
using System.Reflection;
using Xunit;

namespace JPPhotoManager.Tests.Unit.Domain.Services
{
    public class NewReleaseNotificationServiceTests
    {
        [Theory]
        [InlineData("v1.0.0", "v2.0.0", true)]
        [InlineData("v1.0.0", "v1.1.0", true)]
        [InlineData("v1.0.0", "v0.0.1", false)]
        [InlineData("v1.0.0", "v0.1.0", false)]
        [InlineData("v1.0.0", "v1.0.0", false)]
        [InlineData("v1.0.0", "v2.1.0", true)]
        [InlineData("v1.0.0", "v2.1.1", true)]
        [InlineData("v1.0.0", "", false)]
        [InlineData("", "", false)]
        [InlineData("invalid", "invalid", false)]
        [InlineData("1.0.0", "2.0.0", false)]
        [InlineData("a.b.c", "a.b.c", false)]
        public async void CheckNewRelease(string currentVersion, string latestReleaseName, bool isNewRelease)
        {
            using var mock = AutoMock.GetLoose(
               cfg =>
               {
                   cfg.RegisterType<NewReleaseNotificationService>().As<INewReleaseNotificationService>().SingleInstance();
               });
            mock.Mock<IReleaseAvailabilityService>().Setup(s => s.GetLatestReleaseAsync())
                .Returns(Task.FromResult(new Release
                {
                    Name = latestReleaseName,
                    PublishedOn = new DateTime(2021, 11, 27),
                    DownloadUrl = $"https://github.com/jpablodrexler/jp-photo-manager/releases/tag/{latestReleaseName}"
                }));

            mock.Mock<IUserConfigurationService>().Setup(s => s.GetAboutInformation(It.IsAny<Assembly>()))
                .Returns(new AboutInformation
                {
                    Version = currentVersion
                });

            var newReleaseNotificationService = mock.Create<INewReleaseNotificationService>();
            var newReleaseResult = await newReleaseNotificationService.CheckNewReleaseAsync();

            newReleaseResult.Should().NotBeNull();
            newReleaseResult.Name.Should().Be(latestReleaseName);
            newReleaseResult.PublishedOn.Should().NotBeNull();
            newReleaseResult.PublishedOn.Should().BeAfter(DateTime.MinValue);
            newReleaseResult.PublishedOn.Should().BeBefore(DateTime.UtcNow);
            newReleaseResult.DownloadUrl.Should().NotBeNullOrWhiteSpace();
            newReleaseResult.IsNewRelease.Should().Be(isNewRelease);
            newReleaseResult.Success.Should().BeTrue();
        }

        [Fact]
        public async void CheckNewRelease_GetLatestReleaseThrowsException_ReturnFalse()
        {
            using var mock = AutoMock.GetLoose(
               cfg =>
               {
                   cfg.RegisterType<NewReleaseNotificationService>().As<INewReleaseNotificationService>().SingleInstance();
               });
            mock.Mock<IReleaseAvailabilityService>().Setup(s => s.GetLatestReleaseAsync())
                .Throws<Exception>();

            mock.Mock<IUserConfigurationService>().Setup(s => s.GetAboutInformation(It.IsAny<Assembly>()))
                .Returns(new AboutInformation
                {
                    Version = "v1.0.0"
                });

            var newReleaseNotificationService = mock.Create<INewReleaseNotificationService>();
            var newReleaseResult = await newReleaseNotificationService.CheckNewReleaseAsync();

            newReleaseResult.Should().NotBeNull();
            newReleaseResult.Name.Should().BeNullOrEmpty();
            newReleaseResult.IsNewRelease.Should().BeFalse();
            newReleaseResult.Success.Should().BeFalse();
        }

        [Fact]
        public async void CheckNewRelease_GetAboutInformationThrowsException_ReturnFalse()
        {
            using var mock = AutoMock.GetLoose(
               cfg =>
               {
                   cfg.RegisterType<NewReleaseNotificationService>().As<INewReleaseNotificationService>().SingleInstance();
               });
            mock.Mock<IUserConfigurationService>().Setup(s => s.GetAboutInformation(It.IsAny<Assembly>()))
                .Throws<Exception>();

            var newReleaseNotificationService = mock.Create<INewReleaseNotificationService>();
            var newReleaseResult = await newReleaseNotificationService.CheckNewReleaseAsync();

            newReleaseResult.Should().NotBeNull();
            newReleaseResult.Name.Should().BeNullOrEmpty();
            newReleaseResult.IsNewRelease.Should().BeFalse();
            newReleaseResult.Success.Should().BeFalse();
        }

        [Fact]
        public async void CheckNewRelease_GetAboutInformationNull_ReturnFalse()
        {
            using var mock = AutoMock.GetLoose(
               cfg =>
               {
                   cfg.RegisterType<NewReleaseNotificationService>().As<INewReleaseNotificationService>().SingleInstance();
               });
            var newReleaseNotificationService = mock.Create<INewReleaseNotificationService>();
            var newReleaseResult = await newReleaseNotificationService.CheckNewReleaseAsync();

            newReleaseResult.Should().NotBeNull();
            newReleaseResult.Name.Should().BeNullOrEmpty();
            newReleaseResult.IsNewRelease.Should().BeFalse();
            newReleaseResult.Success.Should().BeFalse();
        }

        [Fact]
        public async void CheckNewRelease_GetLatestReleaseNull_ReturnFalse()
        {
            using var mock = AutoMock.GetLoose(
               cfg =>
               {
                   cfg.RegisterType<NewReleaseNotificationService>().As<INewReleaseNotificationService>().SingleInstance();
               });
            mock.Mock<IUserConfigurationService>().Setup(s => s.GetAboutInformation(It.IsAny<Assembly>()))
                .Returns(new AboutInformation
                {
                    Version = "v1.0.0"
                });

            var newReleaseNotificationService = mock.Create<INewReleaseNotificationService>();
            var newReleaseResult = await newReleaseNotificationService.CheckNewReleaseAsync();

            newReleaseResult.Should().NotBeNull();
            newReleaseResult.Name.Should().BeNullOrEmpty();
            newReleaseResult.IsNewRelease.Should().BeFalse();
            newReleaseResult.Success.Should().BeFalse();
        }
    }
}
