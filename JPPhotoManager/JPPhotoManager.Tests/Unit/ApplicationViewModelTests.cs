using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using Moq;
using System.Windows;
using System.Windows.Media.Imaging;
using Xunit;

namespace JPPhotoManager.Tests.Unit
{
    public class ApplicationViewModelTests
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
        [InlineData(0, false)]
        [InlineData(1, true)]
        [InlineData(2, true)]
        [InlineData(3, true)]
        [InlineData(4, true)]
        public void CanGoToPreviousAsset(int currentPosition, bool expected)
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
                viewModel.CanGoToPreviousAsset.Should().Be(expected);
            }
        }

        [Theory]
        [InlineData(0, true)]
        [InlineData(1, true)]
        [InlineData(2, true)]
        [InlineData(3, true)]
        [InlineData(4, false)]
        public void CanGoToNextAsset(int currentPosition, bool expected)
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
                viewModel.CanGoToNextAsset.Should().Be(expected);
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
                    Reason = ReasonEnum.AssetCreated
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
                    Reason = ReasonEnum.AssetCreated
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
                    Reason = ReasonEnum.AssetCreated
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
                    Reason = ReasonEnum.AssetCreated
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
        public void NotifyCatalogChange_NullFolder_IgnoreNewAsset()
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

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);

                viewModel.NotifyCatalogChange(null);

                viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
                {
                    Asset = null,
                    Reason = ReasonEnum.AssetCreated
                });

                viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
                {
                    Asset = newAsset,
                    Reason = ReasonEnum.AssetCreated
                });

                viewModel.ObservableAssets.Should().HaveCount(5);
                viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
                viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                viewModel.ObservableAssets[2].FileName.Should().Be("Image3.jpg");
                viewModel.ObservableAssets[3].FileName.Should().Be("Image4.jpg");
                viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
            }
        }

        [Theory]
        [InlineData("", "", "")]
        [InlineData("NewImage.jpg", "", "")]
        [InlineData("", "C3BB07CC-343F-4DCC-A39D-767B8F3E5DA4", "")]
        [InlineData("", "", "NewFolder")]
        [InlineData("NewImage.jpg", "C3BB07CC-343F-4DCC-A39D-767B8F3E5DA4", "")]
        [InlineData("NewImage.jpg", "", "C:\\NewFolder")]
        public void NotifyCatalogChange_InvalidParameters_IgnoreAsset(string fileName, string folderId, string folderPath)
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

            Asset newAsset = new Asset
            {
                FileName = fileName,
                ImageData = new BitmapImage(),
                Folder = new Folder { FolderId = folderId, Path = folderPath }
            };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);

                viewModel.NotifyCatalogChange(null);

                viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
                {
                    Asset = null,
                    Reason = ReasonEnum.AssetCreated
                });

                viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
                {
                    Asset = newAsset,
                    Reason = ReasonEnum.AssetCreated
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
        public void NotifyCatalogChange_DeletedInFromCurrentFolder_RemoveFromAssetList()
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

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);

                viewModel.NotifyCatalogChange(new CatalogChangeCallbackEventArgs
                {
                    Asset = assets[2],
                    Message = statusMessage,
                    Reason = ReasonEnum.AssetDeleted
                });

                viewModel.ObservableAssets.Should().HaveCount(4);
                viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
                viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                viewModel.ObservableAssets[3].FileName.Should().Be("Image5.jpg");
                viewModel.StatusMessage.Should().Be(statusMessage);
            }
        }

        [Theory]
        [InlineData(0, 0)]
        [InlineData(1, 1)]
        [InlineData(2, 2)]
        [InlineData(3, 3)]
        [InlineData(4, 3)]
        public void RemoveAsset_AssetRemovedIsCurrent_RemoveFromAssetList(int currentPosition, int newPosition)
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
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);
                viewModel.ViewerPosition = currentPosition;
                viewModel.RemoveAssets(new Asset[] { assets[currentPosition] });

                viewModel.ViewerPosition.Should().Be(newPosition);
                viewModel.ObservableAssets.Should().HaveCount(4);
            }
        }

        [Fact]
        public void RemoveAsset_SoleElement_ListBeEmptyAndPositionBeforeFirstItem()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() }
            };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);
                viewModel.ViewerPosition = 0;

                viewModel.RemoveAssets(new Asset[] { assets[0] });

                viewModel.ViewerPosition.Should().Be(-1);
                viewModel.ObservableAssets.Should().BeEmpty();
            }
        }

        [Fact]
        public void RemoveAsset_NullAssetFromEmptyAssetList_EmptyAssetList()
        {
            Asset[] assets = new Asset[] { };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);
                viewModel.ViewerPosition = -1;

                viewModel.RemoveAssets(null);

                viewModel.ViewerPosition.Should().Be(-1);
                viewModel.ObservableAssets.Should().BeEmpty();
            }
        }

        [Fact]
        public void RemoveAsset_NullAssetFromNullAssetList_NullAssetList()
        {
            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(null);
                viewModel.ViewerPosition = -1;

                viewModel.RemoveAssets(null);

                viewModel.ViewerPosition.Should().Be(-1);
                viewModel.ObservableAssets.Should().BeNull();
            }
        }

        [Theory]
        [InlineData(AppModeEnum.Thumbnails, Visibility.Visible)]
        [InlineData(AppModeEnum.Viewer, Visibility.Hidden)]
        public void ThumbnailsVisible_ChangeAppMode_RefreshThumbnailsVisible(AppModeEnum appMode, Visibility expected)
        {
            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.ChangeAppMode(appMode);

                viewModel.ThumbnailsVisible.Should().Be(expected);
            }
        }

        [Theory]
        [InlineData(AppModeEnum.Viewer, Visibility.Visible)]
        [InlineData(AppModeEnum.Thumbnails, Visibility.Hidden)]
        public void ViewerVisible_ChangeAppMode_RefreshViewerVisible(AppModeEnum appMode, Visibility expected)
        {
            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.ChangeAppMode(appMode);

                viewModel.ViewerVisible.Should().Be(expected);
            }
        }

        [Theory]
        [InlineData(AppModeEnum.Thumbnails, @"JPPhotoManager v1.0.0.0 - D:\Data - image 4 de 5 - sorted by file name ascending")]
        [InlineData(AppModeEnum.Viewer, @"JPPhotoManager v1.0.0.0 - D:\Data - Image4.jpg - image 4 de 5 - sorted by file name ascending")]
        public void AppTitle_AppMode_ApplyAppTitleFormat(AppModeEnum appMode, string expected)
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
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.Product = "JPPhotoManager";
                viewModel.Version = "v1.0.0.0";
                viewModel.ChangeAppMode(appMode);
                viewModel.SetAssets(assets);
                viewModel.ViewerPosition = 3;

                viewModel.AppTitle.Should().Be(expected);
            }
        }

        [Fact]
        public void SortAssetsByCriteria_NullAssetList_AssetListIsNull()
        {
            Asset[] assets = null;

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);
                viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileName);

                viewModel.ObservableAssets.Should().BeNull();
            }
        }

        [Fact]
        public void SortAssetsByCriteria_EmptyAssetList_AssetListIsEmpty()
        {
            Asset[] assets = new Asset[] { };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);
                viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileName);

                viewModel.ObservableAssets.Should().NotBeNull();
                viewModel.ObservableAssets.Should().BeEmpty();
            }
        }

        [Theory]
        [InlineData(SortCriteriaEnum.FileCreationDateTime)]
        [InlineData(SortCriteriaEnum.FileModificationDateTime)]
        [InlineData(SortCriteriaEnum.FileName)]
        [InlineData(SortCriteriaEnum.FileSize)]
        [InlineData(SortCriteriaEnum.ThumbnailCreationDateTime)]
        [InlineData(SortCriteriaEnum.Undefined)]
        public void SortAssetsByCriteria_SingleItemAssetList_AssetListHasSameSingleItem(SortCriteriaEnum sortCriteria)
        {
            Asset[] assets = new Asset[]
            {
                new Asset
                {
                    FileName="Image1.jpg",
                    ImageData = new BitmapImage(),
                    FileSize = 2048,
                    FileCreationDateTime = DateTime.Now,
                    FileModificationDateTime = DateTime.Now,
                    ThumbnailCreationDateTime = DateTime.Now
                }
            };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);
                viewModel.SortAssetsByCriteria(sortCriteria);

                viewModel.ObservableAssets.Should().NotBeNull();
                viewModel.ObservableAssets.Should().ContainSingle();
                viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
            }
        }

        [Fact]
        public void SortAssetsByCriteria_ByFileNameMultipleAssetsList_AssetListSortedByFileName()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() }
            };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();
                viewModel.SortAssetsByCriteria(SortCriteriaEnum.Undefined);

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
        }

        [Fact]
        public void SortAssetsByCriteria_ByFileNameMultipleAssetsListMultipleTimes_AssetListSortedByFileNameChangingSortingDirection()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage() },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage() }
            };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();
                viewModel.SortAssetsByCriteria(SortCriteriaEnum.Undefined);

                viewModel.SetAssets(assets);

                for (int i = 0; i < 10; i++)
                {
                    viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileName);

                    viewModel.ObservableAssets.Should().NotBeNull();
                    viewModel.ObservableAssets.Should().HaveCount(5);

                    if (i % 2 == 0) // Ascending
                    {
                        viewModel.SortAscending.Should().BeTrue();
                        viewModel.ObservableAssets[0].FileName.Should().Be("Image1.jpg");
                        viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                        viewModel.ObservableAssets[2].FileName.Should().Be("Image3.jpg");
                        viewModel.ObservableAssets[3].FileName.Should().Be("Image4.jpg");
                        viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
                    }
                    else // Descending
                    {
                        viewModel.SortAscending.Should().BeFalse();
                        viewModel.ObservableAssets[0].FileName.Should().Be("Image5.jpg");
                        viewModel.ObservableAssets[1].FileName.Should().Be("Image4.jpg");
                        viewModel.ObservableAssets[2].FileName.Should().Be("Image3.jpg");
                        viewModel.ObservableAssets[3].FileName.Should().Be("Image2.jpg");
                        viewModel.ObservableAssets[4].FileName.Should().Be("Image1.jpg");
                    }
                }
            }
        }

        [Fact]
        public void SortAssetsByCriteria_ByThumbnailCreationDateTimeMultipleAssetsList_AssetListSortedByThumbnailCreationDateTime()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2010, 2, 1) },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2010, 1, 1) },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2010, 8, 1) }
            };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

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
        }

        [Fact]
        public void SortAssetsByCriteria_ByThumbnailCreationDateTimeMultipleAssetsListMultipleTimes_AssetListSortedByThumbnailCreationDateTimeChangingSortingDirection()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2010, 2, 1) },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2010, 1, 1) },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), ThumbnailCreationDateTime = new DateTime(2010, 8, 1) }
            };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);

                for (int i = 0; i < 10; i++)
                {
                    viewModel.SortAssetsByCriteria(SortCriteriaEnum.ThumbnailCreationDateTime);

                    viewModel.ObservableAssets.Should().NotBeNull();
                    viewModel.ObservableAssets.Should().HaveCount(5);

                    if (i % 2 == 0) // Ascending
                    {
                        viewModel.SortAscending.Should().BeTrue();
                        viewModel.ObservableAssets[0].FileName.Should().Be("Image3.jpg");
                        viewModel.ObservableAssets[1].FileName.Should().Be("Image1.jpg");
                        viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                        viewModel.ObservableAssets[3].FileName.Should().Be("Image2.jpg");
                        viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
                    }
                    else // Descending
                    {
                        viewModel.SortAscending.Should().BeFalse();
                        viewModel.ObservableAssets[0].FileName.Should().Be("Image5.jpg");
                        viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                        viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                        viewModel.ObservableAssets[3].FileName.Should().Be("Image1.jpg");
                        viewModel.ObservableAssets[4].FileName.Should().Be("Image3.jpg");
                    }
                }
            }
        }

        [Fact]
        public void SortAssetsByCriteria_ByFileCreationDateTimeMultipleAssetsList_AssetListSortedByFileCreationDateTime()
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
        public void SortAssetsByCriteria_ByFileCreationDateTimeMultipleAssetsListMultipleTimes_AssetListSortedByFileCreationDateTimeChangingSortingDirection()
        {
            Asset[] assets = new Asset[]
            {
                new Asset { FileName="Image5.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image2.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2020, 6, 1) },
                new Asset { FileName="Image1.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2010, 2, 1) },
                new Asset { FileName="Image3.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2010, 1, 1) },
                new Asset { FileName="Image4.jpg", ImageData = new BitmapImage(), FileCreationDateTime = new DateTime(2010, 8, 1) }
            };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(a => a.GetInitialFolder()).Returns(@"D:\Data");

                var viewModel = mock.Create<ApplicationViewModel>();

                viewModel.SetAssets(assets);

                for (int i = 0; i < 10; i++)
                {
                    viewModel.SortAssetsByCriteria(SortCriteriaEnum.FileCreationDateTime);

                    viewModel.ObservableAssets.Should().NotBeNull();
                    viewModel.ObservableAssets.Should().HaveCount(5);

                    if (i % 2 == 0) // Ascending
                    {
                        viewModel.SortAscending.Should().BeTrue();
                        viewModel.ObservableAssets[0].FileName.Should().Be("Image3.jpg");
                        viewModel.ObservableAssets[1].FileName.Should().Be("Image1.jpg");
                        viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                        viewModel.ObservableAssets[3].FileName.Should().Be("Image2.jpg");
                        viewModel.ObservableAssets[4].FileName.Should().Be("Image5.jpg");
                    }
                    else // Descending
                    {
                        viewModel.SortAscending.Should().BeFalse();
                        viewModel.ObservableAssets[0].FileName.Should().Be("Image5.jpg");
                        viewModel.ObservableAssets[1].FileName.Should().Be("Image2.jpg");
                        viewModel.ObservableAssets[2].FileName.Should().Be("Image4.jpg");
                        viewModel.ObservableAssets[3].FileName.Should().Be("Image1.jpg");
                        viewModel.ObservableAssets[4].FileName.Should().Be("Image3.jpg");
                    }
                }
            }
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
