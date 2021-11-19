using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.Converters;
using System.Globalization;
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
            result.Should().Be(expected);
        }

        [Fact]
        public void ConvertBackTest()
        {
            PixelSizeConverter converter = new PixelSizeConverter();
            Func<object> function = () => converter.ConvertBack("1920x1080 pixels", typeof(string), null, new CultureInfo("en-US"));
            function.Should().Throw<NotImplementedException>();
        }
    }
}
