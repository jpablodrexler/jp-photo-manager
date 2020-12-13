using FluentAssertions;
using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using Moq;
using System.Collections.Generic;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class DuplicatedAssetsViewModelTest
    {
        [Fact]
        public void ViewModelTest()
        {
            List<DuplicatedAssetCollection> duplicatedAssetSets = new List<DuplicatedAssetCollection>
            {
                new DuplicatedAssetCollection(new List<Asset>
                {
                    new Asset { FileName = "Image 2.jpg" },
                    new Asset { FileName = "Image 2 duplicated.jpg" }
                })
            };
            
            Mock<IApplication> mock = new Mock<IApplication>();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");
            mock.Setup(app => app.LoadThumbnailAndFileInformation(It.IsAny<Asset>()))
                .Callback<Asset>(a => a.ImageData = new System.Windows.Media.Imaging.BitmapImage());
            
            DuplicatedAssetsViewModel viewModel = new DuplicatedAssetsViewModel(mock.Object)
            {
                DuplicatedAssetCollectionSets = duplicatedAssetSets
            };

            viewModel.DuplicatedAssetCollectionSetsPosition.Should().Be(0);
            viewModel.DuplicatedAssetPosition.Should().Be(0);
            viewModel.DuplicatedAssetCollectionSets.Should().ContainSingle();
            viewModel.CurrentDuplicatedAssetCollection.Should().NotBeNull();
            viewModel.CurrentDuplicatedAsset.FileName.Should().Be("Image 2.jpg");
            
            viewModel.DuplicatedAssetPosition = 1;
            
            viewModel.CurrentDuplicatedAsset.FileName.Should().Be("Image 2 duplicated.jpg");
        }

        [Fact]
        public void ViewModelEmptyDuplicatedAssetCollectionTest()
        {
            List<DuplicatedAssetCollection> duplicatedAssetSets = new List<DuplicatedAssetCollection>();

            Mock<IApplication> mock = new Mock<IApplication>();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            DuplicatedAssetsViewModel viewModel = new DuplicatedAssetsViewModel(mock.Object)
            {
                DuplicatedAssetCollectionSets = duplicatedAssetSets
            };

            viewModel.DuplicatedAssetCollectionSetsPosition = -1;
            viewModel.DuplicatedAssetPosition = -1;

            viewModel.DuplicatedAssetCollectionSetsPosition.Should().Be(-1);
            viewModel.DuplicatedAssetPosition.Should().Be(-1);
            viewModel.CurrentDuplicatedAssetCollection.Should().BeNull();
            viewModel.CurrentDuplicatedAsset.Should().BeNull();
        }

        [Fact]
        public void ViewModelNullDuplicatedAssetCollectionTest()
        {
            List<DuplicatedAssetCollection> duplicatedAssetSets = null;

            Mock<IApplication> mock = new Mock<IApplication>();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            DuplicatedAssetsViewModel viewModel = new DuplicatedAssetsViewModel(mock.Object)
            {
                DuplicatedAssetCollectionSets = duplicatedAssetSets
            };

            viewModel.DuplicatedAssetCollectionSetsPosition = -1;
            viewModel.DuplicatedAssetPosition = -1;

            viewModel.DuplicatedAssetCollectionSetsPosition.Should().Be(-1);
            viewModel.DuplicatedAssetPosition.Should().Be(-1);
            viewModel.CurrentDuplicatedAssetCollection.Should().BeNull();
            viewModel.CurrentDuplicatedAsset.Should().BeNull();
        }
    }
}
