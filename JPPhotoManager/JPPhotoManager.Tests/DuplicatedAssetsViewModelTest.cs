using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using Moq;
using System;
using System.Collections.Generic;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class DuplicatedAssetsViewModelTest
    {
        [Fact]
        public void ViewModelTest()
        {
            List<List<Asset>> duplicatedAssetSets = new List<List<Asset>>
            {
                new List<Asset>(new List<Asset>
                {
                    new Asset { FileName = "Image 2.jpg" },
                    new Asset { FileName = "Image 2 duplicated.jpg" }
                })
            };

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(app => app.GetInitialFolder()).Returns(@"C:\");
                mock.Mock<IApplication>().Setup(app => app.LoadThumbnail(It.IsAny<Asset>()))
                    .Callback<Asset>(a => a.ImageData = new System.Windows.Media.Imaging.BitmapImage());

                FindDuplicatedAssetsViewModel viewModel = mock.Create<FindDuplicatedAssetsViewModel>();
                viewModel.SetDuplicates(duplicatedAssetSets);

                viewModel.DuplicatedAssetCollectionSetsPosition.Should().Be(0);
                viewModel.DuplicatedAssetPosition.Should().Be(0);
                viewModel.ObservableDuplicatedAssetCollectionSets.Should().ContainSingle();
                viewModel.CurrentDuplicatedAssetCollection.Should().NotBeNull();
                viewModel.CurrentDuplicatedAsset.Asset.FileName.Should().Be("Image 2.jpg");

                viewModel.DuplicatedAssetPosition = 1;

                viewModel.CurrentDuplicatedAsset.Asset.FileName.Should().Be("Image 2 duplicated.jpg");
            }
        }

        [Fact]
        public void ViewModel_EmptyDuplicatedAssetCollectionTest()
        {
            List<List<Asset>> duplicatedAssetSets = new List<List<Asset>>();

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(app => app.GetInitialFolder()).Returns(@"C:\");

                FindDuplicatedAssetsViewModel viewModel = mock.Create<FindDuplicatedAssetsViewModel>();
                viewModel.SetDuplicates(duplicatedAssetSets);

                viewModel.DuplicatedAssetCollectionSetsPosition = -1;
                viewModel.DuplicatedAssetPosition = -1;

                viewModel.ObservableDuplicatedAssetCollectionSets.Should().BeEmpty();
                viewModel.DuplicatedAssetCollectionSetsPosition.Should().Be(-1);
                viewModel.DuplicatedAssetPosition.Should().Be(-1);
                viewModel.CurrentDuplicatedAssetCollection.Should().BeNull();
                viewModel.CurrentDuplicatedAsset.Should().BeNull();
            }
        }

        [Fact]
        public void ViewModel_NullDuplicatedAssetCollectionTest()
        {
            List<List<Asset>> duplicatedAssetSets = null;

            using (var mock = AutoMock.GetLoose())
            {
                mock.Mock<IApplication>().Setup(app => app.GetInitialFolder()).Returns(@"C:\");

                FindDuplicatedAssetsViewModel viewModel = mock.Create<FindDuplicatedAssetsViewModel>();

                Action action = new Action(() => viewModel.SetDuplicates(duplicatedAssetSets));
                action.Should().Throw<ArgumentNullException>();
            }
        }
    }
}
