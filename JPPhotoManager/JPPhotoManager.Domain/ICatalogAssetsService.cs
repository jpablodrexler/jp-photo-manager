using System.Threading;

namespace JPPhotoManager.Domain
{
    public interface ICatalogAssetsService
    {
        void CatalogImages(CatalogChangeCallback callback);
        Asset CreateAsset(string directoryName, string fileName);
        bool MoveAsset(Asset asset, Folder destinationFolder, bool preserveOriginalFile);
        void DeleteAsset(Asset asset, bool deleteFile);
    }
}
