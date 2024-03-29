﻿using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Domain.Interfaces.Services;
using System.Globalization;
using Xunit;

namespace JPPhotoManager.Tests.Unit.Domain.Entities
{
    public class AssetTests
    {
        [Fact]
        public void GetHashCode_SameFileAndFolder_AreEqual()
        {
            Asset asset1 = new()
            {
                FolderId = 1,
                FileName = "Image1.jpg"
            };

            Asset asset2 = new()
            {
                FolderId = 1,
                FileName = "Image1.jpg"
            };

            asset1.Should().Be(asset2);
            asset1.GetHashCode().Should().Be(asset2.GetHashCode());
        }

        [Fact]
        public void GetHashCode_SameFileWithDifferentFolder_AreNotEqual()
        {
            Asset asset1 = new()
            {
                FolderId = 1,
                FileName = "Image1.jpg"
            };

            Asset asset2 = new()
            {
                FolderId = 2,
                FileName = "Image1.jpg"
            };

            asset1.Should().NotBe(asset2);
            asset1.GetHashCode().Should().NotBe(asset2.GetHashCode());
        }

        [Fact]
        public void GetHashCode_OnlyOneFileName_AreNotEqual()
        {
            Asset asset1 = new()
            {
                FileName = "Image1.jpg"
            };

            Asset asset2 = new();

            asset1.Should().NotBe(asset2);
            asset1.GetHashCode().Should().NotBe(asset2.GetHashCode());
        }

        [Fact]
        public void GetHashCode_DifferentFilesWithoutFolder_AreNotEqual()
        {
            Asset asset1 = new()
            {
                FileName = "Image1.jpg"
            };

            Asset asset2 = new()
            {
                FileName = "Image2.jpg"
            };

            asset1.Should().NotBe(asset2);
            asset1.GetHashCode().Should().NotBe(asset2.GetHashCode());
        }

        [Fact]
        public void GetHashCode_DifferentFilesAndFolders_AreNotEqual()
        {
            Asset asset1 = new()
            {
                FolderId = 1,
                FileName = "Image1.jpg"
            };

            Asset asset2 = new()
            {
                FolderId = 2,
                FileName = "Image2.jpg"
            };

            asset1.Should().NotBe(asset2);
            asset1.GetHashCode().Should().NotBe(asset2.GetHashCode());
        }

        [Fact]
        public void GetHashCode_SameFileAndOnlyOneFolder_AreNotEqual()
        {
            Asset asset1 = new()
            {
                FolderId = 1,
                FileName = "Image1.jpg"
            };

            Asset asset2 = new()
            {
                FileName = "Image1.jpg"
            };

            asset1.Should().NotBe(asset2);
            asset1.GetHashCode().Should().NotBe(asset2.GetHashCode());
        }

        [Fact]
        public void Equal_OnlyOneObject_ReturnFalse()
        {
            Asset asset = new()
            {
                FolderId = 1,
                FileName = "Image1.jpg"
            };

            asset.Should().NotBe(null);
        }

        [Fact]
        public void Equal_OneObjectComparedToItself_ReturnTrue()
        {
            Asset asset = new()
            {
                FolderId = 1,
                FileName = "Image1.jpg"
            };

            asset.Should().Be(asset);
        }

        [Theory]
        [InlineData("Image1.jpg", "Image1.jpg")]
        [InlineData("", "")]
        [InlineData(null, null)]
        public void ToString_ReturnFileName(string fileName, string expected)
        {
            Asset asset = new()
            {
                FolderId = 1,
                FileName = fileName
            };

            asset.ToString().Should().Be(expected);
        }

