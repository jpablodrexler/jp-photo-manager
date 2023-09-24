namespace JPPhotoManager.Domain.Interfaces
{
    public interface IFolderRepository
    {
        Folder AddFolder(string path);
        bool FolderExists(string path);
        Folder[] GetFolders();
        Folder[] GetSubFolders(Folder parentFolder, bool includeHidden);
        Folder GetFolderByPath(string path);
        void DeleteFolder(Folder folder);
    }
}
