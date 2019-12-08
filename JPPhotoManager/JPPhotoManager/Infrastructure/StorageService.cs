using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.Serialization.Formatters.Binary;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Infrastructure
{
    public class StorageService : IStorageService
    {
        private const string ASSET_CATALOG_FILENAME = "AssetCatalog.json";
        private readonly IUserConfigurationService userConfigurationService;

        public StorageService(IUserConfigurationService userConfigurationService)
        {
            this.userConfigurationService = userConfigurationService;
        }

        public string GetParentDirectory(string directoryPath)
        {
            return new DirectoryInfo(directoryPath).Parent.FullName;
        }

        public string ResolveDataDirectory()
        {
            return userConfigurationService.GetApplicationDataFolder();
        }

        public string ResolveCatalogPath(string dataDirectory)
        {
            dataDirectory = !string.IsNullOrEmpty(dataDirectory) ? dataDirectory : string.Empty;
            return Path.Combine(dataDirectory, ASSET_CATALOG_FILENAME);
        }

        public string ResolveThumbnailsFilePath(string dataDirectory, string thumbnailsFileName)
        {
            return Path.Combine(dataDirectory, thumbnailsFileName);
        }

        public void CreateDirectory(string directory)
        {
            Directory.CreateDirectory(directory);
        }

        public T ReadObjectFromJson<T>(string jsonFilePath)
        {
            T result = default(T);
            string json;

            if (File.Exists(jsonFilePath))
            {
                using (StreamReader reader = new StreamReader(jsonFilePath))
                {
                    json = reader.ReadToEnd();
                }

                result = JsonSerializer.Deserialize<T>(json);
            }
            
            return result;
        }

        public void WriteObjectToJson(object anObject, string jsonFilePath)
        {
            string json = JsonSerializer.Serialize(anObject, new JsonSerializerOptions { WriteIndented = true });

            using (StreamWriter writer = new StreamWriter(jsonFilePath, false))
            {
                writer.Write(json);
            }
        }

        public object ReadObjectFromBinaryFile(string binaryFilePath)
        {
            object result = null;

            if (File.Exists(binaryFilePath))
            {
                using (FileStream fileStream = new FileStream(binaryFilePath, FileMode.Open))
                {
                    BinaryFormatter binaryFormatter = new BinaryFormatter();
                    result = binaryFormatter.Deserialize(fileStream);
                }
            }

            return result;
        }

        public void WriteObjectToBinaryFile(object anObject, string binaryFilePath)
        {
            using (FileStream fileStream = new FileStream(binaryFilePath, FileMode.Create))
            {
                BinaryFormatter binaryFormatter = new BinaryFormatter();
                binaryFormatter.Serialize(fileStream, anObject);
            }
        }

        public void DeleteFile(string directory, string fileName)
        {
            string fullPath = Path.Combine(directory, fileName);

            if (File.Exists(fullPath))
            {
                File.Delete(fullPath);
            }
        }

        public string[] GetFileNames(string directory)
        {
            string[] files = Directory.GetFiles(directory);
            return files.Select(f => Path.GetFileName(f)).ToArray();
        }

        public byte[] GetFileBytes(string filePath)
        {
            return File.ReadAllBytes(filePath);
        }

        public BitmapImage LoadBitmapImage(byte[] buffer, int? width = null, int? height = null)
        {
            MemoryStream stream = new MemoryStream(buffer);
            BitmapImage thumbnailImage = new BitmapImage();
            thumbnailImage.BeginInit();
            thumbnailImage.CacheOption = BitmapCacheOption.None;
            thumbnailImage.CreateOptions = BitmapCreateOptions.IgnoreColorProfile;

            if (width.HasValue && height.HasValue)
            {
                thumbnailImage.DecodePixelWidth = width.Value;
                thumbnailImage.DecodePixelHeight = height.Value;
            }

            thumbnailImage.StreamSource = stream;
            thumbnailImage.Rotation = Rotation.Rotate0;
            thumbnailImage.EndInit();
            thumbnailImage.Freeze();

            return thumbnailImage;
        }

        public BitmapImage LoadBitmapImage(string imagePath)
        {
            BitmapImage image = null;

            if (File.Exists(imagePath))
            {
                image = new BitmapImage();
                image.BeginInit();
                image.CacheOption = BitmapCacheOption.OnLoad;
                image.CreateOptions = BitmapCreateOptions.IgnoreColorProfile;
                image.UriSource = new Uri(imagePath);
                image.Rotation = Rotation.Rotate0;
                image.EndInit();
                image.Freeze();
            }

            return image;
        }

        public byte[] GetJpegBitmapImage(BitmapImage thumbnailImage)
        {
            byte[] imageBuffer;
            BitmapEncoder encoder = new JpegBitmapEncoder();
            encoder.Frames.Add(BitmapFrame.Create(thumbnailImage));

            using (var memoryStream = new MemoryStream())
            {
                encoder.Save(memoryStream);
                imageBuffer = memoryStream.ToArray();
            }

            return imageBuffer;
        }

        public bool HasSameContent(Asset assetA, Asset assetB)
        {
            bool result;

            byte[] assetABytes = File.ReadAllBytes(assetA.FullPath);
            byte[] assetBBytes = File.ReadAllBytes(assetB.FullPath);

            result = assetABytes.Length == assetBBytes.Length;

            if (result)
            {
                for (int i = 0; i < assetABytes.Length; i++)
                {
                    result = (assetABytes[i] == assetBBytes[i]);

                    if (!result)
                    {
                        break;
                    }
                }
            }

            return result;
        }

        public Folder[] GetDrives()
        {
            string[] drives = Directory.GetLogicalDrives();
            return drives.Select(d => new Folder { Path = d }).ToArray();
        }

        public Folder[] GetFolders(Folder parentFolder, bool includeHidden)
        {
            Folder[] result = Array.Empty<Folder>();

            try
            {
                string[] directories = Directory.GetDirectories(parentFolder.Path);
                result = directories.Select(d => new Folder { Path = d }).ToArray();

                if (!includeHidden)
                {
                    result = result.Where(f => !IsHiddenDirectory(f.Path)).ToArray();
                }
            }
            catch (UnauthorizedAccessException ex)
            {

            }

            return result;
        }

        private bool IsHiddenDirectory(string path)
        {
            DirectoryInfo directoryInfo = new DirectoryInfo(path);
            return directoryInfo.Attributes.HasFlag(FileAttributes.Hidden);
        }

        public bool ImageExists(Asset asset, Folder folder)
        {
            string fullPath = Path.Combine(folder.Path, asset.FileName);
            return File.Exists(fullPath);
        }

        public bool ImageExists(string fullPath)
        {
            return File.Exists(fullPath);
        }

        public bool FolderExists(string fullPath)
        {
            return Directory.Exists(fullPath);
        }

        public bool CopyImage(string sourcePath, string destinationPath)
        {
            string destinationFolderPath = new FileInfo(destinationPath).Directory.FullName;
            Directory.CreateDirectory(destinationFolderPath);
            File.Copy(sourcePath, destinationPath);

            return ImageExists(sourcePath) && ImageExists(destinationPath);
        }
    }
}
