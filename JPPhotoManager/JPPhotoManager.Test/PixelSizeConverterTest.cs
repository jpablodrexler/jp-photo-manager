using JPPhotoManager.Converters;
using JPPhotoManager.Domain;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Threading;

namespace JPPhotoManager.Test
{
    [TestClass]
    public class PixelSizeConverterTest
    {
        [TestMethod]
        public void GetFormattedPixelSizeTest1()
        {
            PixelSizeConverter converter = new PixelSizeConverter();
            Asset asset = new Asset
            {
                PixelWidth = 1920,
                PixelHeight = 1080
            };

            string result = (string)converter.Convert(asset, typeof(Asset), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual("1920x1080 pixels", result);
        }

        [TestMethod]
        public void GetFormattedPixelSizeTest2()
        {
            PixelSizeConverter converter = new PixelSizeConverter();
            Asset asset = new Asset
            {
                PixelWidth = 1024,
                PixelHeight = 768
            };

            string result = (string)converter.Convert(asset, typeof(Asset), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual("1024x768 pixels", result);
        }
    }
}
