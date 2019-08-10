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
        public void GetFormattedFileSizeBytesTest()
        {
            Asset asset = new Asset
            {
                FileSize = 656
            };

            string result = asset.FormattedFileSize;
            Assert.AreEqual("656 bytes", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeKilobytesTest1()
        {
            Asset asset = new Asset
            {
                FileSize = 17734
            };

            string result = asset.FormattedFileSize;
            Assert.AreEqual(17.3 + " KB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeKilobytesTest2()
        {
            Asset asset = new Asset
            {
                FileSize = 20480
            };

            string result = asset.FormattedFileSize;
            Assert.AreEqual(20.ToString("0.0") + " KB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeKilobytesTest3()
        {
            Asset asset = new Asset
            {
                FileSize = 562688
            };

            string result = asset.FormattedFileSize;
            Assert.AreEqual(549.5 + " KB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeKilobytesTest4()
        {
            Asset asset = new Asset
            {
                FileSize = 565248
            };

            string result = asset.FormattedFileSize;
            Assert.AreEqual(552.ToString("0.0") + " KB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeMegabytesTest1()
        {
            Asset asset = new Asset
            {
                FileSize = 54712102
            };

            string result = asset.FormattedFileSize;
            Assert.AreEqual(52.2 + " MB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeMegabytesTest2()
        {
            Asset asset = new Asset
            {
                FileSize = 54956032
            };

            string result = asset.FormattedFileSize;
            Assert.AreEqual(52.4 + " MB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeGigabytesTest1()
        {
            Asset asset = new Asset
            {
                FileSize = 1742919342
            };

            string result = asset.FormattedFileSize;
            Assert.AreEqual(1.6 + " GB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeGigabytesTest2()
        {
            Asset asset = new Asset
            {
                FileSize = 1753653248
            };

            string result = asset.FormattedFileSize;
            Assert.AreEqual(1.6 + " GB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeGigabytesTest3()
        {
            Asset asset = new Asset
            {
                FileSize = 24998490626
            };

            string result = asset.FormattedFileSize;
            Assert.AreEqual(23.3 + " GB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeGigabytesTest4()
        {
            Asset asset = new Asset
            {
                FileSize = 25073561600
            };

            string result = asset.FormattedFileSize;
            Assert.AreEqual(23.4 + " GB", result);
        }

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