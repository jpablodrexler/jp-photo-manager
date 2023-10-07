using JPPhotoManager.Domain.Entities;
using System;
using System.Globalization;
using System.Windows.Data;

namespace JPPhotoManager.UI.Converters
{
    public class PixelSizeConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            string result = "";

            if (value != null)
            {
                result = value is Asset asset ? $"{asset.PixelWidth}x{asset.PixelHeight} pixels" : "";
            }

            return result;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}