        [Theory]
        [InlineData("MyImage.jpg", null, 1, "")]
        [InlineData("MyImage.jpg", "", 1, "")]
        [InlineData("MyImage.jpg", " ", 1, "")]
        [InlineData("MyImage.jpg", "  ", 1, "")]
        [InlineData("MyImage.jpg", ".jpg", 1, "")]
        [InlineData("MyImage.jpg", "<#>.jpg", 1, @"C:\My Images\My Folder\1.jpg")]
        [InlineData("MyImage.jpg", "<##>.jpg", 1, @"C:\My Images\My Folder\01.jpg")]
        [InlineData("MyImage.jpg", "<###>.jpg", 1, @"C:\My Images\My Folder\001.jpg")]
        [InlineData("MyImage.jpg", "<####>.jpg", 1, @"C:\My Images\My Folder\0001.jpg")]
        [InlineData("MyImage.jpg", "<#####>.jpg", 1, @"C:\My Images\My Folder\00001.jpg")]
        [InlineData("MyImage.jpg", "<######>.jpg", 1, @"C:\My Images\My Folder\000001.jpg")]
        [InlineData("MyImage.jpg", "<#######>.jpg", 1, @"C:\My Images\My Folder\0000001.jpg")]
        [InlineData("MyImage.jpg", "<########>.jpg", 1, @"C:\My Images\My Folder\00000001.jpg")]
        [InlineData("MyImage.jpg", "<#########>.jpg", 1, @"C:\My Images\My Folder\000000001.jpg")]
        [InlineData("MyImage.jpg", "<##########>.jpg", 1, @"C:\My Images\My Folder\0000000001.jpg")]
        [InlineData("MyImage.jpg", "<##>_Image.jpg", 1, @"C:\My Images\My Folder\01_Image.jpg")]
        [InlineData("MyImage.jpg", "Image_<##>.jpg", 1, @"C:\My Images\My Folder\Image_01.jpg")]
        [InlineData("MyImage.jpg", "My_<##>_Image.jpg", 1, @"C:\My Images\My Folder\My_01_Image.jpg")]
        [InlineData("MyImage.jpg", "<#>.jpg", 2, @"C:\My Images\My Folder\2.jpg")]
        [InlineData("MyImage.jpg", "<##>.jpg", 2, @"C:\My Images\My Folder\02.jpg")]
        [InlineData("MyImage.jpg", "<###>.jpg", 2, @"C:\My Images\My Folder\002.jpg")]
        [InlineData("MyImage.jpg", "<####>.jpg", 2, @"C:\My Images\My Folder\0002.jpg")]
        [InlineData("MyImage.jpg", "<#####>.jpg", 2, @"C:\My Images\My Folder\00002.jpg")]
        [InlineData("MyImage.jpg", "<######>.jpg", 2, @"C:\My Images\My Folder\000002.jpg")]
        [InlineData("MyImage.jpg", "<#######>.jpg", 2, @"C:\My Images\My Folder\0000002.jpg")]
        [InlineData("MyImage.jpg", "<########>.jpg", 2, @"C:\My Images\My Folder\00000002.jpg")]
        [InlineData("MyImage.jpg", "<#########>.jpg", 2, @"C:\My Images\My Folder\000000002.jpg")]
        [InlineData("MyImage.jpg", "<##########>.jpg", 2, @"C:\My Images\My Folder\0000000002.jpg")]
        [InlineData("MyImage.jpg", "<##>_Image.jpg", 2, @"C:\My Images\My Folder\02_Image.jpg")]
        [InlineData("MyImage.jpg", "Image_<##>.jpg", 2, @"C:\My Images\My Folder\Image_02.jpg")]
        [InlineData("MyImage.jpg", "My_<##>_Image.jpg", 2, @"C:\My Images\My Folder\My_02_Image.jpg")]
        [InlineData("MyImage.png", ".png", 1, "")]
        [InlineData("MyImage.png", "<#>.png", 1, @"C:\My Images\My Folder\1.png")]
        [InlineData("MyImage.png", "<##>.png", 1, @"C:\My Images\My Folder\01.png")]
        [InlineData("MyImage.png", "<###>.png", 1, @"C:\My Images\My Folder\001.png")]
        [InlineData("MyImage.png", "<####>.png", 1, @"C:\My Images\My Folder\0001.png")]
        [InlineData("MyImage.png", "<#####>.png", 1, @"C:\My Images\My Folder\00001.png")]
        [InlineData("MyImage.png", "<######>.png", 1, @"C:\My Images\My Folder\000001.png")]
        [InlineData("MyImage.jpg", "<#######>.png", 1, @"C:\My Images\My Folder\0000001.png")]
        [InlineData("MyImage.jpg", "<########>.png", 1, @"C:\My Images\My Folder\00000001.png")]
        [InlineData("MyImage.jpg", "<#########>.png", 1, @"C:\My Images\My Folder\000000001.png")]
        [InlineData("MyImage.jpg", "<##########>.png", 1, @"C:\My Images\My Folder\0000000001.png")]
        [InlineData("MyImage.JPG", ".JPG", 1, "")]
        [InlineData("MyImage.JPG", "<#>.JPG", 1, @"C:\My Images\My Folder\1.JPG")]
        [InlineData("MyImage.JPG", "<##>.JPG", 1, @"C:\My Images\My Folder\01.JPG")]
        [InlineData("MyImage.JPG", "<###>.JPG", 1, @"C:\My Images\My Folder\001.JPG")]
        [InlineData("MyImage.JPG", "<####>.JPG", 1, @"C:\My Images\My Folder\0001.JPG")]
        [InlineData("MyImage.JPG", "<#####>.JPG", 1, @"C:\My Images\My Folder\00001.JPG")]
        [InlineData("MyImage.JPG", "<######>.JPG", 1, @"C:\My Images\My Folder\000001.JPG")]
        [InlineData("MyImage.JPG", "<#######>.JPG", 1, @"C:\My Images\My Folder\0000001.JPG")]
        [InlineData("MyImage.JPG", "<########>.JPG", 1, @"C:\My Images\My Folder\00000001.JPG")]
        [InlineData("MyImage.JPG", "<#########>.JPG", 1, @"C:\My Images\My Folder\000000001.JPG")]
        [InlineData("MyImage.JPG", "<##########>.JPG", 1, @"C:\My Images\My Folder\0000000001.JPG")]
        [InlineData("MyImage.jpg", ".jpg ", 1, "")]
        [InlineData("MyImage.jpg", "<#>.jpg ", 1, @"C:\My Images\My Folder\1.jpg")]
        [InlineData("MyImage.jpg", "<##>.jpg ", 1, @"C:\My Images\My Folder\01.jpg")]
        [InlineData("MyImage.jpg", "<###>.jpg ", 1, @"C:\My Images\My Folder\001.jpg")]
        [InlineData("MyImage.jpg", "<####>.jpg ", 1, @"C:\My Images\My Folder\0001.jpg")]
        [InlineData("MyImage.jpg", "<#####>.jpg ", 1, @"C:\My Images\My Folder\00001.jpg")]
        [InlineData("MyImage.jpg", "<######>.jpg ", 1, @"C:\My Images\My Folder\000001.jpg")]
        [InlineData("MyImage.jpg", "<#######>.jpg ", 1, @"C:\My Images\My Folder\0000001.jpg")]
        [InlineData("MyImage.jpg", "<########>.jpg ", 1, @"C:\My Images\My Folder\00000001.jpg")]
        [InlineData("MyImage.jpg", "<#########>.jpg ", 1, @"C:\My Images\My Folder\000000001.jpg")]
        [InlineData("MyImage.jpg", "<##########>.jpg ", 1, @"C:\My Images\My Folder\0000000001.jpg")]
        [InlineData("MyImage.jpg", " .jpg", 1, "")]
        [InlineData("MyImage.jpg", " <#>.jpg", 1, @"C:\My Images\My Folder\1.jpg")]
        [InlineData("MyImage.jpg", " <##>.jpg", 1, @"C:\My Images\My Folder\01.jpg")]
        [InlineData("MyImage.jpg", " <###>.jpg", 1, @"C:\My Images\My Folder\001.jpg")]
        [InlineData("MyImage.jpg", " <####>.jpg", 1, @"C:\My Images\My Folder\0001.jpg")]
        [InlineData("MyImage.jpg", " <#####>.jpg", 1, @"C:\My Images\My Folder\00001.jpg")]
        [InlineData("MyImage.jpg", " <######>.jpg", 1, @"C:\My Images\My Folder\000001.jpg")]
        [InlineData("MyImage.jpg", " <#######>.jpg", 1, @"C:\My Images\My Folder\0000001.jpg")]
        [InlineData("MyImage.jpg", " <########>.jpg", 1, @"C:\My Images\My Folder\00000001.jpg")]
        [InlineData("MyImage.jpg", " <#########>.jpg", 1, @"C:\My Images\My Folder\000000001.jpg")]
        [InlineData("MyImage.jpg", " <##########>.jpg", 1, @"C:\My Images\My Folder\0000000001.jpg")]
        [InlineData("MyImage.jpg", " .jpg ", 1, "")]
        [InlineData("MyImage.jpg", " <#>.jpg ", 1, @"C:\My Images\My Folder\1.jpg")]
        [InlineData("MyImage.jpg", " <##>.jpg ", 1, @"C:\My Images\My Folder\01.jpg")]
        [InlineData("MyImage.jpg", " <###>.jpg ", 1, @"C:\My Images\My Folder\001.jpg")]
        [InlineData("MyImage.jpg", " <####>.jpg ", 1, @"C:\My Images\My Folder\0001.jpg")]
        [InlineData("MyImage.jpg", " <#####>.jpg ", 1, @"C:\My Images\My Folder\00001.jpg")]
        [InlineData("MyImage.jpg", " <######>.jpg ", 1, @"C:\My Images\My Folder\000001.jpg")]
        [InlineData("MyImage.jpg", " <#######>.jpg ", 1, @"C:\My Images\My Folder\0000001.jpg")]
        [InlineData("MyImage.jpg", " <########>.jpg ", 1, @"C:\My Images\My Folder\00000001.jpg")]
        [InlineData("MyImage.jpg", " <#########>.jpg ", 1, @"C:\My Images\My Folder\000000001.jpg")]
        [InlineData("MyImage.jpg", " <##########>.jpg ", 1, @"C:\My Images\My Folder\0000000001.jpg")]
        [InlineData("MyImage.jpg", " <##>.jpg ", 542, @"C:\My Images\My Folder\542.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<PixelWidth>.jpg ", 1, @"C:\My Images\My Folder\MyImage_1920.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<PixelHeight>.jpg ", 1, @"C:\My Images\My Folder\MyImage_1080.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<PixelWidth>x<PixelHeight>.jpg ", 1, @"C:\My Images\My Folder\MyImage_1920x1080.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<#> - <PixelWidth>x<PixelHeight>.jpg ", 1, @"C:\My Images\My Folder\MyImage_1 - 1920x1080.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<#> - <pixelwidth>x<pixelheight>.jpg ", 1, @"C:\My Images\My Folder\MyImage_1 - 1920x1080.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<CreationDate>.jpg ", 1, @"C:\My Images\My Folder\MyImage_20211206.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<CreationTime>.jpg ", 1, @"C:\My Images\My Folder\MyImage_162515.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<CreationDate>-<CreationTime>.jpg ", 1, @"C:\My Images\My Folder\MyImage_20211206-162515.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<#> - <CreationDate>-<CreationTime>.jpg ", 1, @"C:\My Images\My Folder\MyImage_1 - 20211206-162515.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<#> - <creationdate>-<creationtime>.jpg ", 1, @"C:\My Images\My Folder\MyImage_1 - 20211206-162515.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<ModificationDate>.jpg ", 1, @"C:\My Images\My Folder\MyImage_20211210.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<ModificationTime>.jpg ", 1, @"C:\My Images\My Folder\MyImage_213522.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<ModificationDate>-<ModificationTime>.jpg ", 1, @"C:\My Images\My Folder\MyImage_20211210-213522.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<#> - <ModificationDate>-<ModificationTime>.jpg ", 1, @"C:\My Images\My Folder\MyImage_1 - 20211210-213522.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<#> - <modificationdate>-<modificationtime>.jpg ", 1, @"C:\My Images\My Folder\MyImage_1 - 20211210-213522.jpg")]
        [InlineData("MyImage.jpg", "<CreationDate>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\20211206\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<CreationDate:yy>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\21\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<CreationDate:yyyy>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\2021\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<CreationDate:MM>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\12\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<CreationDate:MMMM>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\December\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<CreationDate:dd>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\06\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<CreationDate:yyyy>\\<CreationDate:MM>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\2021\12\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<CreationDate:yyyy>\\<CreationDate:MM>\\<CreationDate:dd>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\2021\12\06\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<ModificationDate>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\20211210\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<ModificationDate:yy>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\21\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<ModificationDate:yyyy>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\2021\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<ModificationDate:MM>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\12\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<ModificationDate:MMMM>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\December\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<ModificationDate:dd>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\10\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<ModificationDate:yyyy>\\<CreationDate:MM>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\2021\12\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<ModificationDate:yyyy>\\<CreationDate:MM>\\<CreationDate:dd>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\2021\12\06\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<CreationTime:HH>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\16\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<CreationTime:mm>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\25\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<CreationTime:ss>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\15\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<CreationTime>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\162515\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<ModificationTime:HH>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\21\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<ModificationTime:mm>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\35\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<ModificationTime:ss>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\22\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "<ModificationTime>\\MyImage_<#>.jpg", 1, @"C:\My Images\My Folder\213522\MyImage_1.jpg")]
        [InlineData("MyImage.jpg", "..\\Image_<##>.jpg", 1, @"C:\My Images\Image_01.jpg")]
        [InlineData("MyImage.jpg", "..\\..\\Image_<##>.jpg", 1, @"C:\Image_01.jpg")]
        [InlineData("MyImage.jpg", "..\\<CreationDate>\\Image_<##>.jpg", 1, @"C:\My Images\20211206\Image_01.jpg")]
        public void ComputeTargetPath_ReturnTargetPath(
            string existingFileName,
            string batchFormat,
            int ordinal,
            string expectedTargetPath)
        {
            Asset asset = new()
            {
                FileName = existingFileName,
                Folder = new Folder { Path = @"C:\My Images\My Folder" },
                PixelWidth = 1920,
                PixelHeight = 1080,
                FileCreationDateTime = DateTime.Parse("2021-12-06T16:25:15"),
                FileModificationDateTime = DateTime.Parse("2021-12-10T21:35:22")
            };

            using var mock = AutoMock.GetLoose();
            mock.Mock<IStorageService>()
                .Setup(s => s.GetParentDirectory(@"C:\My Images\My Folder"))
                .Returns(@"C:\My Images");
            mock.Mock<IStorageService>()
                .Setup(s => s.GetParentDirectory(@"C:\My Images"))
                .Returns(@"C:\");
            var storageService = mock.Mock<IStorageService>().Object;
            string targetPath = asset.ComputeTargetPath(batchFormat, ordinal, new CultureInfo("en-US"), storageService, false);
            targetPath.Should().Be(expectedTargetPath);
        }

        [Fact]
        public void ComputeTargetPath_ResultLongerThanMaxPathLength_ReturnEmptyTargetPath()
        {
            Asset asset = new()
            {
                FileName = new string('1', 256) + ".jpg",
                Folder = new Folder
                {
                    Path = @"C:\Users\user1\Pictures\Pictures Folder with long name\"
                }
            };

            using var mock = AutoMock.GetLoose();
            var storageService = mock.Mock<IStorageService>().Object;
            string targetPath = asset.ComputeTargetPath($"{asset.FileName}_#.jpg", int.MaxValue, new CultureInfo("en-US"), storageService, false);
            targetPath.Should().BeNullOrEmpty(targetPath);
        }

        [Theory]
        [InlineData(null, false)]
        [InlineData("", false)]
        [InlineData(" ", false)]
        [InlineData("  ", false)]
        [InlineData(".jpg", false)]
        [InlineData("<#>.jpg", true)]
        [InlineData("<##>.jpg", true)]
        [InlineData("<###>.jpg", true)]
        [InlineData("<####>.jpg", true)]
        [InlineData("<#####>.jpg", true)]
        [InlineData("<######>.jpg", true)]
        [InlineData("<#######>.jpg", true)]
        [InlineData("<########>.jpg", true)]
        [InlineData("<#########>.jpg", true)]
        [InlineData("<##########>.jpg", true)]
        [InlineData("<##>_Image.jpg", true)]
        [InlineData("Image_<##>.jpg", true)]
        [InlineData("My_<##>_Image.jpg", true)]
        [InlineData(".png", false)]
        [InlineData("<#>.png", true)]
        [InlineData("<##>.png", true)]
        [InlineData("<###>.png", true)]
        [InlineData("<####>.png", true)]
        [InlineData("<#####>.png", true)]
        [InlineData("<######>.png", true)]
        [InlineData("<#######>.png", true)]
        [InlineData("<########>.png", true)]
        [InlineData("<#########>.png", true)]
        [InlineData("<##########>.png", true)]
        [InlineData(".JPG", false)]
        [InlineData("<#>.JPG", true)]
        [InlineData("<##>.JPG", true)]
        [InlineData("<###>.JPG", true)]
        [InlineData("<####>.JPG", true)]
        [InlineData("<#####>.JPG", true)]
        [InlineData("<######>.JPG", true)]
        [InlineData("<#######>.JPG", true)]
        [InlineData("<########>.JPG", true)]
        [InlineData("<#########>.JPG", true)]
        [InlineData("<##########>.JPG", true)]
        [InlineData(".jpg ", false)]
        [InlineData("<#>.jpg ", true)]
        [InlineData("<##>.jpg ", true)]
        [InlineData("<###>.jpg ", true)]
        [InlineData("<####>.jpg ", true)]
        [InlineData("<#####>.jpg ", true)]
        [InlineData("<######>.jpg ", true)]
        [InlineData("<#######>.jpg ", true)]
        [InlineData("<########>.jpg ", true)]
        [InlineData("<#########>.jpg ", true)]
        [InlineData("<##########>.jpg ", true)]
        [InlineData(" .jpg", false)]
        [InlineData(" <#>.jpg", true)]
        [InlineData(" <##>.jpg", true)]
        [InlineData(" <###>.jpg", true)]
        [InlineData(" <####>.jpg", true)]
        [InlineData(" <#####>.jpg", true)]
        [InlineData(" <######>.jpg", true)]
        [InlineData(" <#######>.jpg", true)]
        [InlineData(" <########>.jpg", true)]
        [InlineData(" <#########>.jpg", true)]
        [InlineData(" <##########>.jpg", true)]
        [InlineData(" .jpg ", false)]
        [InlineData(" <#>.jpg ", true)]
        [InlineData(" <##>.jpg ", true)]
        [InlineData(" <###>.jpg ", true)]
        [InlineData(" <####>.jpg ", true)]
        [InlineData(" <#####>.jpg ", true)]
        [InlineData(" <######>.jpg ", true)]
        [InlineData(" <#######>.jpg ", true)]
        [InlineData(" <########>.jpg ", true)]
        [InlineData(" <#########>.jpg ", true)]
        [InlineData(" <##########>.jpg ", true)]
        [InlineData("MyImage_<PixelWidth>.jpg ", true)]
        [InlineData("MyImage_<PixelHeight>.jpg ", true)]
        [InlineData("MyImage_<PixelWidth>x<PixelHeight>.jpg ", true)]
        [InlineData("MyImage_<#> - <PixelWidth>x<PixelHeight>.jpg ", true)]
        [InlineData("MyImage_<#> - <pixelwidth>x<pixelheight>.jpg ", true)]
        [InlineData("MyImage_<CreationDate>.jpg ", true)]
        [InlineData("MyImage_<CreationTime>.jpg ", true)]
        [InlineData("MyImage_<CreationDate>-<CreationTime>.jpg ", true)]
        [InlineData("MyImage_<#> - <CreationDate>-<CreationTime>.jpg ", true)]
        [InlineData("MyImage_<#> - <creationdate>-<creationtime>.jpg ", true)]
        [InlineData("MyImage_<ModificationDate>.jpg ", true)]
        [InlineData("MyImage_<ModificationTime>.jpg ", true)]
        [InlineData("MyImage_<ModificationDate>-<ModificationTime>.jpg ", true)]
        [InlineData("MyImage_<#> - <ModificationDate>-<ModificationTime>.jpg ", true)]
        [InlineData("MyImage_<#> - <modificationdate>-<modificationtime>.jpg ", true)]
        [InlineData("MyImage_<", false)]
        [InlineData("MyImage_>", false)]
        [InlineData("MyImage_<>", false)]
        [InlineData("MyImage_<><", false)]
        [InlineData("MyImage_<CreationDate><", false)]
        [InlineData("MyImage_<Whatever>", false)]
        [InlineData("MyImage_<.jpg", false)]
        [InlineData("MyImage_>.jpg", false)]
        [InlineData("MyImage_<>.jpg", false)]
        [InlineData("MyImage_<><.jpg", false)]
        [InlineData("MyImage_<CreationDate><.jpg", false)]
        [InlineData("MyImage_<CreationDate><>.jpg", false)]
        [InlineData("MyImage_<Whatever>.jpg", false)]
        [InlineData("MyImage_<CreationDate><Whatever>.jpg", false)]
        [InlineData("MyImage_<CreationDate><Whatever><.jpg", false)]
        [InlineData("MyImage_><CreationDate><Whatever><.jpg", false)]
        [InlineData("MyImage_><><CreationDate><Whatever><><.jpg", false)]
        [InlineData("MyImage_>><CreationDate><Whatever>>.jpg", false)]
        [InlineData("MyImage\\<CreationDate>.jpg", true)]
        [InlineData("MyImage/<CreationDate>.jpg", false)]
        [InlineData("MyImage:<CreationDate>.jpg", false)]
        [InlineData("MyImage*<CreationDate>.jpg", false)]
        [InlineData("MyImage?<CreationDate>.jpg", false)]
        [InlineData("MyImage\"<CreationDate>.jpg", false)]
        [InlineData("MyImage<<CreationDate>.jpg", false)]
        [InlineData("MyImage><CreationDate>.jpg", false)]
        [InlineData("MyImage|<CreationDate>.jpg", false)]
        [InlineData("MyImage_#..", false)]
        [InlineData("MyImage_#_..", false)]
        [InlineData("<CreationDate>\\MyImage_#.jpg", false)]
        [InlineData("<CreationDate:yyyy>\\MyImage_#.jpg", false)]
        [InlineData("<CreationDate:yy>\\MyImage_#.jpg", false)]
        [InlineData("<CreationDate:M>\\MyImage_#.jpg", false)]
        [InlineData("<CreationDate:MM>\\MyImage_#.jpg", false)]
        [InlineData("<CreationDate:MMMM>\\MyImage_#.jpg", false)]
        [InlineData("<CreationDate:d>\\MyImage_#.jpg", false)]
        [InlineData("<CreationDate:dd>\\MyImage_#.jpg", false)]
        [InlineData("<CreationDate:yyyy>\\<CreationDate:MM>\\MyImage_#.jpg", false)]
        [InlineData("<CreationDate:yyyy>\\<CreationDate:MM>\\<CreationDate:dd>\\MyImage_#.jpg", false)]
        [InlineData("<ModificationDate>\\MyImage_#.jpg", false)]
        [InlineData("<ModificationDate:yy>\\MyImage_#.jpg", false)]
        [InlineData("<ModificationDate:yyyy>\\MyImage_#.jpg", false)]
        [InlineData("<ModificationDate:MM>\\MyImage_#.jpg", false)]
        [InlineData("<ModificationDate:MMMM>\\MyImage_#.jpg", false)]
        [InlineData("<ModificationDate:d>\\MyImage_#.jpg", false)]
        [InlineData("<ModificationDate:dd>\\MyImage_#.jpg", false)]
        [InlineData("<ModificationDate:yyyy>\\<CreationDate:MM>\\MyImage_#.jpg", false)]
        [InlineData("<ModificationDate:yyyy>\\<CreationDate:MM>\\<CreationDate:dd>\\MyImage_#.jpg", false)]
        [InlineData("<CreationDate>\\MyImage_<#>.jpg", true)]
        [InlineData("<CreationDate:y>\\MyImage_<#>.jpg", false)]
        [InlineData("<CreationDate:yy>\\MyImage_<#>.jpg", true)]
        [InlineData("<CreationDate:yyy>\\MyImage_<#>.jpg", false)]
        [InlineData("<CreationDate:yyyy>\\MyImage_<#>.jpg", true)]
        [InlineData("<CreationDate:M>\\MyImage_<#>.jpg", false)]
        [InlineData("<CreationDate:MM>\\MyImage_<#>.jpg", true)]
        [InlineData("<CreationDate:MMM>\\MyImage_<#>.jpg", false)]
        [InlineData("<CreationDate:MMMM>\\MyImage_<#>.jpg", true)]
        [InlineData("<CreationDate:d>\\MyImage_<#>.jpg", false)]
        [InlineData("<CreationDate:dd>\\MyImage_<#>.jpg", true)]
        [InlineData("<CreationDate:yyyy>\\<CreationDate:MM>\\MyImage_<#>.jpg", true)]
        [InlineData("<CreationDate:yyyy>\\<CreationDate:MM>\\<CreationDate:dd>\\MyImage_<#>.jpg", true)]
        [InlineData("<ModificationDate>\\MyImage_<#>.jpg", true)]
        [InlineData("<ModificationDate:y>\\MyImage_<#>.jpg", false)]
        [InlineData("<ModificationDate:yy>\\MyImage_<#>.jpg", true)]
        [InlineData("<ModificationDate:yyy>\\MyImage_<#>.jpg", false)]
        [InlineData("<ModificationDate:yyyy>\\MyImage_<#>.jpg", true)]
        [InlineData("<ModificationDate:M>\\MyImage_<#>.jpg", false)]
        [InlineData("<ModificationDate:MM>\\MyImage_<#>.jpg", true)]
        [InlineData("<ModificationDate:MMM>\\MyImage_<#>.jpg", false)]
        [InlineData("<ModificationDate:MMMM>\\MyImage_<#>.jpg", true)]
        [InlineData("<ModificationDate:d>\\MyImage_<#>.jpg", false)]
        [InlineData("<ModificationDate:dd>\\MyImage_<#>.jpg", true)]
        [InlineData("<ModificationDate:yyyy>\\<CreationDate:MM>\\MyImage_<#>.jpg", true)]
        [InlineData("<ModificationDate:yyyy>\\<CreationDate:MM>\\<CreationDate:dd>\\MyImage_<#>.jpg", true)]
        [InlineData("..\\Image_<##>.jpg", true)]
        [InlineData("..\\..\\Image_<##>.jpg", true)]
        [InlineData("..\\<CreationDate>\\Image_<##>.jpg", true)]
        [InlineData("D:Image_<##>.jpg", false)]
        [InlineData("D:\\Image_<##>.jpg", true)]
        [InlineData("D:\\OtherFolder\\Image_<##>.jpg", true)]
        public void IsValidBatchFormat_ReturnIsValid(string batchFormat, bool expectedIsValid)
        {
            bool isValid = Asset.IsValidBatchFormat(batchFormat);
            isValid.Should().Be(expectedIsValid);
        }
    }
}
