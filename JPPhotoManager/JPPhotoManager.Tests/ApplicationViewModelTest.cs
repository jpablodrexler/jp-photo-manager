using System;
using System.Collections.Generic;
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
        public void GoToAssetTest()
        {
            Folder folder = new Folder { Path = @"D:\Data" };

            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), Folder = folder }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");
            mockApp.Setup(a => a.FileExists(@"D:\Data\Image3.jpg")).Returns(true);

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = 4;
            viewModel.GoToAsset(assets[2]);

            Assert.Equal(2, viewModel.ViewerPosition);
        }

        [Fact]
        public void GoToAssetNotInListTest()
        {
            Folder folder = new Folder { Path = @"D:\Data" };

            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), Folder = folder }
            };

            Asset assetNotInList = new Asset { FileName = "ImageNotInList.jpg", ImageData = new BitmapImage(), Folder = folder };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");
            mockApp.Setup(a => a.FileExists(@"D:\Data\Image3.jpg")).Returns(true);

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetFiles(assets);
            viewModel.ViewerPosition = 4;
            viewModel.GoToAsset(assetNotInList);

            Assert.Equal(4, viewModel.ViewerPosition);
        }

        [Fact]
        public void NotifyCatalogChangeCreatedToNonEmptyListCurrentFolderTest()
        {
            Folder folder = new Folder { Path = @"D:\Data" };

            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), Folder = folder }
            };

            Asset newAsset = new Asset { FileName = "NewImage.jpg", ImageData = new BitmapImage(), Folder = folder };
            string statusMessage = "Creating thumbnail for NewImage.jpg";

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");
            
            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetFiles(assets);

            viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
            {
                Asset = newAsset,
                Message = statusMessage,
                Reason = ReasonEnum.Created
            });

            Assert.Equal(6, viewModel.Files.Count);
            Assert.Equal("Image1.jpg", viewModel.Files[0].FileName);
            Assert.Equal("Image2.jpg", viewModel.Files[1].FileName);
            Assert.Equal("Image3.jpg", viewModel.Files[2].FileName);
            Assert.Equal("Image4.jpg", viewModel.Files[3].FileName);
            Assert.Equal("Image5.jpg", viewModel.Files[4].FileName);
            Assert.Equal("NewImage.jpg", viewModel.Files[5].FileName);
            Assert.Equal(statusMessage, viewModel.StatusMessage);
        }

        [Fact]
        public void NotifyCatalogChangeCreatedToEmptyListCurrentFolderWithCataloguedAssetsTest()
        {
            Folder folder = new Folder { Path = @"D:\Data" };
            Asset[] assets = new Asset[] { };
            
            var cataloguedAssets = new List<Asset>
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), Folder = folder }
            };

            string statusMessage = "Creating thumbnail for Image5.jpg";

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetFiles(assets);

            viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
            {
                Asset = cataloguedAssets[4],
                CataloguedAssets = cataloguedAssets,
                Message = statusMessage,
                Reason = ReasonEnum.Created
            });

            Assert.Equal(5, viewModel.Files.Count);
            Assert.Equal("Image1.jpg", viewModel.Files[0].FileName);
            Assert.Equal("Image2.jpg", viewModel.Files[1].FileName);
            Assert.Equal("Image3.jpg", viewModel.Files[2].FileName);
            Assert.Equal("Image4.jpg", viewModel.Files[3].FileName);
            Assert.Equal("Image5.jpg", viewModel.Files[4].FileName);
            Assert.Equal(statusMessage, viewModel.StatusMessage);
        }

        [Fact]
        public void NotifyCatalogChangeCreatedToEmptyListCurrentFolderTest()
        {
            Folder folder = new Folder { Path = @"D:\Data" };
            Asset[] assets = new Asset[] { };
            Asset newAsset = new Asset { FileName = "NewImage.jpg", ImageData = new BitmapImage(), Folder = folder };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetFiles(assets);

            viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
            {
                Asset = newAsset,
                Reason = ReasonEnum.Created
            });

            Assert.Single(viewModel.Files);
        }

        [Fact]
        public void NotifyCatalogChangeCreatedToNonEmptyListDifferentFolderTest()
        {
            Folder folder = new Folder { Path = @"D:\Data" };

            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), Folder = folder }
            };

            Folder newFolder = new Folder { Path = @"D:\NewFolder" };
            Asset newAsset = new Asset { FileName = "NewImage.jpg", ImageData = new BitmapImage(), Folder = newFolder };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetFiles(assets);

            viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
            {
                Asset = newAsset,
                Reason = ReasonEnum.Created
            });

            Assert.Equal(5, viewModel.Files.Count);
            Assert.Equal("Image1.jpg", viewModel.Files[0].FileName);
            Assert.Equal("Image2.jpg", viewModel.Files[1].FileName);
            Assert.Equal("Image3.jpg", viewModel.Files[2].FileName);
            Assert.Equal("Image4.jpg", viewModel.Files[3].FileName);
            Assert.Equal("Image5.jpg", viewModel.Files[4].FileName);
        }

        [Fact]
        public void NotifyCatalogChangeInvalidParametersTest()
        {
            Folder folder = new Folder { Path = @"D:\Data" };

            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), Folder = folder }
            };

            Asset newAsset = new Asset { FileName = "NewImage.jpg", ImageData = new BitmapImage(), Folder = null };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetFiles(assets);

            viewModel.NotifyCatalogChange(null);

            viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
            {
                Asset = null,
                Reason = ReasonEnum.Created
            });

            viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
            {
                Asset = newAsset,
                Reason = ReasonEnum.Created
            });

            Assert.Equal(5, viewModel.Files.Count);
            Assert.Equal("Image1.jpg", viewModel.Files[0].FileName);
            Assert.Equal("Image2.jpg", viewModel.Files[1].FileName);
            Assert.Equal("Image3.jpg", viewModel.Files[2].FileName);
            Assert.Equal("Image4.jpg", viewModel.Files[3].FileName);
            Assert.Equal("Image5.jpg", viewModel.Files[4].FileName);
        }

        [Fact]
        public void RemoveAssetFromCurrentFolderTest()
        {
            Folder folder = new Folder { Path = @"D:\Data" };
            
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), Folder = folder },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), Folder = folder }
            };

            string statusMessage = "Removing thumbnail for Image3.jpg";

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetFiles(assets);

            viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
            {
                Asset = assets[2],
                Message = statusMessage,
                Reason = ReasonEnum.Deleted
            });

            Assert.Equal(4, viewModel.Files.Count);
            Assert.Equal("Image1.jpg", viewModel.Files[0].FileName);
            Assert.Equal("Image2.jpg", viewModel.Files[1].FileName);
            Assert.Equal("Image4.jpg", viewModel.Files[2].FileName);
            Assert.Equal("Image5.jpg", viewModel.Files[3].FileName);
            Assert.Equal(statusMessage, viewModel.StatusMessage);
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
