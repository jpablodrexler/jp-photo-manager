using JPPhotoManager.Common;
using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;
using System.IO;
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
            return new DirectoryInfo(directoryPath).Parent?.FullName;
        }

        public List<DirectoryInfo> GetSubDirectories(string directoryPath)
        {
            return new DirectoryInfo(directoryPath).EnumerateDirectories().ToList();
        }

        public List<DirectoryInfo> GetRecursiveSubDirectories(string directoryPath)
        {
            List<DirectoryInfo> result = new();
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

        public T ReadObjectFromJsonFile<T>(string jsonFilePath)
        {
            return FileHelper.ReadObjectFromJsonFile<T>(jsonFilePath);
        }

        public void WriteObjectToJsonFile(object anObject, string jsonFilePath)
        {
            FileHelper.WriteObjectToJsonFile(anObject, jsonFilePath);
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
            return BitmapHelper.LoadBitmapImage(buffer, width, height);
        }

        public BitmapImage LoadBitmapImage(byte[] buffer, Rotation rotation, int width, int height)
        {
            return BitmapHelper.LoadBitmapImage(buffer, rotation, width, height);
        }

        public BitmapImage LoadBitmapImage(string imagePath, Rotation rotation)
        {
            return BitmapHelper.LoadBitmapImage(imagePath, rotation);
        }

        public BitmapImage LoadBitmapImage(byte[] buffer, Rotation rotation)
        {
            return BitmapHelper.LoadBitmapImage(buffer, rotation);
        }

        public ushort? GetExifOrientation(byte[] buffer)
        {
            return ExifHelper.GetExifOrientation(buffer);
        }

        public Rotation GetImageRotation(ushort exifOrientation)
        {
            return ExifHelper.GetImageRotation(exifOrientation);
        }

        public byte[] GetJpegBitmapImage(BitmapImage thumbnailImage)
        {
            return BitmapHelper.GetJpegBitmapImage(thumbnailImage);
        }

        public byte[] GetPngBitmapImage(BitmapImage thumbnailImage)
        {
            return BitmapHelper.GetPngBitmapImage(thumbnailImage);
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
            CreateDirectory(destinationFolderPath);
            File.Copy(sourcePath, destinationPath);

            return FileExists(sourcePath) && FileExists(destinationPath);
        }

        public bool MoveImage(string sourcePath, string destinationPath)
        {
            string destinationFolderPath = new FileInfo(destinationPath).Directory.FullName;
            CreateDirectory(destinationFolderPath);
            File.Move(sourcePath, destinationPath);

            return !FileExists(sourcePath) && FileExists(destinationPath);
        }

        public void GetFileInformation(Asset asset)
        {
            if (FileExists(asset.FullPath))
            {
                FileInfo info = new(asset.FullPath);
                asset.FileCreationDateTime = info.CreationTime;
                asset.FileModificationDateTime = info.LastWriteTime;
            }
        }
    }
}
