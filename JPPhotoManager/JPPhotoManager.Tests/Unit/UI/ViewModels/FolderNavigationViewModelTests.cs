using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Application;
using JPPhotoManager.Domain.Entities;
using JPPhotoManager.UI.ViewModels;
using Xunit;

namespace JPPhotoManager.Tests.Unit.UI.ViewModels
{
    public class FolderNavigationViewModelTests
    {
        [Fact]
        public void ViewModelTest()
        {
            using var mock = AutoMock.GetLoose();
            mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            Folder sourceFolder = new() { Path = @"D:\Data\Folder1" };
            Folder selectedFolder = new() { Path = @"D:\Data\Folder2" };

            var viewModel = mock.Container.Resolve<FolderNavigationViewModel>(
                new NamedParameter("sourceFolder", sourceFolder),
                new NamedParameter("lastSelectedFolder", selectedFolder),
                new NamedParameter("recentTargetPaths", new List<string>()));

            viewModel.TargetPath = @"D:\Data\Folder2";
            viewModel.HasConfirmed = true;

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
            using var mock = AutoMock.GetLoose();
            mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            Folder sourceFolder = new() { Path = sourcePath };

            var viewModel = mock.Container.Resolve<FolderNavigationViewModel>(
                new NamedParameter("sourceFolder", sourceFolder),
                new NamedParameter("lastSelectedFolder", null),
                new NamedParameter("recentTargetPaths", new List<string>()));

            viewModel.TargetPath = selectedPath;
            viewModel.CanConfirm.Should().Be(expected);
        }

        [Fact]
        public void CanConfirmNullSourceFolderTest()
        {
            using var mock = AutoMock.GetLoose();
            mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            Folder sourceFolder = null;

            var viewModel = mock.Container.Resolve<FolderNavigationViewModel>(
                new NamedParameter("sourceFolder", sourceFolder),
                new NamedParameter("lastSelectedFolder", null),
                new NamedParameter("recentTargetPaths", new List<string>()));

            viewModel.TargetPath = @"D:\Data\Folder2";
            viewModel.CanConfirm.Should().BeFalse();
        }

        [Fact]
        public void CanConfirmNullTargetPathTest()
        {
            using var mock = AutoMock.GetLoose();
            mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            Folder sourceFolder = new() { Path = @"D:\Data\Folder1" };

            var viewModel = mock.Container.Resolve<FolderNavigationViewModel>(
                new NamedParameter("sourceFolder", sourceFolder),
                new NamedParameter("lastSelectedFolder", null),
                new NamedParameter("recentTargetPaths", new List<string>()));

            viewModel.TargetPath = null;
            viewModel.CanConfirm.Should().BeFalse();
        }
    }
}
