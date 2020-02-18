using FluentAssertions;
using JPPhotoManager.Domain;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class FolderTest
    {
        [Fact]
        public void SameFileAndFolderEqualTest()
        {
            Folder folder1 = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = @"C:\Users\TestUser\Pictures"
            };

            Folder folder2 = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = @"C:\Users\TestUser\Pictures"
            };

            folder1.Should().Be(folder2);
            folder1.GetHashCode().Should().Be(folder2.GetHashCode());
        }

        [Fact]
        public void DifferentPathNonEqualTest()
        {
            Folder folder1 = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = @"C:\Users\TestUser\Pictures"
            };

            Folder folder2 = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = @"C:\Users\TestUser\Pictures\MyAlbum"
            };

            folder1.Should().NotBe(folder2);
            folder1.GetHashCode().Should().NotBe(folder2.GetHashCode());
        }

        [Fact]
        public void OnlyOnePathNonEqualTest()
        {
            Folder folder1 = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = @"C:\Users\TestUser\Pictures"
            };

            Folder folder2 = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = null
            };

            folder1.Should().NotBe(folder2);
            folder1.GetHashCode().Should().NotBe(folder2.GetHashCode());
        }

        [Fact]
        public void OnlyOneObjectNonEqualTest()
        {
            Folder folder = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = @"C:\Users\TestUser\Pictures"
            };

            folder.Should().NotBeNull();
        }

        [Fact]
        public void ToStringWithPathTest()
        {
            Folder folder = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = @"C:\Users\TestUser\Pictures"
            };

            folder.ToString().Should().Be(@"C:\Users\TestUser\Pictures");
        }

        [Fact]
        public void ToStringWithoutPathTest()
        {
            Folder folder1 = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = null
            };

            folder1.ToString().Should().BeNull();

            Folder folder2 = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = ""
            };

            folder2.ToString().Should().Be("");
        }
    }
}
