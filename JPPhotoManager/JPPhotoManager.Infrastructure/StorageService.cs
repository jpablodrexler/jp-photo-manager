using JPPhotoManager.Domain;
using System.IO;
using System.Text.Json;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Infrastructure
{
    public class StorageService : IStorageService
    {
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

        public List<DirectoryInfo> GetRecursiveSubDirectories(string directoryPath)
        {
            List<DirectoryInfo> result = new List<DirectoryInfo>();
            GetRecursiveSubDirectories(directoryPath, result);

            return result;
        }

        private void GetRecursiveSubDirectories(string directoryPath, List<DirectoryInfo> result)
        {
            List<DirectoryInfo> subdirs = GetSubDirectories(directoryPath);
            result.AddRange(subdirs);

            foreach (var dir in subdirs)
            {
                GetRecursiveSubDirectories(dir.FullName, result);
            }
        }

        public string ResolveDataDirectory(int storageVersion)
        {
            return Path.Combine(userConfigurationService.GetApplicationDataFolder(), "v" + storageVersion);
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
