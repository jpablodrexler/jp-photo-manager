using JPPhotoManager.Domain;
using log4net;
using SimplePortableDatabase;
using System;
using System.Collections.Generic;
using System.Data;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Threading;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Infrastructure
{
    public class AssetRepository : IAssetRepository
    {
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        private const int STORAGE_VERSION = 3;
        private const string SEPARATOR = "|";

        public bool IsInitialized { get; private set; }
        private string dataDirectory;
        private readonly IDatabase database;
        private readonly IStorageService storageService;
        protected AssetCatalog AssetCatalog { get; private set; }
        protected Dictionary<string, Dictionary<string, byte[]>> Thumbnails { get; private set; }

        public AssetRepository(IDatabase database, IStorageService storageService)
        {
            this.database = database;
            this.storageService = storageService;
            this.Thumbnails = new Dictionary<string, Dictionary<string, byte[]>>();
            this.Initialize();
        }

        private void Initialize()
        {
            if (!this.IsInitialized)
            {
                this.InitializeDatabase();
                this.ReadCatalog();

                if (this.AssetCatalog == null)
                {
                    this.AssetCatalog = new AssetCatalog();
                    this.AssetCatalog.StorageVersion = STORAGE_VERSION;
                    SaveCatalog(null);
                }

                this.IsInitialized = true;
            }
        }

        private void InitializeDatabase()
        {
            this.dataDirectory = this.storageService.ResolveDataDirectory(STORAGE_VERSION);
            var separatorChar = SEPARATOR.ToCharArray().First();
            this.database.Initialize(this.dataDirectory, separatorChar);

            this.database.SetDataTableProperties(new DataTableProperties
            {
                TableName = "Folder",
                ColumnProperties = new ColumnProperties[]
                {
                    new ColumnProperties { ColumnName = "FolderId" },
                    new ColumnProperties { ColumnName = "Path" }
                }
            });

            this.database.SetDataTableProperties(new DataTableProperties
            {
                TableName = "Asset",
                ColumnProperties = new ColumnProperties[]
                {
                    new ColumnProperties { ColumnName = "FolderId" },
                    new ColumnProperties { ColumnName = "FileName" },
                    new ColumnProperties { ColumnName = "FileSize" },
                    new ColumnProperties { ColumnName = "ImageRotation" },
                    new ColumnProperties { ColumnName = "PixelWidth" },
                    new ColumnProperties { ColumnName = "PixelHeight" },
                    new ColumnProperties { ColumnName = "ThumbnailPixelWidth" },
                    new ColumnProperties { ColumnName = "ThumbnailPixelHeight" },
                    new ColumnProperties { ColumnName = "ThumbnailCreationDateTime" },
                    new ColumnProperties { ColumnName = "Hash" }
                }
            });

            this.database.SetDataTableProperties(new DataTableProperties
            {
                TableName = "Import",
                ColumnProperties = new ColumnProperties[]
                {
                    new ColumnProperties { ColumnName = "SourceDirectory" },
                    new ColumnProperties { ColumnName = "DestinationDirectory" },
                    new ColumnProperties { ColumnName = "IncludeSubFolders" }
                }
            });

            this.database.SetDataTableProperties(new DataTableProperties
            {
                TableName = "RecentTargetPaths",
                ColumnProperties = new ColumnProperties[]
                {
                    new ColumnProperties { ColumnName = "Path" }
                }
            });
        }

        private void ReadCatalog()
        {
            this.AssetCatalog = new AssetCatalog();
            this.AssetCatalog.Assets.Clear();
            this.AssetCatalog.Assets.AddRange(ReadAssets());
            this.AssetCatalog.Folders.Clear();
            this.AssetCatalog.Folders.AddRange(ReadFolders());
            this.AssetCatalog.ImportNewAssetsConfiguration.Imports.Clear();
            this.AssetCatalog.ImportNewAssetsConfiguration.Imports.AddRange(ReadImportDefinitions());
            this.AssetCatalog.Assets.ForEach(a => a.Folder = GetFolderById(a.FolderId));
            this.AssetCatalog.RecentTargetPaths.AddRange(ReadRecentTargetPaths());
        }

        public void SaveCatalog(Folder folder)
        {
            lock (this.AssetCatalog)
            {
                if (this.AssetCatalog.HasChanges)
                {
                    WriteAssets(this.AssetCatalog.Assets);
                    WriteFolders(this.AssetCatalog.Folders);
                    WriteImports(this.AssetCatalog.ImportNewAssetsConfiguration.Imports);
                    WriteRecentTargetPaths(this.AssetCatalog.RecentTargetPaths);
                }
                
                this.AssetCatalog.HasChanges = false;

                if (Thumbnails != null && folder != null && Thumbnails.ContainsKey(folder.Path))
                {
                    this.SaveThumbnails(Thumbnails[folder.Path], folder.ThumbnailsFilename);
                }
            }
        }

        public List<Folder> ReadFolders()
        {
            List<Folder> result;

            try
            {
                result = database.ReadObjectList("Folder", f =>
                    new Folder
                    {
                        FolderId = f[0],
                        Path = f[1]
                    });
            }
            catch (ArgumentException ex)
            {
                throw new ApplicationException($"Error while trying to read data table 'Folder'. " +
                    $"DataDirectory: {database.DataDirectory} - " +
                    $"Separator: {database.Separator} - " +
                    $"LastReadFilePath: {database.Diagnostics.LastReadFilePath} - " +
                    $"LastReadFileRaw: {database.Diagnostics.LastReadFileRaw}",
                    ex);
            }

            return result;
        }

        public List<Asset> ReadAssets()
        {
            List<Asset> result;
            
            try
            {
                result = database.ReadObjectList("Asset", f =>
                    new Asset
                    {
                        FolderId = f[0],
                        FileName = f[1],
                        FileSize = long.Parse(f[2]),
                        ImageRotation = (Rotation)Enum.Parse(typeof(Rotation), f[3]),
                        PixelWidth = int.Parse(f[4]),
                        PixelHeight = int.Parse(f[5]),
                        ThumbnailPixelWidth = int.Parse(f[6]),
                        ThumbnailPixelHeight = int.Parse(f[7]),
                        ThumbnailCreationDateTime = DateTime.Parse(f[8]),
                        Hash = f[9]
                    });
            }
            catch (ArgumentException ex)
            {
                throw new ApplicationException($"Error while trying to read data table 'Asset'. " +
                    $"DataDirectory: {database.DataDirectory} - " +
                    $"Separator: {database.Separator} - " +
                    $"LastReadFilePath: {database.Diagnostics.LastReadFilePath} - " +
                    $"LastReadFileRaw: {database.Diagnostics.LastReadFileRaw}",
                    ex);
            }
            
            return result;
        }

        public List<ImportNewAssetsDirectoriesDefinition> ReadImportDefinitions()
        {
            List<ImportNewAssetsDirectoriesDefinition> result;

            try
            {
                result = database.ReadObjectList("Import", f =>
                    new ImportNewAssetsDirectoriesDefinition
                    {
                        SourceDirectory = f[0],
                        DestinationDirectory = f[1],
                        IncludeSubFolders = bool.Parse(f[2])
                    });
            }
            catch (ArgumentException ex)
            {
                throw new ApplicationException($"Error while trying to read data table 'Import'. " +
                    $"DataDirectory: {database.DataDirectory} - " +
                    $"Separator: {database.Separator} - " +
                    $"LastReadFilePath: {database.Diagnostics.LastReadFilePath} - " +
                    $"LastReadFileRaw: {database.Diagnostics.LastReadFileRaw}",
                    ex);
            }
            
            return result;
        }

        public List<string> ReadRecentTargetPaths()
        {
            List<string> result;

            try
            {
                result = database.ReadObjectList("RecentTargetPaths", f => f[0]);
            }
            catch (ArgumentException ex)
            {
                throw new ApplicationException($"Error while trying to read data table 'RecentTargetPaths'. " +
                    $"DataDirectory: {database.DataDirectory} - " +
                    $"Separator: {database.Separator} - " +
                    $"LastReadFilePath: {database.Diagnostics.LastReadFilePath} - " +
                    $"LastReadFileRaw: {database.Diagnostics.LastReadFileRaw}",
                    ex);
            }

            return result;
        }

        public void WriteFolders(List<Folder> folders)
        {
            this.database.WriteObjectList(folders, "Folder", (f, i) =>
            {
                return i switch
                {
                    0 => f.FolderId,
                    1 => f.Path,
                    _ => throw new ArgumentOutOfRangeException(nameof(i))
                };
            });
        }

        public void WriteAssets(List<Asset> assets)
        {
            this.database.WriteObjectList(assets, "Asset", (a, i) =>
            {
                return i switch
                {
                    0 => a.FolderId,
                    1 => a.FileName,
                    2 => a.FileSize,
                    3 => a.ImageRotation,
                    4 => a.PixelWidth,
                    5 => a.PixelHeight,
                    6 => a.ThumbnailPixelWidth,
                    7 => a.ThumbnailPixelHeight,
                    8 => a.ThumbnailCreationDateTime,
                    9 => a.Hash,
                    _ => throw new ArgumentOutOfRangeException(nameof(i))
                };
            });
        }

        public void WriteImports(List<ImportNewAssetsDirectoriesDefinition> imports)
        {
            this.database.WriteObjectList(imports, "Import", (d, i) =>
            {
                return i switch
                {
                    0 => d.SourceDirectory,
                    1 => d.DestinationDirectory,
                    2 => d.IncludeSubFolders,
                    _ => throw new ArgumentOutOfRangeException(nameof(i))
                };
            });
        }

        public void WriteRecentTargetPaths(List<string> recentTargetPaths)
        {
            this.database.WriteObjectList(recentTargetPaths, "RecentTargetPaths", (p, i) =>
            {
                return i switch
                {
                    0 => p,
                    _ => throw new ArgumentOutOfRangeException(nameof(i))
                };
            });
        }

        public bool FolderHasThumbnails(Folder folder)
        {
            string thumbnailsFilePath = this.database.ResolveBlobFilePath(dataDirectory, folder.ThumbnailsFilename);
            // TODO: Implement through the NuGet package.
            return File.Exists(thumbnailsFilePath);
        }

        private void DeleteThumbnails(Folder folder)
        {
            // TODO: Implement through the NuGet package.
            string thumbnailsFilePath = this.database.ResolveBlobFilePath(dataDirectory, folder.ThumbnailsFilename);
            File.Delete(thumbnailsFilePath);
        }

        protected virtual Dictionary<string, byte[]> GetThumbnails(string thumbnailsFileName, out bool isNewFile)
        {
            isNewFile = false;
            Dictionary<string, byte[]> thumbnails = (Dictionary<string, byte[]>)this.database.ReadBlob(thumbnailsFileName);
            
            if (thumbnails == null)
            {
                thumbnails = new Dictionary<string, byte[]>();
                isNewFile = true;
            }

            return thumbnails;
        }

        public bool HasChanges()
        {
            bool result = false;

            lock (this.AssetCatalog)
            {
                result = this.AssetCatalog.HasChanges;
            }

            return result;
        }

        private void SaveThumbnails(Dictionary<string, byte[]> thumbnails, string thumbnailsFileName)
        {
            this.database.WriteBlob(thumbnails, thumbnailsFileName);
        }

        public Asset[] GetAssets(string directory)
        {
            List<Asset> assetsList = null;

            try
            {
                lock (this.AssetCatalog)
                {
                    Folder folder = GetFolderByPath(directory);

                    if (folder != null)
                    {
                        assetsList = GetAssetsByFolderId(folder.FolderId);
                        var thumbnails = GetThumbnails(folder.ThumbnailsFilename, out bool isNewFile);

                        if (!isNewFile)
                        {
                            foreach (Asset asset in assetsList)
                            {
                                if (thumbnails.ContainsKey(asset.FileName))
                                {
                                    asset.ImageData = this.storageService.LoadBitmapImage(thumbnails[asset.FileName], asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight);
                                }
                            }

                            // Removes assets with no thumbnails.
                            List<Asset> assetsToRemove = new List<Asset>();

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
                            this.storageService.GetFileInformation(asset);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }

            return assetsList.ToArray();
        }

        public bool FolderExists(string path)
        {
            bool result = false;

            lock (this.AssetCatalog)
            {
                result = this.AssetCatalog.Folders.Any(f => f.Path == path);
            }

            return result;
        }

        public Folder AddFolder(string path)
        {
            Folder folder;

            lock (this.AssetCatalog)
            {
                string folderId = Guid.NewGuid().ToString();

                folder = new Folder
                {
                    FolderId = folderId,
                    Path = path
                };

                this.AssetCatalog.Folders.Add(folder);
                this.AssetCatalog.HasChanges = true;
            }

            return folder;
        }

        public void AddAsset(Asset asset, byte[] thumbnailData)
        {
            lock (this.AssetCatalog)
            {
                Folder folder = GetFolderById(asset.FolderId);

                if (folder == null)
                {
                    this.AddFolder(asset.Folder.Path);
                }

                if (!this.Thumbnails.ContainsKey(asset.Folder.Path))
                {
                    this.Thumbnails[asset.Folder.Path] = this.GetThumbnails(asset.Folder.ThumbnailsFilename, out bool isNewFile);
                }

                this.Thumbnails[asset.Folder.Path][asset.FileName] = thumbnailData;
                this.AssetCatalog.Assets.Add(asset);
                this.AssetCatalog.HasChanges = true;
            }
        }

        public Folder[] GetFolders()
        {
            Folder[] result;

            lock (this.AssetCatalog)
            {
                result = this.AssetCatalog.Folders.ToArray();
            }

            return result;
        }

        public Folder[] GetSubFolders(Folder parentFolder, bool includeHidden)
        {
            Folder[] folders = GetFolders();
            folders = folders.Where(f => parentFolder.IsParentOf(f, null)).ToArray();
            return folders;
        }

        public Folder GetFolderByPath(string path)
        {
            Folder result = null;

            lock (this.AssetCatalog)
            {
                result = this.AssetCatalog.Folders.FirstOrDefault(f => f.Path == path);
            }

            return result;
        }

        public Folder[] GetFoldersByPaths(string[] paths)
        {
            Folder[] folders = new Folder[paths.Length];

            for (int i = 0; i < paths.Length; i++)
            {
                folders[i] = this.AddFolder(paths[i]);
            }

            return folders;
        }

        private Folder GetFolderById(string folderId)
        {
            Folder result = null;

            lock (this.AssetCatalog)
            {
                result = this.AssetCatalog.Folders.FirstOrDefault(f => f.FolderId == folderId);
            }

            return result;
        }

        private List<Asset> GetAssetsByFolderId(string folderId)
        {
            List<Asset> result = null;

            lock (this.AssetCatalog)
            {
                result = this.AssetCatalog.Assets.Where(a => a.FolderId == folderId).ToList();
            }

            return result;
        }

        private Asset GetAssetByFolderIdFileName(string folderId, string fileName)
        {
            Asset result = null;

            lock (this.AssetCatalog)
            {
                result = this.AssetCatalog.Assets.FirstOrDefault(a => a.FolderId == folderId && a.FileName == fileName);
            }

            return result;
        }

        public void DeleteAsset(string directory, string fileName)
        {
            lock (this.AssetCatalog)
            {
                Folder folder = GetFolderByPath(directory);

                if (folder != null)
                {
                    Asset deletedAsset = GetAssetByFolderIdFileName(folder.FolderId, fileName);

                    if (!this.Thumbnails.ContainsKey(folder.Path))
                    {
                        this.Thumbnails[folder.Path] = GetThumbnails(folder.ThumbnailsFilename, out bool isNewFile);
                    }

                    if (this.Thumbnails.ContainsKey(folder.Path))
                    {
                        this.Thumbnails[folder.Path].Remove(fileName);
                    }
                    
                    if (deletedAsset != null)
                    {
                        this.AssetCatalog.Assets.Remove(deletedAsset);
                        this.AssetCatalog.HasChanges = true;
                    }
                }
            }
        }

        public void DeleteFolder(Folder folder)
        {
            lock (this.AssetCatalog)
            {
                if (folder != null)
                {
                    if (this.Thumbnails.ContainsKey(folder.Path))
                    {
                        this.Thumbnails.Remove(folder.Path);
                    }

                    if (this.FolderHasThumbnails(folder))
                    {
                        this.DeleteThumbnails(folder);
                    }

                    this.AssetCatalog.Folders.Remove(folder);
                    this.AssetCatalog.HasChanges = true;
                }
            }
        }

        public List<Asset> GetCataloguedAssets()
        {
            List<Asset> cataloguedAssets = null;

            lock (this.AssetCatalog)
            {
                cataloguedAssets = this.AssetCatalog.Assets;
            }

            return cataloguedAssets;
        }

        public List<Asset> GetCataloguedAssets(string directory)
        {
            List<Asset> cataloguedAssets = null;

            lock (this.AssetCatalog)
            {
                Folder folder = GetFolderByPath(directory);

                if (folder != null)
                {
                    cataloguedAssets = this.AssetCatalog.Assets.Where(a => a.FolderId == folder.FolderId).ToList();
                }
            }

            return cataloguedAssets;
        }

        public bool IsAssetCatalogued(string directoryName, string fileName)
        {
            bool result = false;

            lock (this.AssetCatalog)
            {
                Folder folder = GetFolderByPath(directoryName);
                result = folder != null && GetAssetByFolderIdFileName(folder.FolderId, fileName) != null;
            }

            return result;
        }

        public bool ContainsThumbnail(string directoryName, string fileName)
        {
            bool result = false;

            lock (this.AssetCatalog)
            {
                if (!this.Thumbnails.ContainsKey(directoryName))
                {
                    Folder folder = GetFolderByPath(directoryName);
                    this.Thumbnails[directoryName] = GetThumbnails(folder.ThumbnailsFilename, out bool isNewFile);
                }

                result = this.Thumbnails[directoryName].ContainsKey(fileName);
            }

            return result;
        }

        public BitmapImage LoadThumbnail(string directoryName, string fileName, int width, int height)
        {
            BitmapImage result = null;

            lock (this.AssetCatalog)
            {
                if (!this.Thumbnails.ContainsKey(directoryName))
                {
                    Folder folder = GetFolderByPath(directoryName);
                    this.Thumbnails[directoryName] = GetThumbnails(folder.ThumbnailsFilename, out bool isNewFile);
                }

                if (Thumbnails[directoryName].ContainsKey(fileName))
                {
                    result = this.storageService.LoadBitmapImage(Thumbnails[directoryName][fileName], width, height);
                }
                else
                {
                    this.DeleteAsset(directoryName, fileName);
                    Folder folder = GetFolderByPath(directoryName);
                    this.SaveCatalog(folder);
                }
            }

            return result;
        }

        public ImportNewAssetsConfiguration GetImportNewAssetsConfiguration()
        {
            ImportNewAssetsConfiguration result;

            lock (this.AssetCatalog)
            {
                result = this.AssetCatalog.ImportNewAssetsConfiguration;
            }

            return result;
        }

        public void SetImportNewAssetsConfiguration(ImportNewAssetsConfiguration importConfiguration)
        {
            lock (this.AssetCatalog)
            {
                this.AssetCatalog.ImportNewAssetsConfiguration = importConfiguration;
                this.AssetCatalog.HasChanges = true;
            }
        }

        public List<string> GetRecentTargetPaths()
        {
            List<string> result = null;

            lock (this.AssetCatalog)
            {
                result = this.AssetCatalog.RecentTargetPaths;
            }

            return result;
        }

        public void SetRecentTargetPaths(List<string> recentTargetPaths)
        {
            lock (this.AssetCatalog)
            {
                this.AssetCatalog.RecentTargetPaths = recentTargetPaths;
                this.AssetCatalog.HasChanges = true;
            }
        }
    }
}
