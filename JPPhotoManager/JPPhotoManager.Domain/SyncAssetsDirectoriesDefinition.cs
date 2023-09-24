using System.ComponentModel.DataAnnotations;
using System.Text.RegularExpressions;

namespace JPPhotoManager.Domain
{
    public class SyncAssetsDirectoriesDefinition
    {
        private const string LOCAL_PATH_PATTERN = "^([A-Za-z])(:)(\\[A-Za-z0-9]*)*";
        private const string REMOTE_PATH_PATTERN = "^(\\\\)(\\[A-Za-z0-9]*)*";

        [Key]
        public int Id { get; set; }

        public string SourceDirectory { get; set; }
        public string DestinationDirectory { get; set; }
        public bool IncludeSubFolders { get; set; }
        public bool DeleteAssetsNotInSource { get; set; }

        internal bool IsValid()
        {
            return (IsValidLocalPath(SourceDirectory) || IsValidRemotePath(SourceDirectory))
                && (IsValidLocalPath(DestinationDirectory) || IsValidRemotePath(DestinationDirectory));
        }

        private bool IsValidLocalPath(string directory)
        {
            Regex regex = new(LOCAL_PATH_PATTERN);
            return regex.IsMatch(directory);
        }

        private bool IsValidRemotePath(string directory)
        {
            Regex regex = new(REMOTE_PATH_PATTERN);
            return regex.IsMatch(directory);
        }

        internal void Normalize()
        {
            bool isRemote = IsValidRemotePath(SourceDirectory);
            string[] parts = SourceDirectory.Split('\\', StringSplitOptions.RemoveEmptyEntries);
            SourceDirectory = string.Join('\\', parts);
            SourceDirectory = isRemote ? @"\\" + SourceDirectory : SourceDirectory;

            isRemote = IsValidRemotePath(DestinationDirectory);
            parts = DestinationDirectory.Split('\\', StringSplitOptions.RemoveEmptyEntries);
            DestinationDirectory = string.Join('\\', parts);
            DestinationDirectory = isRemote ? @"\\" + DestinationDirectory : DestinationDirectory;
        }
    }
}
