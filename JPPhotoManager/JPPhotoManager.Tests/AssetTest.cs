using FluentAssertions;
using JPPhotoManager.Domain;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class AssetTest
    {
        [Fact]
        public void SameFileAndFolderEqualTest()
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
        public void SameFileWithDifferentFolderNonEqualTest()
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
        public void OnlyOneFileNameNonEqualTest()
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
        public void DifferentFilesWithoutFolderNonEqualTest()
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
        public void DifferentFilesAndFoldersNonEqualTest()
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
        public void SameFileAndOnlyOneFolderNonEqualTest()
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
        public void OnlyOneObjectNonEqualTest()
        {
            Asset asset = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            asset.Should().NotBe(null);
        }

        [Fact]
        public void OneObjectComparedToItselfEqualTest()
        {
            Asset asset = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            asset.Should().Be(asset);
        }

        [Fact]
        public void ToStringWithFileNameTest()
        {
            Asset asset = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            asset.ToString().Should().Be("Image1.jpg");
        }

        [Fact]
        public void ToStringWithoutFileNameTest()
        {
            Asset asset1 = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = null
            };

            asset1.ToString().Should().BeNull();

            Asset asset2 = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = ""
            };

            asset2.ToString().Should().Be("");
        }
    }
}
