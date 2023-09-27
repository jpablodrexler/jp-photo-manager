using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;

namespace JPPhotoManager.Infrastructure
{
    public class FolderRepository : IFolderRepository
    {
        private readonly AppDbContext _appDbContext;
        private readonly SyncLock _syncLock;

        public FolderRepository(AppDbContext appDbContext, SyncLock syncLock)
        {
            _appDbContext = appDbContext;
            _syncLock = syncLock;
        }

        public Folder AddFolder(string path)
        {
            Folder folder;

            lock (_syncLock)
            {
                folder = new Folder
                {
                    Path = path
                };

                _appDbContext.Folders.Add(folder);
                _appDbContext.SaveChanges();
            }

            return folder;
        }

        public Folder[] GetFolders()
        {
            Folder[] result;

            lock (_syncLock)
            {
                result = _appDbContext.Folders.ToArray();
            }

            return result;
        }

        public Folder[] GetSubFolders(Folder parentFolder, bool includeHidden)
        {
            Folder[] folders = GetFolders();
            folders = folders.Where(f => parentFolder.IsParentOf(f)).ToArray();
            return folders;
        }

        public Folder GetFolderByPath(string path)
        {
            Folder result = null;

            lock (_syncLock)
            {
                result = _appDbContext.Folders.FirstOrDefault(f => f.Path == path);
            }

            return result;
        }

        private Folder GetFolderById(int folderId)
        {
            Folder result = null;

            lock (_syncLock)
            {
                result = _appDbContext.Folders.FirstOrDefault(f => f.FolderId == folderId);
            }

            return result;
        }

        public void DeleteFolder(Folder folder)
        {
            lock (_syncLock)
            {
                if (folder != null)
                {
                    _appDbContext.Folders.Remove(folder);
                    _appDbContext.SaveChanges();
                }
            }
        }

        public bool FolderExists(string path)
        {
            bool result = false;

            lock (_syncLock)
            {
                result = _appDbContext.Folders.Any(f => f.Path == path);
            }

            return result;
        }
    }
}
