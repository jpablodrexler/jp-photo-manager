using System;
using System.Globalization;
using System.Windows.Data;

namespace JPPhotoManager.UI.Converters
{
    public class FileSizeConverter : IValueConverter
    {
        private const long ONE_KILOBYTE = 1024;
        private const long ONE_MEGABYTE = ONE_KILOBYTE * 1024;
        private const long ONE_GIGABYTE = ONE_MEGABYTE * 1024;
        private const string KILOBYTE_UNIT = "KB";
        private const string MEGABYTE_UNIT = "MB";
        private const string GIGABYTE_UNIT = "GB";

        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            long FileSize = System.Convert.ToInt64(value, culture);
            string result;

            if (FileSize < ONE_KILOBYTE)
            {
                result = FileSize + " bytes";
            }
            else
            {
                decimal bytes = FileSize;
                decimal decimal_value;
                string unit;

                bool sizeInKb = (FileSize >= ONE_KILOBYTE && FileSize < ONE_MEGABYTE && FileSize < ONE_GIGABYTE);
                bool sizeInMb = (FileSize >= ONE_MEGABYTE && FileSize < ONE_GIGABYTE);

                if (sizeInKb)
                {
                    decimal_value = bytes / ONE_KILOBYTE;
                    unit = KILOBYTE_UNIT;
                }
                else if (!sizeInKb && sizeInMb)
                {
                    decimal_value = bytes / ONE_MEGABYTE;
                    unit = MEGABYTE_UNIT;
                }
                else
                {
                    decimal_value = bytes / ONE_GIGABYTE;
                    unit = GIGABYTE_UNIT;
                }
                
                result = decimal_value.ToString("0.0", culture) + " " + unit;
            }

            return result;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}
