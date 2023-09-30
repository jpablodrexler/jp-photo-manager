using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;
using log4net;
using Microsoft.EntityFrameworkCore;
using System.Data;
using System.IO;
using System.Reflection;
using System.Runtime.Serialization.Formatters.Binary;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Infrastructure
{
    public class AssetRepository : IAssetRepository
    {
        private static readonly ILog _log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        private const int PAGE_SIZE = 100;
        
        private readonly AppDbContext _appDbContext;
        private readonly IStorageService _storageService;
        private readonly IUserConfigurationService _userConfigurationService;

        protected Dictionary<string, byte[]> Thumbnails { get; private set; }
        
        private readonly SyncLock _syncLock;

        public AssetRepository(AppDbContext appDbContext, IStorageService storageService, IUserConfigurationService userConfigurationService, SyncLock syncLock)
        {
            _appDbContext = appDbContext;
            _storageService = storageService;
            _userConfigurationService = userConfigurationService;
            Thumbnails = new Dictionary<string, byte[]>();
            _syncLock = syncLock;
        }

        public List<Folder> ReadFolders()
        {
            List<Folder> result = null;

            lock (_syncLock)
            {
                result = _appDbContext.Folders.ToList();
            }

            return result;
        }

        public List<Asset> ReadAssets()
        {
            List<Asset> result = null;

            lock (_syncLock)
            {
                result = _appDbContext
                    .Assets
                    .Include(a => a.Folder)
                    .ToList();
            }

            return result;
        }

        public List<SyncAssetsDirectoriesDefinition> ReadSyncDefinitions()
        {
            List<SyncAssetsDirectoriesDefinition> result = null;


            lock (_syncLock)
            {
                result = _appDbContext.SyncAssetsDirectoriesDefinitions.ToList();
            }

            return result;
        }

        public List<string> ReadRecentTargetPaths()
        {
            List<string> result = null;

            lock ( _syncLock)
            {
                result = _appDbContext
                    .RecentTargetPaths
                    .Select(p => p.Path)
                    .ToList();
            }

            return result;
        }

        public void DeleteThumbnails(Folder folder)
        {
            var assets = GetAssetsByFolderId(folder.FolderId);

            foreach (var asset in assets)
            {
                DeleteThumbnail(asset);
            }
        }

        public void RemoveThumbnailCache(string thumbnailBlobName)
        {
            if (Thumbnails.ContainsKey(thumbnailBlobName))
            {
                Thumbnails.Remove(thumbnailBlobName);
            }
        }

        protected void DeleteThumbnail(Asset asset)
        {
            RemoveThumbnailCache(asset.ThumbnailBlobName);
            string thumbnailsFilePath = GetBinaryFilePath(asset.ThumbnailBlobName);
            File.Delete(thumbnailsFilePath);
        }

        public void DeleteThumbnail(string thumbnailBlobName)
        {
            RemoveThumbnailCache(thumbnailBlobName);
            string thumbnailsFilePath = GetBinaryFilePath(thumbnailBlobName);
            File.Delete(thumbnailsFilePath);
        }

        public string[] GetThumbnailsList()
        {
            string blobsDirectory = _userConfigurationService.GetBinaryFilesDirectory();
            return Directory.GetFiles(blobsDirectory);
        }

        public PaginatedData<Asset> GetAssets(Folder folder, int pageIndex)
        {
            PaginatedData<Asset> result;
            List<Asset> assetsList = null;
            bool isNewFile = false;
            int totalCount = 0;

            try
            {
                lock (_syncLock)
                {
                    if (folder != null)
                    {
                        assetsList = GetAssetsByFolderId(folder.FolderId);
                        totalCount = assetsList.Count;
                        assetsList = assetsList.Skip(pageIndex * PAGE_SIZE).Take(PAGE_SIZE).ToList();
                        
                        if (!isNewFile)
                        {
                            foreach (Asset asset in assetsList)
                            {
                                if (!Thumbnails.ContainsKey(asset.ThumbnailBlobName))
                                {
                                    var bytes = (byte[])ReadFromBinaryFile(asset.ThumbnailBlobName);

                                    if (bytes != null)
                                    {
                                        Thumbnails[asset.ThumbnailBlobName] = bytes;
                                    }
                                }
                                
                                asset.ImageData = Thumbnails.ContainsKey(asset.ThumbnailBlobName) ? _storageService.LoadBitmapImage(Thumbnails[asset.ThumbnailBlobName], asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight) : null;
                            }

                            // Removes assets with no thumbnails.
                            List<Asset> assetsToRemove = new();

                            for (int i = 0; i < assetsList.Count; i++)
                            {
                                if (assetsList[i].ImageData == null)
                                {
                                    assetsToRemove.Add(assetsList[i]);
                                }
                            }

                            foreach (Asset asset in assetsToRemove)
                            {
                                assetsList.Remove(asset);
                            }
                        }

                        foreach (Asset asset in assetsList)
                        {
                            _storageService.GetFileInformation(asset);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }

            return new PaginatedData<Asset> { Items = assetsList.ToArray(), PageIndex = pageIndex, TotalCount = totalCount };
        }

        public void AddAsset(Asset asset, Folder folder, byte[] thumbnailData)
        {
            lock (_syncLock)
            {
                _appDbContext.Assets.Add(asset);
                _appDbContext.SaveChanges();

                Thumbnails[asset.ThumbnailBlobName] = thumbnailData;

                if (thumbnailData != null)
                {
                    WriteToBinaryFile(thumbnailData, asset.ThumbnailBlobName);
                }
            }
        }

        public List<Asset> GetAssetsByFolderId(int folderId)
        {
            List<Asset> result = null;

            lock (_syncLock)
            {
                result = _appDbContext.Assets.Where(a => a.FolderId == folderId).ToList();
            }

            return result;
        }

        private Asset GetAssetByFolderIdFileName(int folderId, string fileName)
        {
            Asset result = null;

            lock (_syncLock)
            {
                result = _appDbContext.Assets.FirstOrDefault(a => a.FolderId == folderId && a.FileName == fileName);
            }

            return result;
        }

        public void DeleteAsset(Folder folder, string fileName)
        {
            lock (_syncLock)
            {
                if (folder != null)
                {
                    Asset deletedAsset = GetAssetByFolderIdFileName(folder.FolderId, fileName);

                    if (deletedAsset != null)
                    {
                        if (Thumbnails.ContainsKey(deletedAsset.ThumbnailBlobName))
                        {
                            DeleteThumbnail(deletedAsset);
                        }

                        _appDbContext.Assets.Remove(deletedAsset);
                        _appDbContext.SaveChanges();
                    }
                }
            }
        }

        public List<Asset> GetCataloguedAssets()
        {
            List<Asset> cataloguedAssets = null;

            lock (_syncLock)
            {
                cataloguedAssets = _appDbContext.Assets.ToList();
            }

            return cataloguedAssets;
        }

        public List<Asset> GetCataloguedAssets(Folder folder)
        {
            List<Asset> cataloguedAssets = null;

            lock (_syncLock)
            {
                if (folder != null)
                {
                    cataloguedAssets = _appDbContext.Assets.Where(a => a.FolderId == folder.FolderId).ToList();
                }
            }

            return cataloguedAssets;
        }

        public bool IsAssetCatalogued(Folder folder, string fileName)
        {
            bool result = false;

            lock (_syncLock)
            {
                result = folder != null && GetAssetByFolderIdFileName(folder.FolderId, fileName) != null;
            }

            return result;
        }

        public bool ContainsThumbnail(Folder folder, string fileName)
        {
            bool result = false;

            lock (_syncLock)
            {
                if (folder != null)
                {
                    var asset = GetAssetByFolderIdFileName(folder.FolderId, fileName);

                    if (asset != null)
                    {
                        var thumbnailBlobName = asset.ThumbnailBlobName;

                        if (!Thumbnails.ContainsKey(thumbnailBlobName))
                        {
                            var thumbnail = (byte[])ReadFromBinaryFile(thumbnailBlobName);

                            if (thumbnail != null)
                            {
                                Thumbnails[thumbnailBlobName] = thumbnail;
                            }
                        }

                        result = Thumbnails.ContainsKey(thumbnailBlobName);
                    }
                }
            }

            return result;
        }

        public BitmapImage LoadThumbnail(Folder folder, string fileName, int width, int height)
        {
            BitmapImage result = null;

            lock (_syncLock)
            {
                var asset = GetAssetByFolderIdFileName(folder.FolderId, fileName);
                var thumbnailBlobName = asset.ThumbnailBlobName;

                if (!Thumbnails.ContainsKey(thumbnailBlobName))
                {
                    var thumbnail = (byte[])ReadFromBinaryFile(thumbnailBlobName);

                    if (thumbnail != null)
                    {
                        Thumbnails[thumbnailBlobName] = thumbnail;
                    }
                }

                if (Thumbnails.ContainsKey(thumbnailBlobName))
                {
                    result = _storageService.LoadBitmapImage(Thumbnails[thumbnailBlobName], width, height);
                }
                else
                {
                    DeleteAsset(folder, fileName);
                }
            }

            return result;
        }

        // TODO: Extract to SyncAssetsConfigurationRepository.
        public SyncAssetsConfiguration GetSyncAssetsConfiguration()
        {
            SyncAssetsConfiguration result = new ();

            lock (_syncLock)
            {
                result.Definitions = _appDbContext
                    .SyncAssetsDirectoriesDefinitions
                    .OrderBy(d => d.Order)
                    .ToList();
            }
            
            return result;
        }

        // TODO: Extract to SyncAssetsConfigurationRepository.
        public void SaveSyncAssetsConfiguration(SyncAssetsConfiguration syncAssetsConfiguration)
        {
            lock (_syncLock)
            {
                var definitionsToDelete = _appDbContext.SyncAssetsDirectoriesDefinitions.Except(syncAssetsConfiguration.Definitions).ToList();
                _appDbContext.SyncAssetsDirectoriesDefinitions.RemoveRange(definitionsToDelete);

                foreach (var definition in syncAssetsConfiguration.Definitions)
                {
                    if (definition.Id > 0)
                    {
                        _appDbContext.SyncAssetsDirectoriesDefinitions.Update(definition);
                    }
                    else
                    {
                        _appDbContext.SyncAssetsDirectoriesDefinitions.Add(definition);
                    }
                }

                _appDbContext.SaveChanges();
            }
        }

        private string GetBinaryFilePath(string binaryFileName) => Path.Combine(_userConfigurationService.GetBinaryFilesDirectory(), binaryFileName);

        public object ReadFromBinaryFile(string binaryFileName)
        {
            object result = null;
            var binaryFilePath = GetBinaryFilePath(binaryFileName);
            
            if (File.Exists(binaryFilePath))
            {
                using (FileStream fileStream = new(binaryFilePath, FileMode.Open))
                {
                    BinaryFormatter binaryFormatter = new();
                    result = binaryFormatter.Deserialize(fileStream);
                }
            }

            return result;
        }

        public void WriteToBinaryFile(object anObject, string binaryFileName)
        {
            Directory.CreateDirectory(_userConfigurationService.GetAppFilesDirectory());
            Directory.CreateDirectory(_userConfigurationService.GetBinaryFilesDirectory());

            var binaryFilePath = GetBinaryFilePath(binaryFileName);

            using (FileStream fileStream = new(binaryFilePath, FileMode.Create))
            {
                BinaryFormatter binaryFormatter = new();
                binaryFormatter.Serialize(fileStream, anObject);
            }
        }
    }
}
