using System;
using System.Collections.ObjectModel;
using System.Windows;
using System.Windows.Media.Imaging;
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
        public void GoToPreviousImageTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = 2;
            viewModel.GoToPreviousImage();

            Assert.Equal(1, viewModel.ViewerPosition);
        }

        [Fact]
        public void GoToPreviousImageFromFirstTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = 0;
            viewModel.GoToPreviousImage();

            Assert.Equal(0, viewModel.ViewerPosition);
        }

        [Fact]
        public void GoToNextImageTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = 2;
            viewModel.GoToNextImage();

            Assert.Equal(3, viewModel.ViewerPosition);
        }

        [Fact]
        public void GoToNextImageFromLastTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = 4;
            viewModel.GoToNextImage();

            Assert.Equal(4, viewModel.ViewerPosition);
        }

        [Fact]
        public void RemoveAssetMidElementTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = 2;
            viewModel.RemoveAsset(assets[2]);

            Assert.Equal(2, viewModel.ViewerPosition);
            Assert.Equal(4, viewModel.Files.Count);
        }

        [Fact]
        public void RemoveAssetFirstElementTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = 0;

            viewModel.RemoveAsset(assets[0]);

            Assert.Equal(0, viewModel.ViewerPosition);
            Assert.Equal(4, viewModel.Files.Count);
        }

        [Fact]
        public void RemoveAssetLastElementTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = 4;

            viewModel.RemoveAsset(assets[4]);

            Assert.Equal(3, viewModel.ViewerPosition);
            Assert.Equal(4, viewModel.Files.Count);
        }

        [Fact]
        public void RemoveAssetSoleElementTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = 0;

            viewModel.RemoveAsset(assets[0]);

            Assert.Equal(-1, viewModel.ViewerPosition);
            Assert.Empty(viewModel.Files);
        }

        [Fact]
        public void RemoveAssetNoElementsTest()
        {
            Asset[] assets = new Asset[] { };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = -1;

            viewModel.RemoveAsset(null);

            Assert.Equal(-1, viewModel.ViewerPosition);
            Assert.Empty(viewModel.Files);
        }

        [Fact]
        public void RemoveAssetNullCollectionTest()
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetFiles(null);
            viewModel.ViewerPosition = -1;
            
            viewModel.RemoveAsset(null);

            Assert.Equal(-1, viewModel.ViewerPosition);
            Assert.Null(viewModel.Files);
        }

        [Fact]
        public void ThumbnailsVisibleTest()
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            
            viewModel.ChangeAppMode(AppModeEnum.Thumbnails);
            
            Assert.Equal(Visibility.Visible, viewModel.ThumbnailsVisible);

            viewModel.ChangeAppMode(AppModeEnum.Viewer);

            Assert.Equal(Visibility.Hidden, viewModel.ThumbnailsVisible);
        }

        [Fact]
        public void ViewerVisibleTest()
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.ChangeAppMode(AppModeEnum.Viewer);

            Assert.Equal(Visibility.Visible, viewModel.ViewerVisible);

            viewModel.ChangeAppMode(AppModeEnum.Thumbnails);

            Assert.Equal(Visibility.Hidden, viewModel.ViewerVisible);
        }

        [Fact]
        public void UpdateAppTitleInThumbnailsModeTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.Product = "JPPhotoManager";
            viewModel.Version = "v1.0.0.0";
            viewModel.ChangeAppMode(AppModeEnum.Thumbnails);
            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = 3;

            Assert.Equal(@"JPPhotoManager v1.0.0.0 - D:\Data", viewModel.AppTitle);
        }

        [Fact]
        public void UpdateAppTitleInViewerModeTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.Product = "JPPhotoManager";
            viewModel.Version = "v1.0.0.0";
            viewModel.ChangeAppMode(AppModeEnum.Viewer);
            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = 3;

            Assert.Equal(@"JPPhotoManager v1.0.0.0 - Image4.jpg - image 4 de 5", viewModel.AppTitle);
        }
    }
}
