using System.IO;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain.Interfaces
{
    public interface IStorageService
    {
        string GetParentDirectory(string directoryPath);
        List<DirectoryInfo> GetSubDirectories(string directoryPath);
        List<DirectoryInfo> GetRecursiveSubDirectories(string directoryPath);
        string ResolveDataDirectory(double storageVersion);
        void CreateDirectory(string directory);
        T ReadObjectFromJsonFile<T>(string jsonFilePath);
        void WriteObjectToJsonFile(object anObject, string jsonFilePath);
        void DeleteFile(string directory, string fileName);
        string[] GetFileNames(string directory);
        byte[] GetFileBytes(string filePath);
        BitmapImage LoadBitmapImage(byte[] buffer, Rotation rotation, int width, int height);
        BitmapImage LoadBitmapImage(byte[] buffer, int width, int height);
        BitmapImage LoadBitmapImage(byte[] buffer, Rotation rotation);
        ushort? GetExifOrientation(byte[] buffer);
        Rotation GetImageRotation(ushort exifOrientation);
        bool HasSameContent(Asset assetA, Asset assetB);
        BitmapImage LoadBitmapImage(string imagePath, Rotation rotation);
        byte[] GetJpegBitmapImage(BitmapImage thumbnailImage);
        byte[] GetPngBitmapImage(BitmapImage thumbnailImage);
        Folder[] GetDrives();
        bool FileExists(Asset asset, Folder folder);
        bool FileExists(string fullPath);
        bool FolderExists(string fullPath);
        bool CopyImage(string sourcePath, string destinationPath);
        bool MoveImage(string sourcePath, string destinationPath);
        void GetFileInformation(Asset asset);
    }
}
