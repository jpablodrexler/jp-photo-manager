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

        public bool IsInitialized { get; private set; }
        private string dataDirectory;
        private readonly IDatabase database;
        private readonly IStorageService storageService;
        private readonly IUserConfigurationService userConfigurationService;

        protected AssetCatalog AssetCatalog { get; private set; }
        protected Dictionary<string, Dictionary<string, byte[]>> Thumbnails { get; private set; }

        public AssetRepository(IDatabase database, IStorageService storageService, IUserConfigurationService userConfigurationService)
        {
            this.database = database;
            this.storageService = storageService;
            this.userConfigurationService = userConfigurationService;
            this.Thumbnails = new Dictionary<string, Dictionary<string, byte[]>>();
            this.Initialize();
        }

        private void Initialize()
        {
            if (!this.IsInitialized)
            {
                this.dataDirectory = this.storageService.ResolveDataDirectory();
                var separator = Thread.CurrentThread.CurrentUICulture.TextInfo.ListSeparator;
                var separatorChar = separator.ToCharArray().First();
                this.database.Initialize(this.dataDirectory, separatorChar);
                this.ReadCatalog();

                if (this.AssetCatalog == null)
                {
                    this.AssetCatalog = new AssetCatalog();
                    this.AssetCatalog.StorageVersion = 2.0;
                    SaveCatalog(null);
                }

                this.IsInitialized = true;
            }
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
            List<Folder> result = new List<Folder>();

            try
            {
                DataTable dataTable = database.ReadDataTable("Folder");

                if (dataTable != null)
                {
                    for (int i = 0; i < dataTable.Rows.Count; i++)
                    {
                        DataRow row = dataTable.Rows[i];

                        Folder folder = new Folder
                        {
                            FolderId = row["FolderId"].ToString(),
                            Path = row["Path"].ToString()
                        };

                        result.Add(folder);
                    }
                }
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
            List<Asset> result = new List<Asset>();
            
            try
            {
                DataTable dataTable = database.ReadDataTable("Asset");

                if (dataTable != null)
                {
                    for (int i = 0; i < dataTable.Rows.Count; i++)
                    {
                        DataRow row = dataTable.Rows[i];

                        Asset asset = new Asset
                        {
                            FolderId = row["FolderId"].ToString(),
                            FileName = row["FileName"].ToString(),
                            FileSize = long.Parse(row["FileSize"].ToString()),
                            ImageRotation = (Rotation)Enum.Parse(typeof(Rotation), row["ImageRotation"].ToString()),
                            PixelWidth = int.Parse(row["PixelWidth"].ToString()),
                            PixelHeight = int.Parse(row["PixelHeight"].ToString()),
                            ThumbnailPixelWidth = int.Parse(row["ThumbnailPixelWidth"].ToString()),
                            ThumbnailPixelHeight = int.Parse(row["ThumbnailPixelHeight"].ToString()),
                            ThumbnailCreationDateTime = DateTime.Parse(row["ThumbnailCreationDateTime"].ToString()),
                            Hash = row["Hash"].ToString()
                        };

                        result.Add(asset);
                    }
                }
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
            List<ImportNewAssetsDirectoriesDefinition> result = new List<ImportNewAssetsDirectoriesDefinition>();

            try
            {
                DataTable dataTable = database.ReadDataTable("Import");

                if (dataTable != null)
                {
                    for (int i = 0; i < dataTable.Rows.Count; i++)
                    {
                        DataRow row = dataTable.Rows[i];

                        ImportNewAssetsDirectoriesDefinition import = new ImportNewAssetsDirectoriesDefinition
                        {
                            SourceDirectory = row["SourceDirectory"].ToString(),
                            DestinationDirectory = row["DestinationDirectory"].ToString(),
                            IncludeSubFolders = bool.Parse(row["IncludeSubFolders"].ToString())
                        };

                        result.Add(import);
                    }
                }
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
            List<string> result = new List<string>();

            try
            {
                DataTable dataTable = database.ReadDataTable("RecentTargetPaths");

                if (dataTable != null)
                {
                    for (int i = 0; i < dataTable.Rows.Count; i++)
                    {
                        DataRow row = dataTable.Rows[i];
                        result.Add(row["Path"].ToString());
                    }
                }
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
            DataTable table = new DataTable("Folder");
            table.Columns.Add("FolderId");
            table.Columns.Add("Path");
            
            foreach (Folder folder in folders)
            {
                DataRow row = table.NewRow();
                row["FolderId"] = folder.FolderId;
                row["Path"] = folder.Path;
                table.Rows.Add(row);
            }

            this.database.WriteDataTable(table);
        }

        public void WriteAssets(List<Asset> assets)
        {
            DataTable table = new DataTable("Asset");
            table.Columns.Add("FolderId");
            table.Columns.Add("FileName");
            table.Columns.Add("FileSize");
            table.Columns.Add("ImageRotation");
            table.Columns.Add("PixelWidth");
            table.Columns.Add("PixelHeight");
            table.Columns.Add("ThumbnailPixelWidth");
            table.Columns.Add("ThumbnailPixelHeight");
            table.Columns.Add("ThumbnailCreationDateTime");
            table.Columns.Add("Hash");

            foreach (Asset asset in assets)
            {
                DataRow row = table.NewRow();
                row["FolderId"] = asset.FolderId;
                row["FileName"] = asset.FileName;
                row["FileSize"] = asset.FileSize;
                row["ImageRotation"] = asset.ImageRotation;
                row["PixelWidth"] = asset.PixelWidth;
                row["PixelHeight"] = asset.PixelHeight;
                row["ThumbnailPixelWidth"] = asset.ThumbnailPixelWidth;
                row["ThumbnailPixelHeight"] = asset.ThumbnailPixelHeight;
                row["ThumbnailCreationDateTime"] = asset.ThumbnailCreationDateTime;
                row["Hash"] = asset.Hash;
                table.Rows.Add(row);
            }

            this.database.WriteDataTable(table);
        }

        public void WriteImports(List<ImportNewAssetsDirectoriesDefinition> imports)
        {
            DataTable table = new DataTable("Import");
            table.Columns.Add("SourceDirectory");
            table.Columns.Add("DestinationDirectory");
            table.Columns.Add("IncludeSubFolders");

            foreach (ImportNewAssetsDirectoriesDefinition import in imports)
            {
                DataRow row = table.NewRow();
                row["SourceDirectory"] = import.SourceDirectory;
                row["DestinationDirectory"] = import.DestinationDirectory;
                row["IncludeSubFolders"] = import.IncludeSubFolders;
                table.Rows.Add(row);
            }

            this.database.WriteDataTable(table);
        }

        public void WriteRecentTargetPaths(List<string> recentTargetPaths)
        {
            DataTable table = new DataTable("RecentTargetPaths");
            table.Columns.Add("Path");

            foreach (string path in recentTargetPaths)
            {
                DataRow row = table.NewRow();
                row["Path"] = path;
                table.Rows.Add(row);
            }

            this.database.WriteDataTable(table);
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

        public Folder GetFolderByPath(string path)
        {
            Folder result = null;

            lock (this.AssetCatalog)
            {
                result = this.AssetCatalog.Folders.FirstOrDefault(f => f.Path == path);
            }

            return result;
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
