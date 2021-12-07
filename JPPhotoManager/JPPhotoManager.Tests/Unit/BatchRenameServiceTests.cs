using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;
using Moq;
using Xunit;

namespace JPPhotoManager.Tests.Unit
{
    public class BatchRenameServiceTests
    {
        private string directory = @"C:\My Images\My Folder";
        private Asset[] sourceAssets;

        public BatchRenameServiceTests()
        {
            sourceAssets = new Asset[]
            {
                new Asset
                {
                    FileName = "MyFirstImage.jpg",
                    Folder = new Folder { Path = directory }
                },
                new Asset
                {
                    FileName = "MySecondImage.jpg",
                    Folder = new Folder { Path = directory }
                },
                new Asset
                {
                    FileName = "MyThirdImage.jpg",
                    Folder = new Folder { Path = directory }
                }
            };
        }

        [Fact]
        public void BatchRename_ReturnValidResult()
        {
            using var mock = AutoMock.GetLoose();
            mock.Mock<IStorageService>()
                .Setup(s => s.MoveImage(It.IsAny<string>(), It.IsAny<string>()))
                .Returns(true);
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(sourceAssets, "Image_<##>.jpg");

            renameResult.SourceAssets.Should().HaveCount(sourceAssets.Length);
            renameResult.SourceAssets[0].Should().Be(sourceAssets[0]);
            renameResult.SourceAssets[1].Should().Be(sourceAssets[1]);
            renameResult.SourceAssets[2].Should().Be(sourceAssets[2]);

            renameResult.TargetFileNames.Should().HaveCount(sourceAssets.Length);
            renameResult.TargetFileNames[0].Should().Be("Image_01.jpg");
            renameResult.TargetFileNames[1].Should().Be("Image_02.jpg");
            renameResult.TargetFileNames[2].Should().Be("Image_03.jpg");
        }

        [Fact]
        public void BatchRename_CallMoveImage()
        {
            using var mock = AutoMock.GetLoose();
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(sourceAssets, "Image_<##>.jpg");

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\My Folder\Image_01.jpg"), Moq.Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MySecondImage.jpg",
                @"C:\My Images\My Folder\Image_02.jpg"), Moq.Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyThirdImage.jpg",
                @"C:\My Images\My Folder\Image_03.jpg"), Moq.Times.Once);
        }
    }
}
