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
        private readonly string directory = @"C:\My Images\My Folder";
        private readonly Asset[] sourceAssets;

        public BatchRenameServiceTests()
        {
            sourceAssets = new Asset[]
            {
                new Asset
                {
                    FileName = "MyFirstImage.jpg",
                    Folder = new Folder { Path = directory },
                    FileCreationDateTime = DateTime.Parse("2021-12-06T16:25:15")
                },
                new Asset
                {
                    FileName = "MySecondImage.jpg",
                    Folder = new Folder { Path = directory },
                    FileCreationDateTime = DateTime.Parse("2021-12-06T16:30:15")
                },
                new Asset
                {
                    FileName = "MyThirdImage.jpg",
                    Folder = new Folder { Path = directory },
                    FileCreationDateTime = DateTime.Parse("2021-12-06T16:35:15")
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

            renameResult.TargetPaths.Should().HaveCount(sourceAssets.Length);
            renameResult.TargetPaths[0].Should().Be(@"C:\My Images\My Folder\Image_01.jpg");
            renameResult.TargetPaths[1].Should().Be(@"C:\My Images\My Folder\Image_02.jpg");
            renameResult.TargetPaths[2].Should().Be(@"C:\My Images\My Folder\Image_03.jpg");
        }

        [Fact]
        public void BatchRename_ToSameFolder_CallMoveImage()
        {
            using var mock = AutoMock.GetLoose();
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(sourceAssets, "Image_<##>.jpg");

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\My Folder\Image_01.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MySecondImage.jpg",
                @"C:\My Images\My Folder\Image_02.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyThirdImage.jpg",
                @"C:\My Images\My Folder\Image_03.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToSameRelativeSubFolder_CallMoveImage()
        {
            using var mock = AutoMock.GetLoose();
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(sourceAssets, @"<CreationDate>\Image_<##>.jpg");

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\My Folder\20211206\Image_01.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MySecondImage.jpg",
                @"C:\My Images\My Folder\20211206\Image_02.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyThirdImage.jpg",
                @"C:\My Images\My Folder\20211206\Image_03.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToDifferentRelativeSubFolders_CallMoveImage()
        {
            using var mock = AutoMock.GetLoose();
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(sourceAssets, @"<CreationDate>-<CreationTime>\Image_<##>.jpg");

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\My Folder\20211206-162515\Image_01.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MySecondImage.jpg",
                @"C:\My Images\My Folder\20211206-163015\Image_02.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyThirdImage.jpg",
                @"C:\My Images\My Folder\20211206-163515\Image_03.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToParentRelativeFolder_CallMoveImage()
        {
            using var mock = AutoMock.GetLoose();
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(sourceAssets, @"..\Image_<##>.jpg");

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\Image_01.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MySecondImage.jpg",
                @"C:\My Images\Image_02.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyThirdImage.jpg",
                @"C:\My Images\Image_03.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToGrandParentRelativeFolder_CallMoveImage()
        {
            using var mock = AutoMock.GetLoose();
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(sourceAssets, @"..\..\Image_<##>.jpg");

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\Image_01.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MySecondImage.jpg",
                @"C:\Image_02.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyThirdImage.jpg",
                @"C:\Image_03.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToGrandGrandParentNotExistingRelativeFolder_DontCallMoveImage()
        {
            using var mock = AutoMock.GetLoose();
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(sourceAssets, @"..\..\..\Image_<##>.jpg");

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(It.IsAny<string>(), It.IsAny<string>()), Times.Never);
        }

        [Fact]
        public void BatchRename_ToSiblingRelativeFolder_CallMoveImage()
        {
            using var mock = AutoMock.GetLoose();
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(sourceAssets, @"..\<CreationDate>\Image_<##>.jpg");

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\20211206\Image_01.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MySecondImage.jpg",
                @"C:\My Images\20211206\Image_02.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyThirdImage.jpg",
                @"C:\My Images\20211206\Image_03.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToSameAbsoluteDriveFolder_CallMoveImage()
        {
            using var mock = AutoMock.GetLoose();
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(sourceAssets, @"D:\OtherFolder\Image_<##>.jpg");

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"D:\OtherFolder\Image_01.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MySecondImage.jpg",
                @"D:\OtherFolder\Image_02.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyThirdImage.jpg",
                @"D:\OtherFolder\Image_03.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToSameAbsoluteNetworkFolder_CallMoveImage()
        {
            using var mock = AutoMock.GetLoose();
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(sourceAssets, @"\\OtherFolder\Image_<##>.jpg");

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"\\OtherFolder\Image_01.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MySecondImage.jpg",
                @"\\OtherFolder\Image_02.jpg"), Times.Once);
            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyThirdImage.jpg",
                @"\\OtherFolder\Image_03.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToExistingDestinationFilenameOverwriteExisting_CallMoveImage()
        {
            throw new NotImplementedException("Write test");
        }

        [Fact]
        public void BatchRename_ToExistingDestinationFilenameDontOverwriteExisting_CallMoveImage()
        {
            throw new NotImplementedException("Write test");
        }
    }
}
