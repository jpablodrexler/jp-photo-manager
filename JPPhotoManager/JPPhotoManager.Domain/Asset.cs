using Newtonsoft.Json;
using System;
using System.IO;
using System.Threading;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain
{
    public class Asset
    {
        private const decimal ONE_KILOBYTE = 1024;
        private const decimal ONE_MEGABYTE = ONE_KILOBYTE * 1024;
        private const decimal ONE_GIGABYTE = ONE_MEGABYTE * 1024;
        private const string KILOBYTE_UNIT = "KB";
        private const string MEGABYTE_UNIT = "MB";
        private const string GIGABYTE_UNIT = "GB";

        public string FolderId { get; set; }

        [JsonIgnore]
        public Folder Folder { get; set; }
        public string FileName { get; set; }
        public long FileSize { get; set; }
        public int PixelWidth { get; set; }
        public int PixelHeight { get; set; }
        public DateTime ThumbnailCreationDateTime { get; set; }
        public string Hash { get; set; }

        [JsonIgnore]
        public BitmapImage ImageData { get; set; }

        [JsonIgnore]
        public string FullPath => Path.Combine(this.Folder.Path, this.FileName);

        public string FormattedFileSize
        {
            get
            {
                string result = string.Empty;

                if (this.FileSize < ONE_KILOBYTE)
                {
                    result = this.FileSize + " bytes";
                }
                else
                {
                    decimal value_bytes = this.FileSize;
                    decimal value = 0;
                    string unit = "";

                    if (this.FileSize >= ONE_KILOBYTE && this.FileSize < ONE_MEGABYTE)
                    {
                        value = value_bytes / ONE_KILOBYTE;
                        unit = KILOBYTE_UNIT;
                    }
                    else if (this.FileSize >= ONE_MEGABYTE && this.FileSize < ONE_GIGABYTE)
                    {
                        value = value_bytes / ONE_MEGABYTE;
                        unit = MEGABYTE_UNIT;
                    }
                    else if (this.FileSize >= ONE_GIGABYTE)
                    {
                        value = value_bytes / ONE_GIGABYTE;
                        unit = GIGABYTE_UNIT;
                    }

                    result = value.ToString("0.0", Thread.CurrentThread.CurrentCulture) + " " + unit;
                }

                return result;
            }
        }

        public string FormattedPixelSize
        {
            get
            {
                return $"{this.PixelWidth}x{this.PixelHeight}";
            }
        }

        public override bool Equals(object obj)
        {
            
            Asset asset = obj as Asset;

            return asset != null && asset.FolderId == this.FolderId && asset.FileName == this.FileName;
        }

        public override int GetHashCode()
        {
            return (!string.IsNullOrEmpty(this.FolderId) ? this.FolderId.GetHashCode() : base.GetHashCode()) + (!string.IsNullOrEmpty(this.FileName) ? this.FileName.GetHashCode() : base.GetHashCode());
        }
    }
}
