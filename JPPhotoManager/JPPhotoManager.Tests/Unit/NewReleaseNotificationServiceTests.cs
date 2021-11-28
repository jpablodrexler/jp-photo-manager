using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain;
using Moq;
using System.Reflection;
using Xunit;

namespace JPPhotoManager.Tests.Unit
{
    public class NewReleaseNotificationServiceTests
    {
        [Theory]
        [InlineData("v1.0.0", "v1.0.0", false)]
        [InlineData("v1.0.0", "v1.0.1", true)]
        [InlineData("v1.0.0", "", false)]
        public async void CheckNewRelease(string currentVersion, string latestReleaseName, bool isNewRelease)
        {
            using (var mock = AutoMock.GetLoose(
               cfg =>
               {
                   cfg.RegisterType<NewReleaseNotificationService>().As<INewReleaseNotificationService>().SingleInstance();
               }))
            {
                mock.Mock<IReleaseAvailabilityService>().Setup(s => s.GetLatestRelease())
                    .Returns(Task.FromResult(new Release
                    {
                        Name = latestReleaseName,
                        PublishedAt = new DateTime(2021, 11, 27)
                    }));

                mock.Mock<IUserConfigurationService>().Setup(s => s.GetAboutInformation(It.IsAny<Assembly>()))
                    .Returns(new AboutInformation
                    {
                        Version = currentVersion
                    });

                var newReleaseNotificationService = mock.Container.Resolve<INewReleaseNotificationService>();
                var newReleaseResult = await newReleaseNotificationService.CheckNewRelease();

                newReleaseResult.Should().NotBeNull();
                newReleaseResult.Name.Should().Be(latestReleaseName);
                newReleaseResult.PublishedAt.Should().NotBeNull();
                newReleaseResult.PublishedAt.Should().BeAfter(DateTime.MinValue);
                newReleaseResult.PublishedAt.Should().BeBefore(DateTime.UtcNow);
                newReleaseResult.IsNewRelease.Should().Be(isNewRelease);
                newReleaseResult.Success.Should().BeTrue();
            }
        }

        [Fact]
        public async void CheckNewRelease_GetLatestException_ReturnFalse()
        {
            using (var mock = AutoMock.GetLoose(
               cfg =>
               {
                   cfg.RegisterType<NewReleaseNotificationService>().As<INewReleaseNotificationService>().SingleInstance();
               }))
            {
                mock.Mock<IReleaseAvailabilityService>().Setup(s => s.GetLatestRelease())
                    .Throws<Exception>();

                mock.Mock<IUserConfigurationService>().Setup(s => s.GetAboutInformation(It.IsAny<Assembly>()))
                    .Returns(new AboutInformation
                    {
                        Version = "v1.0.0"
                    });

                var newReleaseNotificationService = mock.Container.Resolve<INewReleaseNotificationService>();
                var newReleaseResult = await newReleaseNotificationService.CheckNewRelease();

                newReleaseResult.Should().NotBeNull();
                newReleaseResult.Name.Should().BeNullOrEmpty();
                newReleaseResult.IsNewRelease.Should().BeFalse();
                newReleaseResult.Success.Should().BeFalse();
            }
        }
    }
}
