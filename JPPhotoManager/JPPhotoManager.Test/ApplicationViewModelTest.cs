using System;
using System.Collections.ObjectModel;
using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.ViewModels;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Moq;

namespace JPPhotoManager.Test
{
    [TestClass]
    public class ApplicationViewModelTest
    {
        [TestMethod]
        public void TestChangeAppMode()
        {
            Mock<IJPPhotoManagerApplication> mock = new Mock<IJPPhotoManagerApplication>();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");
            ApplicationViewModel viewModel = new ApplicationViewModel(mock.Object);

            Assert.AreEqual(AppModeEnum.Thumbnails, viewModel.AppMode);
            viewModel.ChangeAppMode();
            Assert.AreEqual(AppModeEnum.Viewer, viewModel.AppMode);
            viewModel.ChangeAppMode();
            Assert.AreEqual(AppModeEnum.Thumbnails, viewModel.AppMode);
            viewModel.ChangeAppMode(AppModeEnum.Viewer);
            Assert.AreEqual(AppModeEnum.Viewer, viewModel.AppMode);
        }

        [TestMethod]
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

            Mock<IJPPhotoManagerApplication> mockApp = new Mock<IJPPhotoManagerApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object)
            {
                Files = assets,
                ViewerPosition = 2
            };

            viewModel.RemoveAsset(assets[2]);

            Assert.AreEqual(2, viewModel.ViewerPosition);
            Assert.AreEqual(4, viewModel.Files.Count);
        }

        [TestMethod]
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

            Mock<IJPPhotoManagerApplication> mockApp = new Mock<IJPPhotoManagerApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object)
            {
                Files = assets,
                ViewerPosition = 0
            };

            viewModel.RemoveAsset(assets[0]);

            Assert.AreEqual(0, viewModel.ViewerPosition);
            Assert.AreEqual(4, viewModel.Files.Count);
        }

        [TestMethod]
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

            Mock<IJPPhotoManagerApplication> mockApp = new Mock<IJPPhotoManagerApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object)
            {
                Files = assets,
                ViewerPosition = 4
            };

            viewModel.RemoveAsset(assets[4]);

            Assert.AreEqual(3, viewModel.ViewerPosition);
            Assert.AreEqual(4, viewModel.Files.Count);
        }

        [TestMethod]
        public void RemoveAssetSoleElementTest()
        {
            ObservableCollection<Asset> assets = new ObservableCollection<Asset>
            {
                new Asset { FileName="Image1.jpg" }
            };

            Mock<IJPPhotoManagerApplication> mockApp = new Mock<IJPPhotoManagerApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object)
            {
                Files = assets,
                ViewerPosition = 0
            };

            viewModel.RemoveAsset(assets[0]);

            Assert.AreEqual(-1, viewModel.ViewerPosition);
            Assert.AreEqual(0, viewModel.Files.Count);
        }

        [TestMethod]
        public void RemoveAssetNoElementsTest()
        {
            ObservableCollection<Asset> assets = new ObservableCollection<Asset>();

            Mock<IJPPhotoManagerApplication> mockApp = new Mock<IJPPhotoManagerApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object)
            {
                Files = assets,
                ViewerPosition = -1
            };

            viewModel.RemoveAsset(null);

            Assert.AreEqual(-1, viewModel.ViewerPosition);
            Assert.AreEqual(0, viewModel.Files.Count);
        }

        [TestMethod]
        public void RemoveAssetNullCollectionTest()
        {
            Mock<IJPPhotoManagerApplication> mockApp = new Mock<IJPPhotoManagerApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object)
            {
                Files = null,
                ViewerPosition = -1
            };

            viewModel.RemoveAsset(null);

            Assert.AreEqual(-1, viewModel.ViewerPosition);
            Assert.IsNull(viewModel.Files);
        }
    }
}
