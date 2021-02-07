using FluentAssertions;
using JPPhotoManager.Domain;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class AssetTest
    {
        [Fact]
        public void GetHashCode_SameFileAndFolder_AreEqual()
        {
            Asset asset1 = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            Asset asset2 = new Asset
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
            Asset asset1 = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            Asset asset2 = new Asset
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
            Asset asset1 = new Asset
            {
                FileName = "Image1.jpg"
            };

            Asset asset2 = new Asset();

            asset1.Should().NotBe(asset2);
            asset1.GetHashCode().Should().NotBe(asset2.GetHashCode());
        }

        [Fact]
        public void GetHashCode_DifferentFilesWithoutFolder_AreNotEqual()
        {
            Asset asset1 = new Asset
            {
                FileName = "Image1.jpg"
            };

            Asset asset2 = new Asset
            {
                FileName = "Image2.jpg"
            };

            asset1.Should().NotBe(asset2);
            asset1.GetHashCode().Should().NotBe(asset2.GetHashCode());
        }

        [Fact]
        public void GetHashCode_DifferentFilesAndFolders_AreNotEqual()
        {
            Asset asset1 = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            Asset asset2 = new Asset
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
            Asset asset1 = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            Asset asset2 = new Asset
            {
                FileName = "Image1.jpg"
            };

            asset1.Should().NotBe(asset2);
            asset1.GetHashCode().Should().NotBe(asset2.GetHashCode());
        }

        [Fact]
        public void Equal_OnlyOneObject_ReturnFalse()
        {
            Asset asset = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            asset.Should().NotBe(null);
        }

        [Fact]
        public void Equal_OneObjectComparedToItself_ReturnTrue()
        {
            Asset asset = new Asset
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
            Asset asset = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = fileName
            };

            asset.ToString().Should().Be(expected);
        }
    }
}
