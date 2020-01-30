using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using Moq;
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

            FolderNavigationViewModel viewModel = new FolderNavigationViewModel(mockApp.Object, sourceFolder, selectedFolder)
            {
                SelectedFolder = selectedFolder,
                HasConfirmed = true
            };

            Assert.Equal(@"D:\Data\Folder1", viewModel.SourceFolder.Path);
            Assert.Equal(@"D:\Data\Folder2", viewModel.LastSelectedFolder.Path);
            Assert.Equal(@"D:\Data\Folder2", viewModel.SelectedFolder.Path);
            Assert.True(viewModel.HasConfirmed);
        }

        [Theory]
        [InlineData(@"D:\Data\Folder1", @"D:\Data\Folder1", false)]
        [InlineData(@"D:\Data\Folder1", @"D:\Data\Folder2", true)]
        public void CanConfirmTest(string sourcePath, string selectedPath, bool expected)
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            Folder sourceFolder = new Folder { Path = sourcePath };
            Folder selectedFolder = new Folder { Path = selectedPath };

            FolderNavigationViewModel viewModel = new FolderNavigationViewModel(mockApp.Object, sourceFolder, null)
            {
                SelectedFolder = selectedFolder
            };

            Assert.Equal(expected, viewModel.CanConfirm);
        }

        [Fact]
        public void CanConfirmNullSourceFolderTest()
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            Folder sourceFolder = null;
            Folder selectedFolder = new Folder { Path = @"D:\Data\Folder2" };

            FolderNavigationViewModel viewModel = new FolderNavigationViewModel(mockApp.Object, sourceFolder, null)
            {
                SelectedFolder = selectedFolder
            };

            Assert.False(viewModel.CanConfirm);
        }

        [Fact]
        public void CanConfirmNullSelectedFolderTest()
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            Folder sourceFolder = new Folder { Path = @"D:\Data\Folder1" };
            Folder selectedFolder = null;

            FolderNavigationViewModel viewModel = new FolderNavigationViewModel(mockApp.Object, sourceFolder, null)
            {
                SelectedFolder = selectedFolder
            };

            Assert.False(viewModel.CanConfirm);
        }
    }
}
