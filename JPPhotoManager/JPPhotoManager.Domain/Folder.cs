using JPPhotoManager.Domain.Interfaces;

namespace JPPhotoManager.Domain
{
    public class Folder
    {
        public string FolderId { get; set; }
        public string Path { get; set; }
        public string ThumbnailsFilename => FolderId + ".bin";

        public string Name
        {
            get
            {
                string[] pathParts = Path.Split(new char[] { '\\' }, StringSplitOptions.RemoveEmptyEntries);
                string result = pathParts[pathParts.Length - 1];

                return result;
            }
        }

        public bool IsParentOf(Folder otherFolder)
        {
            bool result;
            string[] thisPathDirectories = Path.Split(System.IO.Path.DirectorySeparatorChar);
            string[] otherPathDirectories = otherFolder.Path.Split(System.IO.Path.DirectorySeparatorChar);

            result = (thisPathDirectories != null
                && otherPathDirectories != null
                && thisPathDirectories.Length == (otherPathDirectories.Length - 1));

            if (result)
            {
                for (int i  = 0; i < thisPathDirectories.Length; i++)
                {
                    if (string.Compare(thisPathDirectories[i], otherPathDirectories[i], StringComparison.OrdinalIgnoreCase) != 0)
                    {
                        result = false;
                        break;
                    }
                }
            }

            return result;
        }

        public override bool Equals(object obj)
        {
            Folder folder = obj as Folder;

            return folder != null && folder.Path == Path;
        }

        public override int GetHashCode()
        {
            return Path != null ? Path.GetHashCode() : base.GetHashCode();
        }

        public override string ToString()
        {
            return Path;
        }
    }
}
