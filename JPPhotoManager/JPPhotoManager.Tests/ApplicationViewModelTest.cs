using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using Moq;
using System;
using System.Collections.Generic;
using System.Windows;
using System.Windows.Media.Imaging;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class ApplicationViewModelTest
    {
        [Fact]
        public void ChangeAppMode_NoParameter_ChangeAppMode()
        {
            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(app => app.GetInitialFolder()).Returns(@"C:\");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.AppMode.Should().Be(AppModeEnum.Thumbnails);
                viewModel.ChangeAppMode();
                viewModel.AppMode.Should().Be(AppModeEnum.Viewer);
                viewModel.ChangeAppMode();
                viewModel.AppMode.Should().Be(AppModeEnum.Thumbnails);
                viewModel.ChangeAppMode(AppModeEnum.Viewer);
                viewModel.AppMode.Should().Be(AppModeEnum.Viewer);
            }
        }

        [Theory]
        [InlineData(0, 0)]
        [InlineData(1, 0)]
        [InlineData(2, 1)]
        [InlineData(3, 2)]
        [InlineData(4, 3)]
        public void GoToPreviousAsset_ChangeViewerPosition(int currentPosition, int expected)
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() }
            };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);
                viewModel.ViewerPosition = currentPosition;
                viewModel.GoToPreviousAsset();

                viewModel.ViewerPosition.Should().Be(expected);
            }
        }

        [Theory]
        [InlineData(0, 1)]
        [InlineData(1, 2)]
        [InlineData(2, 3)]
        [InlineData(3, 4)]
        [InlineData(4, 4)]
        public void GoToNextAsset_ChangeViewerPosition(int currentPosition, int expected)
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() }
            };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);
                viewModel.ViewerPosition = currentPosition;
                viewModel.GoToNextAsset();

                viewModel.ViewerPosition.Should().Be(expected);
            }
        }

        [Theory]
        [InlineData(0, 0)]
        [InlineData(1, 0)]
        [InlineData(2, 0)]
        [InlineData(3, 0)]
        [InlineData(4, 0)]
        [InlineData(0, 1)]
        [InlineData(1, 1)]
        [InlineData(2, 1)]
        [InlineData(3, 1)]
        [InlineData(4, 1)]
        [InlineData(0, 2)]
        [InlineData(1, 2)]
        [InlineData(2, 2)]
        [InlineData(3, 2)]
        [InlineData(4, 2)]
        [InlineData(0, 3)]
        [InlineData(1, 3)]
        [InlineData(2, 3)]
        [InlineData(3, 3)]
        [InlineData(4, 3)]
        [InlineData(0, 4)]
        [InlineData(1, 4)]
        [InlineData(2, 4)]
        [InlineData(3, 4)]
        [InlineData(4, 4)]
        public void GoToAsset_ChangeViewerPosition(int currentPosition, int goToAssetIndex)
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

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");
                mock.Mock<IApplication>().Setup(a => a.FileExists(@"D:\Data\Image1.jpg")).Returns(true);
                mock.Mock<IApplication>().Setup(a => a.FileExists(@"D:\Data\Image2.jpg")).Returns(true);
                mock.Mock<IApplication>().Setup(a => a.FileExists(@"D:\Data\Image3.jpg")).Returns(true);
                mock.Mock<IApplication>().Setup(a => a.FileExists(@"D:\Data\Image4.jpg")).Returns(true);
                mock.Mock<IApplication>().Setup(a => a.FileExists(@"D:\Data\Image5.jpg")).Returns(true);

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);
                viewModel.ViewerPosition = currentPosition;
                viewModel.GoToAsset(assets[goToAssetIndex]);

                viewModel.ViewerPosition.Should().Be(goToAssetIndex);
            }
        }

        [Fact]
        public void GoToAsset_NotInList_KeepPosition()
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

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");
                mock.Mock<IApplication>().Setup(a => a.FileExists(@"D:\Data\Image3.jpg")).Returns(true);

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);
                viewModel.ViewerPosition = 4;
                viewModel.GoToAsset(assetNotInList);

                viewModel.ViewerPosition.Should().Be(4);
            }
        }

        [Fact]
        public void NotifyCatalogChange_CreatedToNonEmptyListCurrentFolder_AddNewAssetToList()
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

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);

                viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
                {
                    Asset = newAsset,
                    Message = statusMessage,
                    Reason = ReasonEnum.Created
                });

                viewModel.ObservableAssets.Should().HaveCount(6);
                viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
                viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                viewModel.ObservableAssets[2].FileName.Should().Be("Image3.jpg");
                viewModel.ObservableAssets[3].FileName.Should().Be("Image4.jpg");
                viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
                viewModel.ObservableAssets[5].FileName.Should().Be("NewImage.jpg");
                viewModel.StatusMessage.Should().Be(statusMessage);
            }
        }
        
        [Fact]
        public void NotifyCatalogChange_CreatedToEmptyListCurrentFolderWithCataloguedAssets_AddAllCataloguedAssetsToList()
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

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);

                viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
                {
                    Asset = cataloguedAssets[4],
                    CataloguedAssets = cataloguedAssets,
                    Message = statusMessage,
                    Reason = ReasonEnum.Created
                });

                viewModel.ObservableAssets.Should().HaveCount(5);
                viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
                viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                viewModel.ObservableAssets[2].FileName.Should().Be("Image3.jpg");
                viewModel.ObservableAssets[3].FileName.Should().Be("Image4.jpg");
                viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
                viewModel.StatusMessage.Should().Be(statusMessage);
            }
        }

        [Fact]
        public void NotifyCatalogChange_CreatedToEmptyListCurrentFolderWithoutCataloguedAssets_AddNewAssetToList()
        {
            Folder folder = new Folder { Path = @"D:\Data" };
            Asset[] assets = new Asset[] { };
            Asset newAsset = new Asset { FileName = "NewImage.jpg", ImageData = new BitmapImage(), Folder = folder };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);

                viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
                {
                    Asset = newAsset,
                    Reason = ReasonEnum.Created
                });

                viewModel.ObservableAssets.Should().ContainSingle();
            }
        }

        [Fact]
        public void NotifyCatalogChange_CreatedToNonEmptyListDifferentFolder_KeepExistingList()
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

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);

                viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
                {
                    Asset = newAsset,
                    Reason = ReasonEnum.Created
                });

                viewModel.ObservableAssets.Should().HaveCount(5);
                viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
                viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                viewModel.ObservableAssets[2].FileName.Should().Be("Image3.jpg");
                viewModel.ObservableAssets[3].FileName.Should().Be("Image4.jpg");
                viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
            }
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
            viewModel.SetAssets(assets);

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

            viewModel.ObservableAssets.Should().HaveCount(5);
            viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
            viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
            viewModel.ObservableAssets[2].FileName.Should().Be("Image3.jpg");
            viewModel.ObservableAssets[3].FileName.Should().Be("Image4.jpg");
            viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
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
            viewModel.SetAssets(assets);

            viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
            {
                Asset = assets[2],
                Message = statusMessage,
                Reason = ReasonEnum.Deleted
            });

            viewModel.ObservableAssets.Should().HaveCount(4);
            viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
            viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
            viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
            viewModel.ObservableAssets[3].FileName.Should().Be("Image5.jpg");
            viewModel.StatusMessage.Should().Be(statusMessage);
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

            viewModel.SetAssets(assets);
            viewModel.ViewerPosition = 2;
            viewModel.RemoveAsset(assets[2]);

            viewModel.ViewerPosition.Should().Be(2);
            viewModel.ObservableAssets.Should().HaveCount(4);
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
            viewModel.SetAssets(assets);
            viewModel.ViewerPosition = 0;

            viewModel.RemoveAsset(assets[0]);

            viewModel.ViewerPosition.Should().Be(0);
            viewModel.ObservableAssets.Should().HaveCount(4);
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
            viewModel.SetAssets(assets);
            viewModel.ViewerPosition = 4;

            viewModel.RemoveAsset(assets[4]);

            viewModel.ViewerPosition.Should().Be(3);
            viewModel.ObservableAssets.Should().HaveCount(4);
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
            viewModel.SetAssets(assets);
            viewModel.ViewerPosition = 0;

            viewModel.RemoveAsset(assets[0]);

            viewModel.ViewerPosition.Should().Be(-1);
            viewModel.ObservableAssets.Should().BeEmpty();
        }

        [Fact]
        public void RemoveAssetNoElementsTest()
        {
            Asset[] assets = new Asset[] { };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetAssets(assets);
            viewModel.ViewerPosition = -1;

            viewModel.RemoveAsset(null);

            viewModel.ViewerPosition.Should().Be(-1);
            viewModel.ObservableAssets.Should().BeEmpty();
        }

        [Fact]
        public void RemoveAssetNullCollectionTest()
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            viewModel.SetAssets(null);
            viewModel.ViewerPosition = -1;
            
            viewModel.RemoveAsset(null);

            viewModel.ViewerPosition.Should().Be(-1);
            viewModel.ObservableAssets.Should().BeNull();
        }

        [Fact]
        public void ThumbnailsVisibleTest()
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);
            
            viewModel.ChangeAppMode(AppModeEnum.Thumbnails);

            viewModel.ThumbnailsVisible.Should().Be(Visibility.Visible);

            viewModel.ChangeAppMode(AppModeEnum.Viewer);

            viewModel.ThumbnailsVisible.Should().Be(Visibility.Hidden);
        }

        [Fact]
        public void ViewerVisibleTest()
        {
            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.ChangeAppMode(AppModeEnum.Viewer);

            viewModel.ViewerVisible.Should().Be(Visibility.Visible);

            viewModel.ChangeAppMode(AppModeEnum.Thumbnails);

            viewModel.ViewerVisible.Should().Be(Visibility.Hidden);
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
            viewModel.SetAssets(assets);
            viewModel.ViewerPosition = 3;

            viewModel.AppTitle.Should().Be(@"JPPhotoManager v1.0.0.0 - D:\Data - image 4 de 5 - sorted by file name ascending");
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
            viewModel.SetAssets(assets);
            viewModel.ViewerPosition = 3;

            viewModel.AppTitle.Should().Be(@"JPPhotoManager v1.0.0.0 - D:\Data - Image4.jpg - image 4 de 5 - sorted by file name ascending");
        }

        [Fact]
        public void SortNullFilesTest()
        {
            Asset[] assets = null;

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);
            viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileName);

            viewModel.ObservableAssets.Should().BeNull();
        }

        [Fact]
        public void SortEmptyFilesTest()
        {
            Asset[] assets = new Asset[] { };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);
            viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileName);

            viewModel.ObservableAssets.Should().NotBeNull();
            viewModel.ObservableAssets.Should().BeEmpty();
        }

        [Fact]
        public void SortByFileNameSingleFileTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);
            viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileName);

            viewModel.ObservableAssets.Should().NotBeNull();
            viewModel.ObservableAssets.Should().ContainSingle();
            viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
        }

        [Fact]
        public void SortByFileNameMultipleFilesOneTimeTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object, SortCriteriaEnum.Undefined);

            viewModel.SetAssets(assets);
            viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileName);

            viewModel.ObservableAssets.Should().NotBeNull();
            viewModel.ObservableAssets.Should().HaveCount(5);
            viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
            viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
            viewModel.ObservableAssets[2].FileName.Should().Be("Image3.jpg");
            viewModel.ObservableAssets[3].FileName.Should().Be("Image4.jpg");
            viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
        }

        [Fact]
        public void SortByFileNameMultipleFilesMultipleTimesTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object, SortCriteriaEnum.Undefined);

            viewModel.SetAssets(assets);

            for (int i = 0; i < 10; i++)
            {
                viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileName);

                viewModel.ObservableAssets.Should().NotBeNull();
                viewModel.ObservableAssets.Should().HaveCount(5);

                if (i % 2 == 0) // Ascending
                {
                    viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
                    viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                    viewModel.ObservableAssets[2].FileName.Should().Be("Image3.jpg");
                    viewModel.ObservableAssets[3].FileName.Should().Be("Image4.jpg");
                    viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
                }
                else // Descending
                {
                    viewModel.ObservableAssets[0].FileName.Should().Be("Image5.jpg");
                    viewModel.ObservableAssets[1].FileName.Should().Be("Image4.jpg");
                    viewModel.ObservableAssets[2].FileName.Should().Be("Image3.jpg");
                    viewModel.ObservableAssets[3].FileName.Should().Be("Image2.jpg");
                    viewModel.ObservableAssets[4].FileName.Should().Be("Image1.jpg");
                }
            }
        }

        [Fact]
        public void SortByThumbnailCreationDateTimeSingleFileTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = DateTime.Now }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);
            viewModel.SortAssetsByCriteria(SortCriteriaEnum.ThumbnailCreationDateTime);

            viewModel.ObservableAssets.Should().NotBeNull();
            viewModel.ObservableAssets.Should().ContainSingle();
            viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
        }

        [Fact]
        public void SortByThumbnailCreationDateTimeMultipleFilesOneTimeTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2010, 2, 1) },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2010, 1, 1) },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2010, 8, 1) }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);
            viewModel.SortAssetsByCriteria(SortCriteriaEnum.ThumbnailCreationDateTime);

            viewModel.ObservableAssets.Should().NotBeNull();
            viewModel.ObservableAssets.Should().HaveCount(5);
            viewModel.ObservableAssets[0].FileName.Should().Be("Image3.jpg");
            viewModel.ObservableAssets[1].FileName.Should().Be("Image1.jpg");
            viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
            viewModel.ObservableAssets[3].FileName.Should().Be("Image2.jpg");
            viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
        }

        [Fact]
        public void SortByThumbnailCreationDateTimeMultipleFilesMultipleTimesTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2010, 2, 1) },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2010, 1, 1) },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2010, 8, 1) }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);

            for (int i = 0; i < 10; i++)
            {
                viewModel.SortAssetsByCriteria(SortCriteriaEnum.ThumbnailCreationDateTime);

                viewModel.ObservableAssets.Should().NotBeNull();
                viewModel.ObservableAssets.Should().HaveCount(5);

                if (i % 2 == 0) // Ascending
                {
                    viewModel.ObservableAssets[0].FileName.Should().Be("Image3.jpg");
                    viewModel.ObservableAssets[1].FileName.Should().Be("Image1.jpg");
                    viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                    viewModel.ObservableAssets[3].FileName.Should().Be("Image2.jpg");
                    viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
                }
                else // Descending
                {
                    viewModel.ObservableAssets[0].FileName.Should().Be("Image5.jpg");
                    viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                    viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                    viewModel.ObservableAssets[3].FileName.Should().Be("Image1.jpg");
                    viewModel.ObservableAssets[4].FileName.Should().Be("Image3.jpg");
                }
            }
        }

        [Fact]
        public void SortByFileCreationDateTimeTimeSingleFileTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), FileCreationDateTime = DateTime.Now }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);
            viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileCreationDateTime);

            viewModel.ObservableAssets.Should().NotBeNull();
            viewModel.ObservableAssets.Should().ContainSingle();
            viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
        }

        [Fact]
        public void SortByFileCreationDateTimeMultipleFilesOneTimeTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2010, 2, 1) },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2010, 1, 1) },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2010, 8, 1) }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);
            viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileCreationDateTime);

            viewModel.ObservableAssets.Should().NotBeNull();
            viewModel.ObservableAssets.Should().HaveCount(5);
            viewModel.ObservableAssets[0].FileName.Should().Be("Image3.jpg");
            viewModel.ObservableAssets[1].FileName.Should().Be("Image1.jpg");
            viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
            viewModel.ObservableAssets[3].FileName.Should().Be("Image2.jpg");
            viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
        }

        [Fact]
        public void SortByFileCreationDateTimeMultipleFilesMultipleTimesTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2010, 2, 1) },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2010, 1, 1) },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2010, 8, 1) }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);

            for (int i = 0; i < 10; i++)
            {
                viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileCreationDateTime);

                viewModel.ObservableAssets.Should().NotBeNull();
                viewModel.ObservableAssets.Should().HaveCount(5);

                if (i % 2 == 0) // Ascending
                {
                    viewModel.ObservableAssets[0].FileName.Should().Be("Image3.jpg");
                    viewModel.ObservableAssets[1].FileName.Should().Be("Image1.jpg");
                    viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                    viewModel.ObservableAssets[3].FileName.Should().Be("Image2.jpg");
                    viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
                }
                else // Descending
                {
                    viewModel.ObservableAssets[0].FileName.Should().Be("Image5.jpg");
                    viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                    viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                    viewModel.ObservableAssets[3].FileName.Should().Be("Image1.jpg");
                    viewModel.ObservableAssets[4].FileName.Should().Be("Image3.jpg");
                }
            }
        }

        [Fact]
        public void SortByFileModificationDateTimeTimeSingleFileTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), FileModificationDateTime = DateTime.Now }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);
            viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileModificationDateTime);

            viewModel.ObservableAssets.Should().NotBeNull();
            viewModel.ObservableAssets.Should().ContainSingle();
            viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
        }

        [Fact]
        public void SortByFileModificationDateTimeMultipleFilesOneTimeTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), FileModificationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), FileModificationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), FileModificationDateTime = new DateTime(2010, 2, 1) },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), FileModificationDateTime = new DateTime(2010, 1, 1) },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), FileModificationDateTime = new DateTime(2010, 8, 1) }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);
            viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileModificationDateTime);

            viewModel.ObservableAssets.Should().NotBeNull();
            viewModel.ObservableAssets.Should().HaveCount(5);
            viewModel.ObservableAssets[0].FileName.Should().Be("Image3.jpg");
            viewModel.ObservableAssets[1].FileName.Should().Be("Image1.jpg");
            viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
            viewModel.ObservableAssets[3].FileName.Should().Be("Image2.jpg");
            viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
        }

        [Fact]
        public void SortByFileModificationDateTimeMultipleFilesMultipleTimesTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), FileModificationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), FileModificationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), FileModificationDateTime = new DateTime(2010, 2, 1) },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), FileModificationDateTime = new DateTime(2010, 1, 1) },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), FileModificationDateTime = new DateTime(2010, 8, 1) }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);

            for (int i = 0; i < 10; i++)
            {
                viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileModificationDateTime);

                viewModel.ObservableAssets.Should().NotBeNull();
                viewModel.ObservableAssets.Should().HaveCount(5);

                if (i % 2 == 0) // Ascending
                {
                    viewModel.ObservableAssets[0].FileName.Should().Be("Image3.jpg");
                    viewModel.ObservableAssets[1].FileName.Should().Be("Image1.jpg");
                    viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                    viewModel.ObservableAssets[3].FileName.Should().Be("Image2.jpg");
                    viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
                }
                else // Descending
                {
                    viewModel.ObservableAssets[0].FileName.Should().Be("Image5.jpg");
                    viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                    viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                    viewModel.ObservableAssets[3].FileName.Should().Be("Image1.jpg");
                    viewModel.ObservableAssets[4].FileName.Should().Be("Image3.jpg");
                }
            }
        }

        [Fact]
        public void SortByFileSizeTimeSingleFileTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), FileSize = 2048 }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);
            viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileSize);

            viewModel.ObservableAssets.Should().NotBeNull();
            viewModel.ObservableAssets.Should().ContainSingle();
            viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
        }

        [Fact]
        public void SortByFileSizeMultipleFilesOneTimeTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), FileSize = 2048 },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), FileSize = 2048 },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), FileSize = 2020 },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), FileSize = 2000 },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), FileSize = 2030 }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);
            viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileSize);

            viewModel.ObservableAssets.Should().NotBeNull();
            viewModel.ObservableAssets.Should().HaveCount(5);
            viewModel.ObservableAssets[0].FileName.Should().Be("Image3.jpg");
            viewModel.ObservableAssets[1].FileName.Should().Be("Image1.jpg");
            viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
            viewModel.ObservableAssets[3].FileName.Should().Be("Image2.jpg");
            viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
        }

        [Fact]
        public void SortByFileSizeMultipleFilesMultipleTimesTest()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), FileSize = 2048 },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), FileSize = 2048 },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), FileSize = 2020 },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), FileSize = 2000 },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), FileSize = 2030 }
            };

            Mock<IApplication> mockApp = new Mock<IApplication>();
            mockApp.Setup(a => a.GetInitialFolder()).Returns("D:\\Data");

            ApplicationViewModel viewModel = new ApplicationViewModel(mockApp.Object);

            viewModel.SetAssets(assets);

            for (int i = 0; i < 10; i++)
            {
                viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileSize);

                viewModel.ObservableAssets.Should().NotBeNull();
                viewModel.ObservableAssets.Should().HaveCount(5);

                if (i % 2 == 0) // Ascending
                {
                    viewModel.ObservableAssets[0].FileName.Should().Be("Image3.jpg");
                    viewModel.ObservableAssets[1].FileName.Should().Be("Image1.jpg");
                    viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                    viewModel.ObservableAssets[3].FileName.Should().Be("Image2.jpg");
                    viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
                }
                else // Descending
                {
                    viewModel.ObservableAssets[0].FileName.Should().Be("Image5.jpg");
                    viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                    viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                    viewModel.ObservableAssets[3].FileName.Should().Be("Image1.jpg");
                    viewModel.ObservableAssets[4].FileName.Should().Be("Image3.jpg");
                }
            }
        }
    }
}
