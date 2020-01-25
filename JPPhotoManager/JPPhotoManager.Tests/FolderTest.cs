using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.Text;
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

            Assert.True(folder1.Equals(folder2));
            Assert.Equal(folder1.GetHashCode(), folder2.GetHashCode());
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

            Assert.False(folder1.Equals(folder2));
            Assert.NotEqual(folder1.GetHashCode(), folder2.GetHashCode());
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

            Assert.False(folder1.Equals(folder2));
            Assert.NotEqual(folder1.GetHashCode(), folder2.GetHashCode());
        }

        [Fact]
        public void OnlyOneObjectNonEqualTest()
        {
            Folder folder = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = @"C:\Users\TestUser\Pictures"
            };

            Assert.False(folder.Equals(null));
        }

        [Fact]
        public void ToStringWithPathTest()
        {
            Folder folder = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = @"C:\Users\TestUser\Pictures"
            };

            Assert.Equal(@"C:\Users\TestUser\Pictures", folder.ToString());
        }

        [Fact]
        public void ToStringWithoutPathTest()
        {
            Folder folder1 = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = null
            };

            Assert.Null(folder1.ToString());

            Folder folder2 = new Folder
            {
                FolderId = "599e3dec-1da6-4e1d-b18d-e2e6cb417292",
                Path = ""
            };

            Assert.Equal("", folder2.ToString());
        }
    }
}
