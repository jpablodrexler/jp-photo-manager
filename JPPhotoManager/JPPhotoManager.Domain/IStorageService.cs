using System;
using System.Collections.Generic;
using System.IO;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain
{
    public interface IStorageService
    {
        string GetParentDirectory(string directoryPath);
        List<DirectoryInfo> GetSubDirectories(string directoryPath);
        string ResolveDataDirectory();
        string ResolveTableFilePath(string dataDirectory, string entityName);
        string ResolveBlobFilePath(string dataDirectory, string thumbnailsFileName);
        string GetTablesDirectory(string dataDirectory);
        string GetBlobsDirectory(string dataDirectory);
        void CreateDirectory(string directory);
        T ReadObjectFromJson<T>(string jsonFilePath);
        void WriteObjectToJson(object anObject, string jsonFilePath);
        List<T> ReadFromCsv<T>(string dataFilePath, Func<string[], T> mappingFunc);
        void WriteToCsvFile<T>(string dataFilePath, List<T> records, string[] headers, Func<T, object[]> mappingFunc);
        object ReadFromBinaryFile(string binaryFilePath);
        void WriteToBinaryFile(object anObject, string binaryFilePath);
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
        Folder[] GetFolders(Folder parentFolder, bool includeHidden);
        bool FileExists(Asset asset, Folder folder);
        bool FileExists(string fullPath);
        bool FolderExists(string fullPath);
        bool CopyImage(string sourcePath, string destinationPath);
    }
}
