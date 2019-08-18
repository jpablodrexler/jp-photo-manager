using Microsoft.VisualStudio.TestTools.UnitTesting;
using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace JPPhotoManager.Test
{
    [TestClass]
    public class AssetTests
    {
        [TestMethod]
        public void GetFormattedPixelSizeTest1()
        {
            Asset asset = new Asset
            {
                PixelWidth = 1920,
                PixelHeight = 1080
            };

            string result = asset.FormattedPixelSize;
            Assert.AreEqual("1920x1080", result);
        }

        [TestMethod]
        public void GetFormattedPixelSizeTest2()
        {
            Asset asset = new Asset
            {
                PixelWidth = 1024,
                PixelHeight = 768
            };

            string result = asset.FormattedPixelSize;
            Assert.AreEqual("1024x768", result);
        }
    }
}