using Newtonsoft.Json;
using System;

namespace JPPhotoManager.Domain
{
    public class Folder
    {
        public string FolderId { get; set; }
        public string Path { get; set; }

        [JsonIgnore]
        public string ThumbnailsFilename => FolderId + ".bin";

        public string Name
        {
            get
            {
                string[] pathParts = this.Path.Split(new char[] { '\\' }, StringSplitOptions.RemoveEmptyEntries);
                string result = pathParts[pathParts.Length - 1];

                return result;
            }
        }

        public override bool Equals(object obj)
        {
            Folder folder = obj as Folder;

            return folder != null && folder.Path == this.Path;
        }

        public override int GetHashCode()
        {
            return this.Path != null ? this.Path.GetHashCode() : base.GetHashCode();
        }
    }
}
