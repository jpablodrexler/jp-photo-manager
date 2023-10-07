using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Domain.Interfaces.Services;
using JPPhotoManager.Domain.Services;
using Moq;
using Xunit;

namespace JPPhotoManager.Tests.Unit.Domain.Services
{
    public class BatchRenameServiceTests
    {
        private readonly string _directory = @"C:\My Images\My Folder";
        private readonly Asset[] _sourceAssets;

        public BatchRenameServiceTests()
        {
            _sourceAssets = new Asset[]
            {
                new Asset
                {
                    FileName = "MyFirstImage.jpg",
                    Folder = new Folder { Path = _directory },
                    FileCreationDateTime = DateTime.Parse("2021-12-06T16:25:15")
                },
                new Asset
                {
                    FileName = "MySecondImage.jpg",
                    Folder = new Folder { Path = _directory },
                    FileCreationDateTime = DateTime.Parse("2021-12-06T16:30:15")
                },
                new Asset
                {
                    FileName = "MyThirdImage.jpg",
                    Folder = new Folder { Path = _directory },
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
            var renameResult = service.BatchRename(_sourceAssets, "Image_<##>.jpg", false);

            renameResult.SourceAssets.Should().HaveCount(_sourceAssets.Length);
            renameResult.SourceAssets[0].Should().Be(_sourceAssets[0]);
            renameResult.SourceAssets[1].Should().Be(_sourceAssets[1]);
            renameResult.SourceAssets[2].Should().Be(_sourceAssets[2]);

            renameResult.TargetPaths.Should().HaveCount(_sourceAssets.Length);
            renameResult.TargetPaths[0].Should().Be(@"C:\My Images\My Folder\Image_01.jpg");
            renameResult.TargetPaths[1].Should().Be(@"C:\My Images\My Folder\Image_02.jpg");
            renameResult.TargetPaths[2].Should().Be(@"C:\My Images\My Folder\Image_03.jpg");
        }

        [Fact]
        public void BatchRename_ToSameFolder_CallMoveImage()
        {
            using var mock = AutoMock.GetLoose();
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(_sourceAssets, "Image_<##>.jpg", false);

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
            var renameResult = service.BatchRename(_sourceAssets, @"<CreationDate>\Image_<##>.jpg", false);

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
            var renameResult = service.BatchRename(_sourceAssets, @"<CreationDate>-<CreationTime>\Image_<##>.jpg", false);

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
            var renameResult = service.BatchRename(_sourceAssets, @"..\Image_<##>.jpg", false);

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
            var renameResult = service.BatchRename(_sourceAssets, @"..\..\Image_<##>.jpg", false);

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
            var renameResult = service.BatchRename(_sourceAssets, @"..\..\..\Image_<##>.jpg", false);

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(It.IsAny<string>(), It.IsAny<string>()), Times.Never);
        }

        [Fact]
        public void BatchRename_ToSiblingRelativeFolder_CallMoveImage()
        {
            using var mock = AutoMock.GetLoose();
            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(_sourceAssets, @"..\<CreationDate>\Image_<##>.jpg", false);

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
            var renameResult = service.BatchRename(_sourceAssets, @"D:\OtherFolder\Image_<##>.jpg", false);

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
            var renameResult = service.BatchRename(_sourceAssets, @"\\OtherFolder\Image_<##>.jpg", false);

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
        public void BatchRename_ToExistingTargetPathOverwriteExisting_CallMoveImageWithSameTargetPath()
        {
            using var mock = AutoMock.GetLoose();

            mock.Mock<IStorageService>()
                .Setup(s => s.FileExists(@"C:\My Images\20211206\Image_01.jpg"))
                .Returns(true);

            mock.Mock<IStorageService>()
                .Setup(s => s.GetFileNames(It.IsAny<string>()))
                .Returns(new string[]
                {
                    @"C:\My Images\20211206\Image_01.jpg",
                    @"C:\My Images\20211206\Image_02.jpg",
                    @"C:\My Images\20211206\Image_03.jpg",
                    @"C:\My Images\20211206\Image_04.jpg",
                    @"C:\My Images\20211206\Image_05.jpg",
                    @"C:\My Images\20211206\Image_06.jpg",
                    @"C:\My Images\20211206\Image_07.jpg"
                });

            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(_sourceAssets, @"..\<CreationDate>\Image_<##>.jpg", true);

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\20211206\Image_01.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToExistingTargetPathDontOverwriteExisting_WithNoLeadingZero_CallMoveImageWithUniqueTargetPath()
        {
            using var mock = AutoMock.GetLoose();

            mock.Mock<IStorageService>()
                .Setup(s => s.FileExists(@"C:\My Images\20211206\Image_1.jpg"))
                .Returns(true);

            mock.Mock<IStorageService>()
                .Setup(s => s.GetFileNames(It.IsAny<string>()))
                .Returns(new string[]
                {
                    @"C:\My Images\20211206\Image_1.jpg",
                    @"C:\My Images\20211206\Image_2.jpg",
                    @"C:\My Images\20211206\Image_3.jpg",
                    @"C:\My Images\20211206\Image_4.jpg",
                    @"C:\My Images\20211206\Image_5.jpg"
                });

            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(_sourceAssets, @"..\<CreationDate>\Image_<#>.jpg", false);

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\20211206\Image_6.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToExistingTargetPathDontOverwriteExisting_WithOneLeadingZeroFullSequence_CallMoveImageWithUniqueTargetPath()
        {
            using var mock = AutoMock.GetLoose();

            mock.Mock<IStorageService>()
                .Setup(s => s.FileExists(@"C:\My Images\20211206\Image_01.jpg"))
                .Returns(true);

            mock.Mock<IStorageService>()
                .Setup(s => s.GetFileNames(It.IsAny<string>()))
                .Returns(new string[]
                {
                    @"C:\My Images\20211206\Image_01.jpg",
                    @"C:\My Images\20211206\Image_02.jpg",
                    @"C:\My Images\20211206\Image_03.jpg",
                    @"C:\My Images\20211206\Image_04.jpg",
                    @"C:\My Images\20211206\Image_05.jpg"
                });

            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(_sourceAssets, @"..\<CreationDate>\Image_<##>.jpg", false);

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\20211206\Image_06.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToExistingTargetPathDontOverwriteExisting_WithTwoLeadingZeroFullSequence_CallMoveImageWithUniqueTargetPath()
        {
            using var mock = AutoMock.GetLoose();

            mock.Mock<IStorageService>()
                .Setup(s => s.FileExists(@"C:\My Images\20211206\Image_001.jpg"))
                .Returns(true);

            mock.Mock<IStorageService>()
                .Setup(s => s.GetFileNames(It.IsAny<string>()))
                .Returns(new string[]
                {
                    @"C:\My Images\20211206\Image_001.jpg",
                    @"C:\My Images\20211206\Image_002.jpg",
                    @"C:\My Images\20211206\Image_003.jpg",
                    @"C:\My Images\20211206\Image_004.jpg",
                    @"C:\My Images\20211206\Image_005.jpg"
                });

            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(_sourceAssets, @"..\<CreationDate>\Image_<###>.jpg", false);

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\20211206\Image_006.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToExistingTargetPathDontOverwriteExisting_WithTwoLeadingZeroBrokenSequenceFromOne_CallMoveImageWithUniqueTargetPath()
        {
            using var mock = AutoMock.GetLoose();

            mock.Mock<IStorageService>()
                .Setup(s => s.FileExists(@"C:\My Images\20211206\Image_001.jpg"))
                .Returns(true);

            mock.Mock<IStorageService>()
                .Setup(s => s.GetFileNames(It.IsAny<string>()))
                .Returns(new string[]
                {
                    @"C:\My Images\20211206\Image_001.jpg",
                    @"C:\My Images\20211206\Image_002.jpg",
                    @"C:\My Images\20211206\Image_004.jpg",
                    @"C:\My Images\20211206\Image_005.jpg"
                });

            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(_sourceAssets, @"..\<CreationDate>\Image_<###>.jpg", false);

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\20211206\Image_003.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToExistingTargetPathDontOverwriteExisting_WithTwoLeadingZeroBrokenSequenceFromFive_CallMoveImageWithUniqueTargetPath()
        {
            using var mock = AutoMock.GetLoose();

            mock.Mock<IStorageService>()
                .Setup(s => s.GetFileNames(It.IsAny<string>()))
                .Returns(new string[]
                {
                    @"C:\My Images\20211206\Image_005.jpg",
                    @"C:\My Images\20211206\Image_006.jpg",
                    @"C:\My Images\20211206\Image_007.jpg",
                    @"C:\My Images\20211206\Image_008.jpg"
                });

            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(_sourceAssets, @"..\<CreationDate>\Image_<###>.jpg", false);

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\20211206\Image_001.jpg"), Times.Once);
        }

        [Fact]
        public void BatchRename_ToExistingTargetPathDontOverwriteExisting_WithNoOrdinal_CallMoveImageWithUniqueTargetPath()
        {
            using var mock = AutoMock.GetLoose();

            mock.Mock<IStorageService>()
                .Setup(s => s.FileExists(@"C:\My Images\Image_20211206.jpg"))
                .Returns(true);

            mock.Mock<IStorageService>()
                .Setup(s => s.GetFileNames(It.IsAny<string>()))
                .Returns(new string[]
                {
                    @"C:\My Images\Image_20211206.jpg"
                });

            var service = mock.Container.Resolve<BatchRenameService>();
            var renameResult = service.BatchRename(_sourceAssets, @"..\Image_<CreationDate>.jpg", false);

            mock.Mock<IStorageService>()
                .Verify(s => s.MoveImage(@"C:\My Images\My Folder\MyFirstImage.jpg",
                @"C:\My Images\Image_20211206_1.jpg"), Times.Once);
        }
    }
}
