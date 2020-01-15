using JPPhotoManager.UI.Converters;
using JPPhotoManager.Domain;
using System.Threading;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class PixelSizeConverterTest
    {
        [Theory]
        [InlineData(1920, 1080, "1920x1080 pixels")]
        [InlineData(1024, 768, "1024x768 pixels")]
        public void GetFormattedPixelSizeTest(int width, int height, string expected)
        {
            PixelSizeConverter converter = new PixelSizeConverter();
            Asset asset = new Asset
            {
                PixelWidth = width,
                PixelHeight = height
            };

            string result = (string)converter.Convert(asset, typeof(Asset), null, Thread.CurrentThread.CurrentCulture);
            Assert.Equal(expected, result);
        }
    }
}
