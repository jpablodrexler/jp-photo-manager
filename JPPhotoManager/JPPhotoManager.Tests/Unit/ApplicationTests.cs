using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;
using Moq;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests.Unit
{
    public class ApplicationTests
    {
        private readonly string _dataDirectory;

        public ApplicationTests()
        {
            _dataDirectory = Path.GetDirectoryName(typeof(ApplicationTests).Assembly.Location);
            _dataDirectory = Path.Combine(_dataDirectory, "TestFiles");
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
            mock.Mock<IFolderRepository>().Setup(m => m.GetFolderByPath(directory)).Returns(new Folder { Path = directory });
            mock.Mock<IAssetRepository>().Setup(m => m.GetAssets(It.Is<Folder>(f => f.Path == directory), 0)).Returns(new PaginatedData<Asset> { Items = expectedResult });
            mock.Mock<IAssetRepository>().Setup(m => m.GetAssetsByFolderId(It.IsAny<int>())).Returns(expectedResult.ToList());

            var app = mock.Container.Resolve<Application.Application>();

            Asset[] assets = app.GetAssets(directory, 0).Items;
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

            Func<PaginatedData<Asset>> function = () => app.GetAssets(directory, 0);
            function.Should().Throw<ArgumentException>();
        }
    }
}
