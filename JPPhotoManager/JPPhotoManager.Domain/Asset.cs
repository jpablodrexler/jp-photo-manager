using JPPhotoManager.Domain.Interfaces;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.IO;
using System.Text.RegularExpressions;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain
{
    public class Asset
    {
        private const int MAX_PATH_LENGTH = 256;

        [Key]
        public string AssetId { get; set; }
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
        
        [NotMapped]
        public BitmapImage ImageData { get; set; }

        [NotMapped]
        public string FullPath => Folder != null ? Path.Combine(Folder.Path, FileName) : FileName;

        public string ThumbnailBlobName => AssetId + ".bin";
        public DateTime FileCreationDateTime { get; set; }
        public DateTime FileModificationDateTime { get; set; }

        public static bool IsValidBatchFormat(string batchFormat)
        {
            bool isValid = !string.IsNullOrWhiteSpace(batchFormat);

            if (isValid)
            {
                batchFormat = batchFormat.Trim();
                isValid = IsValidBatchFormatStart(batchFormat, isValid);
                isValid = IsValidBatchFormatEnd(batchFormat, isValid);
                (isValid, string remainingBatchFormat) = IdentifySupportedTags(isValid, batchFormat);
                isValid = IdentifyUnexpectedCharacters(isValid, batchFormat, remainingBatchFormat);
            }

            return isValid;
        }

        private static bool IsValidBatchFormatStart(string batchFormat, bool isValid)
        {
            return batchFormat.StartsWith(".") ? batchFormat.StartsWith("..") : isValid;
        }

        private static bool IsValidBatchFormatEnd(string batchFormat, bool isValid)
        {
            return isValid
                && !batchFormat.EndsWith(".")
                && !batchFormat.EndsWith("<")
                && !batchFormat.EndsWith(">");
        }

        private static (bool isValid, string remainingBatchFormat) IdentifySupportedTags(bool isValid, string batchFormat)
        {
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
                    || string.Compare(tag, "CreationDate:MM", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "CreationDate:MMMM", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "CreationDate:dd", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "CreationTime:HH", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "CreationTime:mm", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "CreationTime:ss", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "CreationTime", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "ModificationDate", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "ModificationDate:yy", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "ModificationDate:yyyy", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "ModificationDate:MM", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "ModificationDate:MMMM", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "ModificationDate:dd", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "ModificationTime:HH", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "ModificationTime:mm", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "ModificationTime:ss", StringComparison.OrdinalIgnoreCase) == 0
                    || string.Compare(tag, "ModificationTime", StringComparison.OrdinalIgnoreCase) == 0);
                remainingBatchFormat = remainingBatchFormat.Replace(match.Value, string.Empty);
            }

            return (isValid, remainingBatchFormat);
        }

        /// <summary>
        /// Identifies if the batch format has any unexpected values after removing the complete tags.
        /// </summary>
        /// <param name="isValid">If the batch format is valid.</param>
        /// <param name="remainingBatchFormat">The remaining of the batch format
        /// after removing the supported tags.</param>
        /// <returns>If the batch format has any unexpected values.</returns>
        private static bool IdentifyUnexpectedCharacters(bool isValid, string batchFormat, string remainingBatchFormat)
        {
            isValid = isValid && remainingBatchFormat
                .IndexOfAny(new[] { '/', '*', '?', '"', '<', '>', '|', '#' }) < 0;

            return isValid && (IsAbsolutePath(batchFormat) ? remainingBatchFormat.IndexOf(':', 3) < 0 :
                remainingBatchFormat.IndexOf(':') < 0);
        }

        private static bool IsAbsolutePath(string batchFormat)
        {
            return batchFormat.Length > 3 && batchFormat.Substring(1, 2) == @":\";
        }

        public string ComputeTargetPath(string batchFormat,
            int ordinal,
            IFormatProvider provider,
            IStorageService storageService,
            bool overwriteExistingTargetFiles)
        {
            bool isValid = IsValidBatchFormat(batchFormat);

            if (isValid)
            {
                batchFormat = batchFormat.Trim();

                // If the batch format is just an extension,
                // return the current filename.
                if (batchFormat == ".")
                {
                    batchFormat = FileName;
                }
                else
                {
                    (batchFormat, bool includesOrdinal) = ReplaceSupportedTagsWithValues(batchFormat, ordinal, provider);
                    (Folder? folder, batchFormat) = ResolveTargetFolder(batchFormat);
                    batchFormat = folder != null ? Path.Combine(folder.Path, batchFormat) : string.Empty;
                    batchFormat = !overwriteExistingTargetFiles ?
                        ComputeUniqueTargetPath(folder, batchFormat, includesOrdinal, storageService) :
                        batchFormat;
                }
            }
            else
            {
                batchFormat = FileName;
            }

            return isValid && batchFormat.Length <= MAX_PATH_LENGTH ? batchFormat : string.Empty;
        }

        private (Folder? folder, string batchFormat) ResolveTargetFolder(string batchFormat)
        {
            Folder? folder = Folder;

            if (batchFormat.StartsWith(@"..\"))
            {
                // If the batch format starts with "..",
                // navigate to parent folder.
                while (batchFormat.StartsWith(@"..\") && folder != null)
                {
                    folder = folder.Parent;
                    batchFormat = batchFormat[3..];
                }
            }

            return (folder, batchFormat);
        }

        private (string batchFormat, bool includesOrdinal) ReplaceSupportedTagsWithValues(string batchFormat, int ordinal, IFormatProvider provider)
        {
            (batchFormat, bool includesOrdinal) = ReplaceOrdinalTagWithValue(batchFormat, ordinal);
            batchFormat = batchFormat.Replace("<PixelWidth>", PixelWidth.ToString(), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<PixelHeight>", PixelHeight.ToString(), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<CreationDate>", FileCreationDateTime.ToString("yyyyMMdd", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<CreationDate:yy>", FileCreationDateTime.ToString("yy", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<CreationDate:yyyy>", FileCreationDateTime.ToString("yyyy", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<CreationDate:MM>", FileCreationDateTime.ToString("MM", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<CreationDate:MMMM>", FileCreationDateTime.ToString("MMMM", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<CreationDate:dd>", FileCreationDateTime.ToString("dd", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<CreationTime:HH>", FileCreationDateTime.ToString("HH", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<CreationTime:mm>", FileCreationDateTime.ToString("mm", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<CreationTime:ss>", FileCreationDateTime.ToString("ss", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<CreationTime>", FileCreationDateTime.ToString("HHmmss", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<ModificationDate>", FileModificationDateTime.ToString("yyyyMMdd", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<ModificationDate:yy>", FileModificationDateTime.ToString("yy", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<ModificationDate:yyyy>", FileModificationDateTime.ToString("yyyy", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<ModificationDate:MM>", FileModificationDateTime.ToString("MM", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<ModificationDate:MMMM>", FileModificationDateTime.ToString("MMMM", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<ModificationDate:dd>", FileModificationDateTime.ToString("dd", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<ModificationTime:HH>", FileModificationDateTime.ToString("HH", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<ModificationTime:mm>", FileModificationDateTime.ToString("mm", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<ModificationTime:ss>", FileModificationDateTime.ToString("ss", provider), StringComparison.OrdinalIgnoreCase);
            batchFormat = batchFormat.Replace("<ModificationTime>", FileModificationDateTime.ToString("HHmmss", provider), StringComparison.OrdinalIgnoreCase);

            return (batchFormat, includesOrdinal);
        }

        private static (string batchFormat, bool includesOrdinal) ReplaceOrdinalTagWithValue(string batchFormat, int ordinal)
        {
            bool includesOrdinal = false;
            int ordinalStart = batchFormat.IndexOf("<#");
            int ordinalEnd = batchFormat.LastIndexOf("#>");

            if (ordinalStart >= 0)
            {
                string ordinalPlaceholder = batchFormat.Substring(ordinalStart + 1, ordinalEnd - ordinalStart);
                string ordinalFormat = new('0', ordinalPlaceholder.Length);
                string ordinalString = ordinal.ToString(ordinalFormat);
                batchFormat = batchFormat.Replace("<" + ordinalPlaceholder + ">", ordinalString);
                includesOrdinal = true;
            }

            return (batchFormat, includesOrdinal);
        }

        private static string ComputeUniqueTargetPath(Folder? folder,
            string targetFileName,
            bool targetFileNameIncludesOrdinal,
            IStorageService storageService)
        {
            if (folder != null && storageService.FileExists(targetFileName))
            {
                string[] fileNames = storageService.GetFileNames(folder.Path);
                
                while (fileNames.Any(f => string.Compare(targetFileName, f, StringComparison.OrdinalIgnoreCase) == 0))
                {
                    string[] fileNameParts = targetFileName.Split('.');

                    if (targetFileNameIncludesOrdinal)
                    {
                        Regex regex = new("(_[0-9]*)$", RegexOptions.IgnoreCase);
                        var matches = regex.Matches(fileNameParts[0]);

                        if (matches.Count > 0)
                        {
                            int count = int.Parse(matches[0].Value[1..]) + 1;
                            string format = new('0', matches[0].Value.Length - 1);
                            targetFileName = $"{fileNameParts[0][..^matches[0].Value.Length]}_{count.ToString(format)}.{fileNameParts[1]}";
                        }
                    }
                    else
                    {
                        targetFileName = $"{fileNameParts[0]}_1.{fileNameParts[1]}";
                    }
                }
            }

            return targetFileName;
        }

        public override bool Equals(object? obj)
        {
            return obj is Asset asset && asset.FolderId == FolderId && asset.FileName == FileName;
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
