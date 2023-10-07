using FluentAssertions;
using JPPhotoManager.Domain.Entities;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests.Unit.Domain.Entities
{
    public class FolderTests
    {
        private string _dataDirectory;

        public FolderTests()
        {
            _dataDirectory = Path.GetDirectoryName(typeof(FolderTests).Assembly.Location);
            _dataDirectory = Path.Combine(_dataDirectory, "TestFiles");
        }

        [Fact]
        public void SameFileAndFolderEqualTest()
        {
            Folder folder1 = new()
            {
                FolderId = 1,
                Path = @"C:\Users\TestUser\Pictures"
            };

            Folder folder2 = new()
            {
                FolderId = 2,
                Path = @"C:\Users\TestUser\Pictures"
            };

            folder1.Should().Be(folder2);
            folder1.GetHashCode().Should().Be(folder2.GetHashCode());
        }

        [Fact]
        public void DifferentPathNonEqualTest()
        {
            Folder folder1 = new()
            {
                FolderId = 1,
                Path = @"C:\Users\TestUser\Pictures"
            };

            Folder folder2 = new()
            {
                FolderId = 2,
                Path = @"C:\Users\TestUser\Pictures\MyAlbum"
            };

            folder1.Should().NotBe(folder2);
            folder1.GetHashCode().Should().NotBe(folder2.GetHashCode());
        }

        [Fact]
        public void OnlyOnePathNonEqualTest()
        {
            Folder folder1 = new()
            {
                FolderId = 1,
                Path = @"C:\Users\TestUser\Pictures"
            };

            Folder folder2 = new()
            {
                FolderId = 2,
                Path = null
            };

            folder1.Should().NotBe(folder2);
            folder1.GetHashCode().Should().NotBe(folder2.GetHashCode());
        }

        [Fact]
        public void OnlyOneObjectNonEqualTest()
        {
            Folder folder = new()
            {
                FolderId = 1,
                Path = @"C:\Users\TestUser\Pictures"
            };

            folder.Should().NotBeNull();
        }

        [Fact]
        public void ToStringWithPathTest()
        {
            Folder folder = new()
            {
                FolderId = 1,
                Path = @"C:\Users\TestUser\Pictures"
            };

            folder.ToString().Should().Be(@"C:\Users\TestUser\Pictures");
        }

        [Fact]
        public void ToStringWithoutPathTest()
        {
            Folder folder1 = new()
            {
                FolderId = 1,
                Path = null
            };

            folder1.ToString().Should().BeNull();

            Folder folder2 = new()
            {
                FolderId = 2,
                Path = ""
            };

            folder2.ToString().Should().BeEmpty();
        }

        [Theory]
        [InlineData("TestFolder", "TestFolder\\TestSubFolder1", true)]
        [InlineData("TestFolder", "TestFolder\\TestSubFolder2", true)]
        [InlineData("TestFolder", "TestFolder\\TestSubFolder2\\TestSubFolder3", false)]
        [InlineData("TestFolder\\TestSubFolder1", "TestFolder", false)]
        [InlineData("TestFolder\\TestSubFolder2", "TestFolder", false)]
        [InlineData("TestFolder\\TestSubFolder2\\TestSubFolder3", "TestFolder", false)]
        [InlineData("TestFolder", "TestFolder", false)]
        [InlineData("TestFolder\\TestSubFolder1", "TestFolder\\TestSubFolder2", false)]
        [InlineData("", "TestFolder", true)]
        [InlineData("", "TestFolder\\TestSubFolder1", false)]
        [InlineData("TestFolder", "", false)]
        public void IsParentFolderTest(string testFolderPath1, string testFolderPath2, bool expected)
        {
            _dataDirectory = Path.GetDirectoryName(typeof(FolderTests).Assembly.Location);
            _dataDirectory = Path.Combine(_dataDirectory, "TestFiles");

            string absoluteTestFolderPath1 = Path.Combine(_dataDirectory, testFolderPath1);
            string absoluteTestFolderPath2 = Path.Combine(_dataDirectory, testFolderPath2);

            Folder folder1 = new()
            {
                Path = absoluteTestFolderPath1
            };

            Folder folder2 = new()
            {
                Path = absoluteTestFolderPath2
            };

            bool result = folder1.IsParentOf(folder2);
            result.Should().Be(expected);
        }
    }
}
