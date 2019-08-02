using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Media.Imaging;

namespace AssetManager.Domain
{
    public interface IStorageService
    {
        string GetParentDirectory(string directoryPath);
        string ResolveDataDirectory(string dataDirectory);
        string ResolveCatalogPath(string dataDirectory);
        string ResolveThumbnailsFilePath(string dataDirectory, string thumbnailsFileName);
        void CreateDirectory(string directory);
        T ReadObjectFromJson<T>(string jsonFilePath);
        void WriteObjectToJson(object anObject, string jsonFilePath);
        object ReadObjectFromBinaryFile(string binaryFilePath);
        void WriteObjectToBinaryFile(object anObject, string binaryFilePath);
        void DeleteFile(string directory, string fileName);
        string[] GetFileNames(string directory);
        byte[] GetFileBytes(string filePath);
        BitmapImage LoadBitmapImage(byte[] buffer, int? width = null, int? height = null);
        bool HasSameContent(Asset assetA, Asset assetB);
        BitmapImage LoadBitmapImage(string imagePath);
        byte[] GetJpegBitmapImage(BitmapImage thumbnailImage);
        Folder[] GetDrives();
        Folder[] GetFolders(Folder parentFolder, bool includeHidden);
        bool ImageExists(string path);
        bool CopyImage(string sourcePath, string destinationPath);
    }
}
