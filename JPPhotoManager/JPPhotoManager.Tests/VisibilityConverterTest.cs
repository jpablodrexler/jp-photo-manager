using FluentAssertions;
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
            result.Should().Be(Visibility.Visible);
        }

        [Fact]
        public void GetVisibilityHiddenTest()
        {
            VisibilityConverter converter = new VisibilityConverter();
            Visibility result = (Visibility)converter.Convert(new NamedObject(), typeof(object), null, new CultureInfo("en-US"));
            result.Should().Be(Visibility.Hidden);
        }

        [Fact]
        public void ConvertBackTest()
        {
            VisibilityConverter converter = new VisibilityConverter();
            Func<object> function = () => converter.ConvertBack(Visibility.Visible, typeof(object), null, new CultureInfo("en-US"));
            function.Should().Throw<NotImplementedException>();
        }
    }
}
