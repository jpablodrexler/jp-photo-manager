using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using Moq;
using System.Collections.Generic;
using System.Collections.ObjectModel;
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

            DuplicatedAssetsViewModel viewModel = new DuplicatedAssetsViewModel(mock.Object)
            {
                DuplicatedAssetCollectionSets = duplicatedAssetSets
            };

            Assert.Equal(0, viewModel.DuplicatedAssetCollectionSetsPosition);
            Assert.Equal(0, viewModel.DuplicatedAssetPosition);
            Assert.Single(viewModel.DuplicatedAssetCollectionSets);
            Assert.NotNull(viewModel.CurrentDuplicatedAssetCollection);

            Assert.Equal("Image 2.jpg", viewModel.CurrentDuplicatedAsset.FileName);
            
            viewModel.DuplicatedAssetPosition = 1;
            
            Assert.Equal("Image 2 duplicated.jpg", viewModel.CurrentDuplicatedAsset.FileName);
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

            Assert.Equal(-1, viewModel.DuplicatedAssetCollectionSetsPosition);
            Assert.Equal(-1, viewModel.DuplicatedAssetPosition);
            Assert.Null(viewModel.CurrentDuplicatedAssetCollection);
            Assert.Null(viewModel.CurrentDuplicatedAsset);
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

            Assert.Equal(-1, viewModel.DuplicatedAssetCollectionSetsPosition);
            Assert.Equal(-1, viewModel.DuplicatedAssetPosition);
            Assert.Null(viewModel.CurrentDuplicatedAssetCollection);
            Assert.Null(viewModel.CurrentDuplicatedAsset);
        }
    }
}
