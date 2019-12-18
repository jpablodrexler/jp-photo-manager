using System;
using System.Text.RegularExpressions;

namespace JPPhotoManager.Domain
{
    public class ImportNewAssetsDirectoriesDefinition
    {
        private const string PATTERN = "^([A-Za-z])(:)(\\[A-Za-z0-9]*)*";

        public string SourceDirectory { get; set; }
        public string DestinationDirectory { get; set; }

        internal bool IsValid()
        {
            Regex regex = new Regex(PATTERN);
            return regex.IsMatch(this.SourceDirectory) && regex.IsMatch(this.DestinationDirectory);
        }

        internal void Normalize()
        {
            string[] parts = this.SourceDirectory.Split('\\', StringSplitOptions.RemoveEmptyEntries);
            this.SourceDirectory = string.Join('\\', parts);

            parts = this.DestinationDirectory.Split('\\', StringSplitOptions.RemoveEmptyEntries);
            this.DestinationDirectory = string.Join('\\', parts);
        }
    }
}
