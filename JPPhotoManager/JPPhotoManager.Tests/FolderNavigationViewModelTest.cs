using FluentAssertions;
using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using Moq;
using System.Collections.Generic;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class FolderNavigationViewModelTest
    {
        [Fact]
        public void ViewModelTest()
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            Folder sourceFolder = new Folder { Path = @"D:\Data\Folder1" };
            Folder selectedFolder = new Folder { Path = @"D:\Data\Folder2" };

            FolderNavigationViewModel viewModel = new FolderNavigationViewModel(mockApp.Object, sourceFolder, selectedFolder, new List<string>())
            {
                TargetPath = @"D:\Data\Folder2",
                HasConfirmed = true
            };

            viewModel.SourceFolder.Path.Should().Be(@"D:\Data\Folder1");
            viewModel.LastSelectedFolder.Path.Should().Be(@"D:\Data\Folder2");
            viewModel.TargetPath.Should().Be(@"D:\Data\Folder2");
            viewModel.SelectedFolder.Path.Should().Be(@"D:\Data\Folder2");
            viewModel.HasConfirmed.Should().BeTrue();
        }

        [Theory]
        [InlineData(@"D:\Data\Folder1", @"D:\Data\Folder1", false)]
        [InlineData(@"D:\Data\Folder1", @"D:\Data\Folder2", true)]
        public void CanConfirmTest(string sourcePath, string selectedPath, bool expected)
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            Folder sourceFolder = new Folder { Path = sourcePath };

            FolderNavigationViewModel viewModel = new FolderNavigationViewModel(mockApp.Object, sourceFolder, null, new List<string>())
            {
                TargetPath = selectedPath
            };

            viewModel.CanConfirm.Should().Be(expected);
        }

        [Fact]
        public void CanConfirmNullSourceFolderTest()
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            Folder sourceFolder = null;

            FolderNavigationViewModel viewModel = new FolderNavigationViewModel(mockApp.Object, sourceFolder, null, new List<string>())
            {
                TargetPath = @"D:\Data\Folder2"
            };

            viewModel.CanConfirm.Should().BeFalse();
        }

        [Fact]
        public void CanConfirmNullTargetPathTest()
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            Folder sourceFolder = new Folder { Path = @"D:\Data\Folder1" };

            FolderNavigationViewModel viewModel = new FolderNavigationViewModel(mockApp.Object, sourceFolder, null, new List<string>())
            {
                TargetPath = null
            };

            viewModel.CanConfirm.Should().BeFalse();
        }
    }
}
