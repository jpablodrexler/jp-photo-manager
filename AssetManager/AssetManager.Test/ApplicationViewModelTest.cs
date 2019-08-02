using System;
using AssetManager.Application;
using AssetManager.Domain;
using AssetManager.ViewModels;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Moq;

namespace AssetManager.Test
{
    [TestClass]
    public class ApplicationViewModelTest
    {
        [TestMethod]
        public void TestChangeAppMode()
        {
            Mock<IAssetManagerApplication> mock = new Mock<IAssetManagerApplication>();
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
    }
}
