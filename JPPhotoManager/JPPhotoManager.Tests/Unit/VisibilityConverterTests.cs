using FluentAssertions;
using JPPhotoManager.UI.Converters;
using System.Globalization;
using System.Windows;
using Xunit;

namespace JPPhotoManager.Tests.Unit
{
    class NamedObject
    {

    }

    public class VisibilityConverterTests
    {
        [Fact]
        public void GetVisibilityVisibleTest()
        {
            VisibilityConverter converter = new();
            Visibility result = (Visibility)converter.Convert("A string", typeof(object), null, new CultureInfo("en-US"));
            result.Should().Be(Visibility.Visible);
        }

        [Fact]
        public void GetVisibilityHiddenTest()
        {
            VisibilityConverter converter = new();
            Visibility result = (Visibility)converter.Convert(new NamedObject(), typeof(object), null, new CultureInfo("en-US"));
            result.Should().Be(Visibility.Hidden);
        }

        [Fact]
        public void ConvertBackTest()
        {
            VisibilityConverter converter = new();
            Func<object> function = () => converter.ConvertBack(Visibility.Visible, typeof(object), null, new CultureInfo("en-US"));
            function.Should().Throw<NotImplementedException>();
        }
    }
}
