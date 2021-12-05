using FluentAssertions;
using JPPhotoManager.Domain;
using Xunit;

namespace JPPhotoManager.Tests.Unit
{
    public class AssetTests
    {
        [Fact]
        public void GetHashCode_SameFileAndFolder_AreEqual()
        {
            Asset asset1 = new()
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            Asset asset2 = new()
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
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
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            Asset asset2 = new()
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417285",
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
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            Asset asset2 = new()
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417285",
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
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
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
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            asset.Should().NotBe(null);
        }

        [Fact]
        public void Equal_OneObjectComparedToItself_ReturnTrue()
        {
            Asset asset = new()
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
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
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = fileName
            };

            asset.ToString().Should().Be(expected);
        }

        [Theory]
        [InlineData("MyImage.jpg", null, 1, 1920, 1080, "MyImage.jpg")]
        [InlineData("MyImage.jpg", "", 1, 1920, 1080, "MyImage.jpg")]
        [InlineData("MyImage.jpg", " ", 1, 1920, 1080, "MyImage.jpg")]
        [InlineData("MyImage.jpg", "  ", 1, 1920, 1080, "MyImage.jpg")]
        [InlineData("MyImage.jpg", ".jpg", 1, 1920, 1080, "MyImage.jpg")]
        [InlineData("MyImage.jpg", "<#>.jpg", 1, 1920, 1080, "1.jpg")]
        [InlineData("MyImage.jpg", "<##>.jpg", 1, 1920, 1080, "01.jpg")]
        [InlineData("MyImage.jpg", "<###>.jpg", 1, 1920, 1080, "001.jpg")]
        [InlineData("MyImage.jpg", "<####>.jpg", 1, 1920, 1080, "0001.jpg")]
        [InlineData("MyImage.jpg", "<#####>.jpg", 1, 1920, 1080, "00001.jpg")]
        [InlineData("MyImage.jpg", "<######>.jpg", 1, 1920, 1080, "000001.jpg")]
        [InlineData("MyImage.jpg", "<#######>.jpg", 1, 1920, 1080, "0000001.jpg")]
        [InlineData("MyImage.jpg", "<########>.jpg", 1, 1920, 1080, "00000001.jpg")]
        [InlineData("MyImage.jpg", "<#########>.jpg", 1, 1920, 1080, "000000001.jpg")]
        [InlineData("MyImage.jpg", "<##########>.jpg", 1, 1920, 1080, "0000000001.jpg")]
        [InlineData("MyImage.jpg", "<##>_Image.jpg", 1, 1920, 1080, "01_Image.jpg")]
        [InlineData("MyImage.jpg", "Image_<##>.jpg", 1, 1920, 1080, "Image_01.jpg")]
        [InlineData("MyImage.jpg", "My_<##>_Image.jpg", 1, 1920, 1080, "My_01_Image.jpg")]
        [InlineData("MyImage.jpg", "<#>.jpg", 2, 1920, 1080, "2.jpg")]
        [InlineData("MyImage.jpg", "<##>.jpg", 2, 1920, 1080, "02.jpg")]
        [InlineData("MyImage.jpg", "<###>.jpg", 2, 1920, 1080, "002.jpg")]
        [InlineData("MyImage.jpg", "<####>.jpg", 2, 1920, 1080, "0002.jpg")]
        [InlineData("MyImage.jpg", "<#####>.jpg", 2, 1920, 1080, "00002.jpg")]
        [InlineData("MyImage.jpg", "<######>.jpg", 2, 1920, 1080, "000002.jpg")]
        [InlineData("MyImage.jpg", "<#######>.jpg", 2, 1920, 1080, "0000002.jpg")]
        [InlineData("MyImage.jpg", "<########>.jpg", 2, 1920, 1080, "00000002.jpg")]
        [InlineData("MyImage.jpg", "<#########>.jpg", 2, 1920, 1080, "000000002.jpg")]
        [InlineData("MyImage.jpg", "<##########>.jpg", 2, 1920, 1080, "0000000002.jpg")]
        [InlineData("MyImage.jpg", "<##>_Image.jpg", 2, 1920, 1080, "02_Image.jpg")]
        [InlineData("MyImage.jpg", "Image_<##>.jpg", 2, 1920, 1080, "Image_02.jpg")]
        [InlineData("MyImage.jpg", "My_<##>_Image.jpg", 2, 1920, 1080, "My_02_Image.jpg")]
        [InlineData("MyImage.png", ".png", 1, 1920, 1080, "MyImage.png")]
        [InlineData("MyImage.png", "<#>.png", 1, 1920, 1080, "1.png")]
        [InlineData("MyImage.png", "<##>.png", 1, 1920, 1080, "01.png")]
        [InlineData("MyImage.png", "<###>.png", 1, 1920, 1080, "001.png")]
        [InlineData("MyImage.png", "<####>.png", 1, 1920, 1080, "0001.png")]
        [InlineData("MyImage.png", "<#####>.png", 1, 1920, 1080, "00001.png")]
        [InlineData("MyImage.png", "<######>.png", 1, 1920, 1080, "000001.png")]
        [InlineData("MyImage.jpg", "<#######>.png", 1, 1920, 1080, "0000001.png")]
        [InlineData("MyImage.jpg", "<########>.png", 1, 1920, 1080, "00000001.png")]
        [InlineData("MyImage.jpg", "<#########>.png", 1, 1920, 1080, "000000001.png")]
        [InlineData("MyImage.jpg", "<##########>.png", 1, 1920, 1080, "0000000001.png")]
        [InlineData("MyImage.JPG", ".JPG", 1, 1920, 1080, "MyImage.JPG")]
        [InlineData("MyImage.JPG", "<#>.JPG", 1, 1920, 1080, "1.JPG")]
        [InlineData("MyImage.JPG", "<##>.JPG", 1, 1920, 1080, "01.JPG")]
        [InlineData("MyImage.JPG", "<###>.JPG", 1, 1920, 1080, "001.JPG")]
        [InlineData("MyImage.JPG", "<####>.JPG", 1, 1920, 1080, "0001.JPG")]
        [InlineData("MyImage.JPG", "<#####>.JPG", 1, 1920, 1080, "00001.JPG")]
        [InlineData("MyImage.JPG", "<######>.JPG", 1, 1920, 1080, "000001.JPG")]
        [InlineData("MyImage.JPG", "<#######>.JPG", 1, 1920, 1080, "0000001.JPG")]
        [InlineData("MyImage.JPG", "<########>.JPG", 1, 1920, 1080, "00000001.JPG")]
        [InlineData("MyImage.JPG", "<#########>.JPG", 1, 1920, 1080, "000000001.JPG")]
        [InlineData("MyImage.JPG", "<##########>.JPG", 1, 1920, 1080, "0000000001.JPG")]
        [InlineData("MyImage.jpg", ".jpg ", 1, 1920, 1080, "MyImage.jpg")]
        [InlineData("MyImage.jpg", "<#>.jpg ", 1, 1920, 1080, "1.jpg")]
        [InlineData("MyImage.jpg", "<##>.jpg ", 1, 1920, 1080, "01.jpg")]
        [InlineData("MyImage.jpg", "<###>.jpg ", 1, 1920, 1080, "001.jpg")]
        [InlineData("MyImage.jpg", "<####>.jpg ", 1, 1920, 1080, "0001.jpg")]
        [InlineData("MyImage.jpg", "<#####>.jpg ", 1, 1920, 1080, "00001.jpg")]
        [InlineData("MyImage.jpg", "<######>.jpg ", 1, 1920, 1080, "000001.jpg")]
        [InlineData("MyImage.jpg", "<#######>.jpg ", 1, 1920, 1080, "0000001.jpg")]
        [InlineData("MyImage.jpg", "<########>.jpg ", 1, 1920, 1080, "00000001.jpg")]
        [InlineData("MyImage.jpg", "<#########>.jpg ", 1, 1920, 1080, "000000001.jpg")]
        [InlineData("MyImage.jpg", "<##########>.jpg ", 1, 1920, 1080, "0000000001.jpg")]
        [InlineData("MyImage.jpg", " .jpg", 1, 1920, 1080, "MyImage.jpg")]
        [InlineData("MyImage.jpg", " <#>.jpg", 1, 1920, 1080, "1.jpg")]
        [InlineData("MyImage.jpg", " <##>.jpg", 1, 1920, 1080, "01.jpg")]
        [InlineData("MyImage.jpg", " <###>.jpg", 1, 1920, 1080, "001.jpg")]
        [InlineData("MyImage.jpg", " <####>.jpg", 1, 1920, 1080, "0001.jpg")]
        [InlineData("MyImage.jpg", " <#####>.jpg", 1, 1920, 1080, "00001.jpg")]
        [InlineData("MyImage.jpg", " <######>.jpg", 1, 1920, 1080, "000001.jpg")]
        [InlineData("MyImage.jpg", " <#######>.jpg", 1, 1920, 1080, "0000001.jpg")]
        [InlineData("MyImage.jpg", " <########>.jpg", 1, 1920, 1080, "00000001.jpg")]
        [InlineData("MyImage.jpg", " <#########>.jpg", 1, 1920, 1080, "000000001.jpg")]
        [InlineData("MyImage.jpg", " <##########>.jpg", 1, 1920, 1080, "0000000001.jpg")]
        [InlineData("MyImage.jpg", " .jpg ", 1, 1920, 1080, "MyImage.jpg")]
        [InlineData("MyImage.jpg", " <#>.jpg ", 1, 1920, 1080, "1.jpg")]
        [InlineData("MyImage.jpg", " <##>.jpg ", 1, 1920, 1080, "01.jpg")]
        [InlineData("MyImage.jpg", " <###>.jpg ", 1, 1920, 1080, "001.jpg")]
        [InlineData("MyImage.jpg", " <####>.jpg ", 1, 1920, 1080, "0001.jpg")]
        [InlineData("MyImage.jpg", " <#####>.jpg ", 1, 1920, 1080, "00001.jpg")]
        [InlineData("MyImage.jpg", " <######>.jpg ", 1, 1920, 1080, "000001.jpg")]
        [InlineData("MyImage.jpg", " <#######>.jpg ", 1, 1920, 1080, "0000001.jpg")]
        [InlineData("MyImage.jpg", " <########>.jpg ", 1, 1920, 1080, "00000001.jpg")]
        [InlineData("MyImage.jpg", " <#########>.jpg ", 1, 1920, 1080, "000000001.jpg")]
        [InlineData("MyImage.jpg", " <##########>.jpg ", 1, 1920, 1080, "0000000001.jpg")]
        [InlineData("MyImage.jpg", " <##>.jpg ", 542, 1920, 1080, "542.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<PixelWidth>.jpg ", 1, 1920, 1080, "MyImage_1920.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<PixelHeight>.jpg ", 1, 1920, 1080, "MyImage_1080.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<PixelWidth>x<PixelHeight>.jpg ", 1, 1920, 1080, "MyImage_1920x1080.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<#> - <PixelWidth>x<PixelHeight>.jpg ", 1, 1920, 1080, "MyImage_1 - 1920x1080.jpg")]
        [InlineData("MyImage.jpg", "MyImage_<#> - <pixelwidth>x<pixelheight>.jpg ", 1, 1920, 1080, "MyImage_1 - 1920x1080.jpg")]
        public void ComputeNewName_ReturnNewName(string existingFileName,
            string batchFormat,
            int ordinal,
            int pixelWidth,
            int pixelHeight,
            string expectedNewName)
        {
            Asset asset = new()
            {
                FileName = existingFileName,
                PixelWidth = pixelWidth,
                PixelHeight = pixelHeight
            };

            string newName = asset.ComputeNewName(batchFormat, ordinal);
            newName.Should().Be(expectedNewName);
        }

        [Fact]
        public void ComputeNewName_ResultLongerThanMaxPathLength_ReturnEmptyNewName()
        {
            Asset asset = new()
            {
                FileName = new string('1', 256) + ".jpg",
                Folder = new Folder
                {
                    Path = @"C:\Users\user1\Pictures\Pictures Folder with long name\"
                }
            };

            string newName = asset.ComputeNewName($"{asset.FileName}_#.jpg", int.MaxValue);
            newName.Should().BeNullOrEmpty(newName);
        }
    }
}
