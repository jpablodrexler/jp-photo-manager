using System;
using System.IO;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain
{
    public class Asset
    {
        public string FolderId { get; set; }
        public Folder Folder { get; set; }
        public string FileName { get; set; }
        public long FileSize { get; set; }
        public int PixelWidth { get; set; }
        public int PixelHeight { get; set; }
        public int ThumbnailPixelWidth { get; set; }
        public int ThumbnailPixelHeight { get; set; }
        public Rotation ImageRotation { get; set; }
        public DateTime ThumbnailCreationDateTime { get; set; }
        public string Hash { get; set; }
        public BitmapImage ImageData { get; set; }
        public string FullPath => Path.Combine(this.Folder.Path, this.FileName);

        public override bool Equals(object obj)
        {
            
            Asset asset = obj as Asset;

            return asset != null && asset.FolderId == this.FolderId && asset.FileName == this.FileName;
        }

        public override int GetHashCode()
        {
            return (!string.IsNullOrEmpty(this.FolderId) ? this.FolderId.GetHashCode() : base.GetHashCode()) + (!string.IsNullOrEmpty(this.FileName) ? this.FileName.GetHashCode() : base.GetHashCode());
        }

        public override string ToString()
        {
            return this.FileName;
        }
    }
}
