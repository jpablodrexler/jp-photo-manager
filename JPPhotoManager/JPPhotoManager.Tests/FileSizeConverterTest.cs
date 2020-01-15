using JPPhotoManager.UI.Converters;
using System.Globalization;
using System.Threading;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class FileSizeConverterTest
    {
        [Theory]
        [InlineData(656, "656 bytes")]
        [InlineData(17734, "17.3 KB")]
        [InlineData(20480, "20.0 KB")]
        [InlineData(562688, "549.5 KB")]
        [InlineData(565248, "552.0 KB")]
        [InlineData(54712102, "52.2 MB")]
        [InlineData(54956032, "52.4 MB")]
        [InlineData(1742919342, "1.6 GB")]
        [InlineData(1753653248, "1.6 GB")]
        [InlineData(24998490626, "23.3 GB")]
        [InlineData(25073561600, "23.4 GB")]
        public void GetFormattedFileSizeTest(long size, string expected)
        {
            FileSizeConverter converter = new FileSizeConverter();
            string result = (string)converter.Convert(size, typeof(long), null, new CultureInfo("en-US"));
            Assert.Equal(expected, result);
        }
    }
}
