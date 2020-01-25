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

            Assert.True(asset1.Equals(asset2));
            Assert.Equal(asset1.GetHashCode(), asset2.GetHashCode());
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

            Assert.False(asset1.Equals(asset2));
            Assert.NotEqual(asset1.GetHashCode(), asset2.GetHashCode());
        }

        [Fact]
        public void OnlyOneFileNameNonEqualTest()
        {
            Asset asset1 = new Asset
            {
                FileName = "Image1.jpg"
            };

            Asset asset2 = new Asset();

            Assert.False(asset1.Equals(asset2));
            Assert.NotEqual(asset1.GetHashCode(), asset2.GetHashCode());
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

            Assert.False(asset1.Equals(asset2));
            Assert.NotEqual(asset1.GetHashCode(), asset2.GetHashCode());
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

            Assert.False(asset1.Equals(asset2));
            Assert.NotEqual(asset1.GetHashCode(), asset2.GetHashCode());
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

            Assert.False(asset1.Equals(asset2));
            Assert.NotEqual(asset1.GetHashCode(), asset2.GetHashCode());
        }

        [Fact]
        public void OnlyOneObjectNonEqualTest()
        {
            Asset asset = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            Assert.False(asset.Equals(null));
        }

        [Fact]
        public void OneObjectComparedToItselfEqualTest()
        {
            Asset asset = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            Assert.True(asset.Equals(asset));
        }

        [Fact]
        public void ToStringWithFileNameTest()
        {
            Asset asset = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = "Image1.jpg"
            };

            Assert.Equal("Image1.jpg", asset.ToString());
        }

        [Fact]
        public void ToStringWithoutFileNameTest()
        {
            Asset asset1 = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = null
            };

            Assert.Null(asset1.ToString());

            Asset asset2 = new Asset
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                FileName = ""
            };

            Assert.Equal("", asset2.ToString());
        }
    }
}
