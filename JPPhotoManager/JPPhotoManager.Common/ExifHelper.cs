using System.IO;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Common
{
    public static class ExifHelper
    {
        public static ushort? GetExifOrientation(byte[] buffer)
        {
            ushort? result = null;

            using (MemoryStream stream = new(buffer))
            {
                BitmapFrame bitmapFrame = BitmapFrame.Create(stream);
                BitmapMetadata bitmapMetadata = bitmapFrame.Metadata as BitmapMetadata;

                if (bitmapMetadata != null && bitmapMetadata.ContainsQuery("System.Photo.Orientation"))
                {
                    object value = bitmapMetadata.GetQuery("System.Photo.Orientation");

                    if (value != null)
                    {
                        result = (ushort)value;
                    }
                }
            }

            return result;
        }

        public static Rotation GetImageRotation(ushort exifOrientation)
        {
            Rotation rotation = Rotation.Rotate0;

            switch (exifOrientation)
            {
                case 1:
                    rotation = Rotation.Rotate0;
                    break;
                case 2:
                    rotation = Rotation.Rotate0; // FlipX
                    break;
                case 3:
                    rotation = Rotation.Rotate180;
                    break;
                case 4:
                    rotation = Rotation.Rotate180; // FlipX
                    break;
                case 5:
                    rotation = Rotation.Rotate90; // FlipX
                    break;
                case 6:
                    rotation = Rotation.Rotate90;
                    break;
                case 7:
                    rotation = Rotation.Rotate270; // FlipX
                    break;
                case 8:
                    rotation = Rotation.Rotate270;
                    break;
                default:
                    rotation = Rotation.Rotate0;
                    break;
            }

            return rotation;
        }
    }
}
