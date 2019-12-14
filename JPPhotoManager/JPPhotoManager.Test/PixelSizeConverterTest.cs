using JPPhotoManager.Converters;
using JPPhotoManager.Domain;
using System.Threading;
using Xunit;

namespace JPPhotoManager.Test
{
    public class PixelSizeConverterTest
    {
        [Fact]
        public void GetFormattedPixelSizeTest1()
        {
            PixelSizeConverter converter = new PixelSizeConverter();
            Asset asset = new Asset
            {
                PixelWidth = 1920,
                PixelHeight = 1080
            };

            string result = (string)converter.Convert(asset, typeof(Asset), null, Thread.CurrentThread.CurrentCulture);
            Assert.Equal("1920x1080 pixels", result);
        }

        [Fact]
        public void GetFormattedPixelSizeTest2()
        {
            PixelSizeConverter converter = new PixelSizeConverter();
            Asset asset = new Asset
            {
                PixelWidth = 1024,
                PixelHeight = 768
            };

            string result = (string)converter.Convert(asset, typeof(Asset), null, Thread.CurrentThread.CurrentCulture);
            Assert.Equal("1024x768 pixels", result);
        }
    }
}
