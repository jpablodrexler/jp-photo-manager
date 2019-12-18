using System;
using System.Collections.ObjectModel;
using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using Moq;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class ApplicationViewModelTest
    {
        [Fact]
        public void TestChangeAppMode()
        {
            Mock<IApplication> mock = new Mock<IApplication>();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");
            ApplicationViewModel viewModel = new ApplicationViewModel(mock.Object);

            Assert.Equal(AppModeEnum.Thumbnails, viewModel.AppMode);
            viewModel.ChangeAppMode();
            Assert.Equal(AppModeEnum.Viewer, viewModel.AppMode);
            viewModel.ChangeAppMode();
            Assert.Equal(AppModeEnum.Thumbnails, viewModel.AppMode);
            viewModel.ChangeAppMode(AppModeEnum.Viewer);
            Assert.Equal(AppModeEnum.Viewer, viewModel.AppMode);
        }

        [Fact]
        public void RemoveAssetMidElementTest()
        {
            ObservableCollection<Asset> assets = new ObservableCollection<Asset>
            {
                new Asset { FileName="Image1.jpg" },
                new Asset { FileName="Image2.jpg" },
                new Asset { FileName="Image3.jpg" },
                new Asset { FileName="Image4.jpg" },
                new Asset { FileName="Image5.jpg" }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object)
            {
                Files = assets,
                ViewerPosition = 2
            };

            viewModel.RemoveAsset(assets[2]);

            Assert.Equal(2, viewModel.ViewerPosition);
            Assert.Equal(4, viewModel.Files.Count);
        }

        [Fact]
        public void RemoveAssetFirstElementTest()
        {
            ObservableCollection<Asset> assets = new ObservableCollection<Asset>
            {
                new Asset { FileName="Image1.jpg" },
                new Asset { FileName="Image2.jpg" },
                new Asset { FileName="Image3.jpg" },
                new Asset { FileName="Image4.jpg" },
                new Asset { FileName="Image5.jpg" }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object)
            {
                Files = assets,
                ViewerPosition = 0
            };

            viewModel.RemoveAsset(assets[0]);

            Assert.Equal(0, viewModel.ViewerPosition);
            Assert.Equal(4, viewModel.Files.Count);
        }

        [Fact]
        public void RemoveAssetLastElementTest()
        {
            ObservableCollection<Asset> assets = new ObservableCollection<Asset>
            {
                new Asset { FileName="Image1.jpg" },
                new Asset { FileName="Image2.jpg" },
                new Asset { FileName="Image3.jpg" },
                new Asset { FileName="Image4.jpg" },
                new Asset { FileName="Image5.jpg" }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object)
            {
                Files = assets,
                ViewerPosition = 4
            };

            viewModel.RemoveAsset(assets[4]);

            Assert.Equal(3, viewModel.ViewerPosition);
            Assert.Equal(4, viewModel.Files.Count);
        }

        [Fact]
        public void RemoveAssetSoleElementTest()
        {
            ObservableCollection<Asset> assets = new ObservableCollection<Asset>
            {
                new Asset { FileName="Image1.jpg" }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object)
            {
                Files = assets,
                ViewerPosition = 0
            };

            viewModel.RemoveAsset(assets[0]);

            Assert.Equal(-1, viewModel.ViewerPosition);
            Assert.Empty(viewModel.Files);
        }

        [Fact]
        public void RemoveAssetNoElementsTest()
        {
            ObservableCollection<Asset> assets = new ObservableCollection<Asset>();

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object)
            {
                Files = assets,
                ViewerPosition = -1
            };

            viewModel.RemoveAsset(null);

            Assert.Equal(-1, viewModel.ViewerPosition);
            Assert.Empty(viewModel.Files);
        }

        [Fact]
        public void RemoveAssetNullCollectionTest()
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object)
            {
                Files = null,
                ViewerPosition = -1
            };

            viewModel.RemoveAsset(null);

            Assert.Equal(-1, viewModel.ViewerPosition);
            Assert.Null(viewModel.Files);
        }
    }
}
