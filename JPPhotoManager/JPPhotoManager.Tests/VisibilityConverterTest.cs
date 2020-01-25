using JPPhotoManager.UI.Converters;
using System;
using System.Globalization;
using System.Windows;
using Xunit;

namespace JPPhotoManager.Tests
{
    class NamedObject
    {

    }

    public class VisibilityConverterTest
    {
        [Fact]
        public void GetVisibilityVisibleTest()
        {
            VisibilityConverter converter = new VisibilityConverter();
            Visibility result = (Visibility)converter.Convert("A string", typeof(object), null, new CultureInfo("en-US"));
            Assert.Equal(Visibility.Visible, result);
        }

        [Fact]
        public void GetVisibilityHiddenTest()
        {
            VisibilityConverter converter = new VisibilityConverter();
            Visibility result = (Visibility)converter.Convert(new NamedObject(), typeof(object), null, new CultureInfo("en-US"));
            Assert.Equal(Visibility.Hidden, result);
        }

        [Fact]
        public void ConvertBackTest()
        {
            VisibilityConverter converter = new VisibilityConverter();
            Assert.Throws<NotImplementedException>(() => converter.ConvertBack(Visibility.Visible, typeof(object), null, new CultureInfo("en-US")));
        }
    }
}
