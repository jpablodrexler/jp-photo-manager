using System.Text.RegularExpressions;

namespace JPPhotoManager.Domain
{
    public class ImportNewAssetsDirectoriesDefinition
    {
        private const string LOCAL_PATH_PATTERN = "^([A-Za-z])(:)(\\[A-Za-z0-9]*)*";
        private const string REMOTE_PATH_PATTERN = "^(\\\\)(\\[A-Za-z0-9]*)*";

        public string SourceDirectory { get; set; }
        public string DestinationDirectory { get; set; }
        public bool IncludeSubFolders { get; set; }

        internal bool IsValid()
        {
            return (IsValidLocalPath(this.SourceDirectory) || IsValidRemotePath(this.SourceDirectory))
                && (IsValidLocalPath(this.DestinationDirectory) || IsValidRemotePath(this.DestinationDirectory));
        }

        private bool IsValidLocalPath(string directory)
        {
            Regex regex = new Regex(LOCAL_PATH_PATTERN);
            return regex.IsMatch(directory);
        }

        private bool IsValidRemotePath(string directory)
        {
            Regex regex = new Regex(REMOTE_PATH_PATTERN);
            return regex.IsMatch(directory);
        }

        internal void Normalize()
        {
            bool isRemote = IsValidRemotePath(this.SourceDirectory);
            string[] parts = this.SourceDirectory.Split('\\', StringSplitOptions.RemoveEmptyEntries);
            this.SourceDirectory = string.Join('\\', parts);
            this.SourceDirectory = isRemote ? @"\\" + this.SourceDirectory : this.SourceDirectory;

            isRemote = IsValidRemotePath(this.DestinationDirectory);
            parts = this.DestinationDirectory.Split('\\', StringSplitOptions.RemoveEmptyEntries);
            this.DestinationDirectory = string.Join('\\', parts);
            this.DestinationDirectory = isRemote ? @"\\" + this.DestinationDirectory : this.DestinationDirectory;
        }
    }
}
