using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.Serialization.Formatters.Binary;
using System.Text.Json;
using System.Threading;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Infrastructure
{
    public class StorageService : IStorageService
    {
        private const string DATA_FILE_FORMAT = "{0}.db";
        private readonly IUserConfigurationService userConfigurationService;

        public StorageService(IUserConfigurationService userConfigurationService)
        {
            this.userConfigurationService = userConfigurationService;
        }

        public string GetParentDirectory(string directoryPath)
        {
            return new DirectoryInfo(directoryPath).Parent.FullName;
        }

        public List<DirectoryInfo> GetSubDirectories(string directoryPath)
        {
            return new DirectoryInfo(directoryPath).EnumerateDirectories().ToList();
        }

        public string ResolveDataDirectory()
        {
            return userConfigurationService.GetApplicationDataFolder();
        }

        public string ResolveTableFilePath(string dataDirectory, string entityName)
        {
            dataDirectory = !string.IsNullOrEmpty(dataDirectory) ? dataDirectory : string.Empty;
            string fileName = string.Format(DATA_FILE_FORMAT, entityName);
            return Path.Combine(GetTablesDirectory(dataDirectory), fileName);
        }

        public string ResolveBlobFilePath(string dataDirectory, string thumbnailsFileName)
        {
            return Path.Combine(GetBlobsDirectory(dataDirectory), thumbnailsFileName);
        }

        public string GetTablesDirectory(string dataDirectory)
        {
            return Path.Combine(dataDirectory, "Tables");
        }

        public string GetBlobsDirectory(string dataDirectory)
        {
            return Path.Combine(dataDirectory, "Blobs");
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

        public List<T> ReadFromCsv<T>(string dataFilePath, Func<string[], T> mappingFunc)
        {
            List<T> result = new List<T>();
            var separator = Thread.CurrentThread.CurrentUICulture.TextInfo.ListSeparator;

            using (StreamReader reader = new StreamReader(dataFilePath))
            {
                string line = reader.ReadLine();
                bool hasRecord;

                do
                {
                    line = reader.ReadLine();
                    hasRecord = !string.IsNullOrEmpty(line);

                    if (hasRecord)
                    {
                        string[] fields = line.Split(separator);
                        result.Add(mappingFunc(fields));
                    }
                }
                while (hasRecord);
            }
            
            return result;
        }

        public void WriteToCsvFile<T>(string dataFilePath, List<T> records, string[] headers, Func<T, object[]> mappingFunc)
        {
            var separator = Thread.CurrentThread.CurrentUICulture.TextInfo.ListSeparator;
            
            using (StreamWriter writer = new StreamWriter(dataFilePath, false))
            {
                writer.WriteLine(string.Join(separator, headers));

                for (int i = 0; i < records.Count; i++)
                {
                    T record = records[i];
                    string line = string.Join(separator, mappingFunc(record));
                    writer.WriteLine(line);
                }

                writer.Flush();
                writer.Close();
            }
        }

        public object ReadFromBinaryFile(string binaryFilePath)
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

        public void WriteToBinaryFile(object anObject, string binaryFilePath)
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

        public BitmapImage LoadBitmapImage(byte[] buffer, int width, int height)
        {
            // TODO: If the stream is disposed by a using block, the thumbnail is not shown. Find a way to dispose of the stream.
            MemoryStream stream = new MemoryStream(buffer);
            BitmapImage thumbnailImage = new BitmapImage();
            thumbnailImage.BeginInit();
            thumbnailImage.CacheOption = BitmapCacheOption.None;
            thumbnailImage.CreateOptions = BitmapCreateOptions.IgnoreColorProfile;
            thumbnailImage.StreamSource = stream;
            thumbnailImage.DecodePixelWidth = width;
            thumbnailImage.DecodePixelHeight = height;
            thumbnailImage.EndInit();
            thumbnailImage.Freeze();

            return thumbnailImage;
        }

        public BitmapImage LoadBitmapImage(byte[] buffer, Rotation rotation, int width, int height)
        {
            BitmapImage image = null;

            using (MemoryStream stream = new MemoryStream(buffer))
            {
                image = new BitmapImage();
                image.BeginInit();
                image.CacheOption = BitmapCacheOption.OnLoad;
                image.CreateOptions = BitmapCreateOptions.IgnoreColorProfile;
                image.StreamSource = stream;
                image.Rotation = rotation;
                image.DecodePixelWidth = width;
                image.DecodePixelHeight = height;
                image.EndInit();
                image.Freeze();
            }

            return image;
        }

        public BitmapImage LoadBitmapImage(string imagePath, Rotation rotation)
        {
            BitmapImage image = null;

            if (File.Exists(imagePath))
            {
                image = new BitmapImage();
                image.BeginInit();
                image.CacheOption = BitmapCacheOption.OnLoad;
                image.CreateOptions = BitmapCreateOptions.IgnoreColorProfile;
                image.UriSource = new Uri(imagePath);
                image.Rotation = rotation;
                image.EndInit();
                image.Freeze();
            }

            return image;
        }

        public BitmapImage LoadBitmapImage(byte[] buffer, Rotation rotation)
        {
            BitmapImage image = new BitmapImage();

            using (MemoryStream stream = new MemoryStream(buffer))
            {
                image.BeginInit();
                image.CacheOption = BitmapCacheOption.OnLoad;
                image.CreateOptions = BitmapCreateOptions.IgnoreColorProfile;
                image.StreamSource = stream;
                image.Rotation = rotation;
                image.EndInit();
                image.Freeze();
            }

            return image;
        }

        public ushort? GetExifOrientation(byte[] buffer)
        {
            ushort? result = null;
            
            using (MemoryStream stream = new MemoryStream(buffer))
            {
                BitmapFrame bitmapFrame = BitmapFrame.Create(stream);
                BitmapMetadata bitmapMetadata = bitmapFrame.Metadata as BitmapMetadata;

                if (bitmapMetadata != null && bitmapMetadata.ContainsQuery("System.Photo.Orientation"))
                {
                    object value = bitmapMetadata.GetQuery("System.Photo.Orientation");

                    if (value != null)
                    {
                        result = (ushort)value;
                    }
                }
            }

            return result;
        }

        public Rotation GetImageRotation(ushort exifOrientation)
        {
            Rotation rotation = Rotation.Rotate0;

            switch (exifOrientation)
            {
                case 1:
                    rotation = Rotation.Rotate0;
                    break;
                case 2:
                    rotation = Rotation.Rotate0; // FlipX
                    break;
                case 3:
                    rotation = Rotation.Rotate180;
                    break;
                case 4:
                    rotation = Rotation.Rotate180; // FlipX
                    break;
                case 5:
                    rotation = Rotation.Rotate90; // FlipX
                    break;
                case 6:
                    rotation = Rotation.Rotate90;
                    break;
                case 7:
                    rotation = Rotation.Rotate270; // FlipX
                    break;
                case 8:
                    rotation = Rotation.Rotate270;
                    break;
                default:
                    rotation = Rotation.Rotate0;
                    break;
            }

            return rotation;
        }

        public byte[] GetJpegBitmapImage(BitmapImage thumbnailImage)
        {
            return GetBitmapImage(thumbnailImage, new JpegBitmapEncoder());
        }

        public byte[] GetPngBitmapImage(BitmapImage thumbnailImage)
        {
            return GetBitmapImage(thumbnailImage, new PngBitmapEncoder());
        }

        private byte[] GetBitmapImage(BitmapImage thumbnailImage, BitmapEncoder encoder)
        {
            byte[] imageBuffer;
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

        public bool FileExists(Asset asset, Folder folder)
        {
            string fullPath = Path.Combine(folder.Path, asset.FileName);
            return File.Exists(fullPath);
        }

        public bool FileExists(string fullPath)
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
            this.CreateDirectory(destinationFolderPath);
            File.Copy(sourcePath, destinationPath);

            return FileExists(sourcePath) && FileExists(destinationPath);
        }

        public void GetFileInformation(Asset asset)
        {
            if (FileExists(asset.FullPath))
            {
                FileInfo info = new FileInfo(asset.FullPath);
                asset.FileCreationDateTime = info.CreationTime;
                asset.FileModificationDateTime = info.LastWriteTime;
            }
        }
    }
}
