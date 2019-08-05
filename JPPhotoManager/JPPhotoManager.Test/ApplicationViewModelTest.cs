using System;
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
    }
}
