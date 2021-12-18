using System.IO;
using System.Text.RegularExpressions;
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

        public static bool IsValidBatchFormat(string batchFormat)
        {
            bool isValid = !string.IsNullOrWhiteSpace(batchFormat);

            if (isValid)
            {
                batchFormat = batchFormat.Trim();
                isValid = !batchFormat.StartsWith(".")
                    && !batchFormat.EndsWith(".")
                    && !batchFormat.EndsWith("<")
                    && !batchFormat.EndsWith(">");

                Regex regex = new("(<[#A-Za-z0-9:]*>)", RegexOptions.IgnoreCase);
                var matches = regex.Matches(batchFormat);
                var remainingBatchFormat = batchFormat;

                // Identifies if the complete tags have supported expressions.
                foreach (Match match in matches)
                {
                    string tag = match.Value[1..^1];
                    isValid = isValid
                        && match.Success
                        && (string.Compare(tag, "#", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "##", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "###", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "####", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "#####", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "######", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "#######", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "########", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "#########", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "##########", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "PixelWidth", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "PixelHeight", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "CreationDate", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "CreationDate:yy", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "CreationDate:yyyy", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "CreationDate:M", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "CreationDate:MM", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "CreationDate:MMM", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "CreationDate:d", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "CreationDate:dd", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "CreationTime", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "ModificationDate", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "ModificationDate:yy", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "ModificationDate:yyyy", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "ModificationDate:M", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "ModificationDate:MM", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "ModificationDate:MMM", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "ModificationDate:d", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "ModificationDate:dd", StringComparison.OrdinalIgnoreCase) == 0
                        || string.Compare(tag, "ModificationTime", StringComparison.OrdinalIgnoreCase) == 0);
                    remainingBatchFormat = remainingBatchFormat.Replace(match.Value, string.Empty);
                }

                // Identifies if the batch format has any unexpected values after removing the complete tags.
                isValid = isValid && remainingBatchFormat
                    .IndexOfAny(new[] { '/', ':', '*', '?', '"', '<', '>', '|' }) < 0;
            }

            return isValid;
        }

        public string ComputeTargetFileName(string batchFormat, int ordinal)
        {
            bool isValid = IsValidBatchFormat(batchFormat);

            if (isValid)
            {
                batchFormat = batchFormat.Trim();

                // If the batch format is just an extension,
                // return the current filename.
                if (batchFormat.IndexOf(".") == 0)
                {
                    batchFormat = FileName;
                }
                else
                {
                    int ordinalStart = batchFormat.IndexOf("<#");
                    int ordinalEnd = batchFormat.LastIndexOf("#>");

                    if (ordinalStart >= 0)
                    {
                        string ordinalPlaceholder = batchFormat.Substring(ordinalStart + 1, ordinalEnd - ordinalStart);
                        string ordinalFormat = new('0', ordinalPlaceholder.Length);
                        string ordinalString = ordinal.ToString(ordinalFormat);
                        batchFormat = batchFormat.Replace("<" + ordinalPlaceholder + ">", ordinalString);
                    }

                    batchFormat = batchFormat.Replace("<PixelWidth>", PixelWidth.ToString(), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<PixelHeight>", PixelHeight.ToString(), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<CreationDate>", FileCreationDateTime.ToString("yyyyMMdd"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<CreationDate:yy>", FileCreationDateTime.ToString("yy"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<CreationDate:yyyy>", FileCreationDateTime.ToString("yyyy"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<CreationDate:M>", FileCreationDateTime.ToString("M"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<CreationDate:MM>", FileCreationDateTime.ToString("MM"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<CreationDate:MMMM>", FileCreationDateTime.ToString("MMMM"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<CreationDate:d>", FileCreationDateTime.ToString("d"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<CreationDate:dd>", FileCreationDateTime.ToString("dd"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<CreationTime>", FileCreationDateTime.ToString("HHmmss"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<ModificationDate>", FileModificationDateTime.ToString("yyyyMMdd"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<ModificationDate:yy>", FileModificationDateTime.ToString("yy"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<ModificationDate:yyyy>", FileModificationDateTime.ToString("yyyy"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<ModificationDate:M>", FileModificationDateTime.ToString("M"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<ModificationDate:MM>", FileModificationDateTime.ToString("MM"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<ModificationDate:MMMM>", FileModificationDateTime.ToString("MMMM"), StringComparison.OrdinalIgnoreCase);
                    batchFormat = batchFormat.Replace("<ModificationTime>", FileModificationDateTime.ToString("HHmmss"), StringComparison.OrdinalIgnoreCase);
                }
            }
            else
            {
                batchFormat = FileName;
            }

            string newFullPath = Folder != null ? Path.Combine(Folder.Path, batchFormat) : batchFormat;

            return isValid && newFullPath.Length <= MAX_PATH_LENGTH ? batchFormat : string.Empty;
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
