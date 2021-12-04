using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests.Unit
{
    public class ApplicationTests
    {
        private readonly string dataDirectory;

        public ApplicationTests()
        {
            dataDirectory = Path.GetDirectoryName(typeof(ApplicationTests).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");
        }

        [Fact]
        public void GetAssets_ValidDirectory_ReturnAssetsArray()
        {
            string directory = @"D:\Imágenes";
            Asset[] expectedResult = new Asset[]
            {
                new Asset
                {
                    FileName = "dbzrrou-1d391dff-a336-4395-81a5-885a98685d93.jpg",
                    Folder = new Folder { Path = @"D:\Imágenes\" }
                },
                new Asset
                {
                    FileName = "dbxb0an-d90d6335-7b9c-4a7b-84aa-71501c73f63b.jpg",
                    Folder = new Folder { Path = @"D:\Imágenes\" }
                }
            };

            using var mock = AutoMock.GetLoose();
            mock.Mock<IAssetRepository>().Setup(m => m.GetAssets(directory)).Returns(expectedResult);

            var app = mock.Container.Resolve<Application.Application>();

            Asset[] assets = app.GetAssets(directory);
            assets.Should().BeEquivalentTo(expectedResult);

            mock.Mock<IAssetRepository>().VerifyAll();
        }

        [Theory]
        [InlineData(null)]
        [InlineData("")]
        [InlineData(" ")]
        public void GetAssets_InvalidDirectory_Test(string directory)
        {
            using var mock = AutoMock.GetLoose();
            var app = mock.Container.Resolve<Application.Application>();

            Func<Asset[]> function = () => app.GetAssets(directory);
            function.Should().Throw<ArgumentException>();
        }
    }
}
