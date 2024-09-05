using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain.Interfaces.Services;
using JPPhotoManager.Domain.Services;
using Xunit;

namespace JPPhotoManager.Tests.Unit.Domain.Services
{
    public class UniqueFileNameProviderServiceTests
    {
        [Fact]
        public void GetUniqueFileName_WithNoExistingFileName_ReturnFileName()
        {
            string destinationDirectory = @"C:\Images\MyGame";
            string destinationFileName = "MyFile.jpg";

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                });

            UniqueFileNameProviderService uniqueFileNameProviderService = mock.Container.Resolve<UniqueFileNameProviderService>();

            string result = uniqueFileNameProviderService.GetUniqueDestinationPath(destinationDirectory, destinationFileName);

            result.Should().Be("MyFile.jpg");
        }

        [Fact]
        public void GetUniqueFileName_WithExistingFileNameAndNoExistingSequence_ReturnUniqueFileName()
        {
            string destinationDirectory = @"C:\Images\MyGame";
            string destinationFileName = "MyFile.jpg";

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                });

            mock.Mock<IStorageService>().Setup(s => s.FileExists(@"C:\Images\MyGame\MyFile.jpg")).Returns(true);

            UniqueFileNameProviderService uniqueFileNameProviderService = mock.Container.Resolve<UniqueFileNameProviderService>();

            string result = uniqueFileNameProviderService.GetUniqueDestinationPath(destinationDirectory, destinationFileName);

            result.Should().Be("MyFile_1.jpg");
        }

        [Fact]
        public void GetUniqueFileName_WithExistingFileNameAndExistingSequenceWithoutGaps_ReturnUniqueFileName()
        {
            string destinationDirectory = @"C:\Images\MyGame";
            string destinationFileName = "MyFile.jpg";

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                });

            mock.Mock<IStorageService>().Setup(s => s.FileExists(@"C:\Images\MyGame\MyFile.jpg")).Returns(true);
            mock.Mock<IStorageService>().Setup(s => s.FileExists(@"C:\Images\MyGame\MyFile_1.jpg")).Returns(true);
            mock.Mock<IStorageService>().Setup(s => s.FileExists(@"C:\Images\MyGame\MyFile_2.jpg")).Returns(true);
            mock.Mock<IStorageService>().Setup(s => s.FileExists(@"C:\Images\MyGame\MyFile_3.jpg")).Returns(true);

            UniqueFileNameProviderService uniqueFileNameProviderService = mock.Container.Resolve<UniqueFileNameProviderService>();

            string result = uniqueFileNameProviderService.GetUniqueDestinationPath(destinationDirectory, destinationFileName);

            result.Should().Be("MyFile_4.jpg");
        }

        [Fact]
        public void GetUniqueFileName_WithExistingFileNameAndExistingSequenceWithGaps_ReturnUniqueFileName()
        {
            string destinationDirectory = @"C:\Images\MyGame";
            string destinationFileName = "MyFile.jpg";

            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                });

            mock.Mock<IStorageService>().Setup(s => s.FileExists(@"C:\Images\MyGame\MyFile.jpg")).Returns(true);
            mock.Mock<IStorageService>().Setup(s => s.FileExists(@"C:\Images\MyGame\MyFile_1.jpg")).Returns(true);
            mock.Mock<IStorageService>().Setup(s => s.FileExists(@"C:\Images\MyGame\MyFile_2.jpg")).Returns(false);
            mock.Mock<IStorageService>().Setup(s => s.FileExists(@"C:\Images\MyGame\MyFile_3.jpg")).Returns(true);

            UniqueFileNameProviderService uniqueFileNameProviderService = mock.Container.Resolve<UniqueFileNameProviderService>();

            string result = uniqueFileNameProviderService.GetUniqueDestinationPath(destinationDirectory, destinationFileName);

            result.Should().Be("MyFile_2.jpg");
        }
    }
}
