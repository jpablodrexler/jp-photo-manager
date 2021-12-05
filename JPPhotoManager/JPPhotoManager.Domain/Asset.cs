using System.IO;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain
{
    public class Asset
    {
        private const int MAX_PATH_LENGTH = 256;

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
        public string FullPath => Folder != null ? Path.Combine(Folder.Path, FileName) : FileName;
        public DateTime FileCreationDateTime { get; set; }
        public DateTime FileModificationDateTime { get; set; }

        public string ComputeNewName(string batchFormat, int ordinal)
        {
            string newName = FileName;

            if (!string.IsNullOrWhiteSpace(batchFormat))
            {
                batchFormat = batchFormat.Trim();
                int ordinalStart = batchFormat.IndexOf("#");
                int ordinalEnd = batchFormat.LastIndexOf("#");

                if (ordinalStart >= 0)
                {
                    string ordinalPlaceholder = batchFormat.Substring(ordinalStart, ordinalEnd - ordinalStart + 1);
                    string ordinalFormat = new('0', ordinalPlaceholder.Length);
                    string ordinalString = ordinal.ToString(ordinalFormat);
                    newName = batchFormat.Replace(ordinalPlaceholder, ordinalString);
                }
            }

            string newFullPath = Folder != null ? Path.Combine(Folder.Path, newName) : newName;

            return newFullPath.Length <= MAX_PATH_LENGTH ? newName : string.Empty;
        }

        public override bool Equals(object obj)
        {
            Asset asset = obj as Asset;

            return asset != null && asset.FolderId == FolderId && asset.FileName == FileName;
        }

        public override int GetHashCode()
        {
            return (!string.IsNullOrEmpty(FolderId) ? FolderId.GetHashCode() : base.GetHashCode()) + (!string.IsNullOrEmpty(FileName) ? FileName.GetHashCode() : base.GetHashCode());
        }

        public override string ToString()
        {
            return FileName;
        }
    }
}
