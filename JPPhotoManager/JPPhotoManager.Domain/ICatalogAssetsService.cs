using System.Collections.Generic;

namespace JPPhotoManager.Domain
{
    public interface ICatalogAssetsService
    {
        void CatalogImages(CatalogChangeCallback callback);
        Asset CreateAsset(Dictionary<string, byte[]> thumbnails, string directoryName, string fileName);
        bool MoveAsset(Asset asset, Folder sourceFolder, Folder destinationFolder, bool preserveOriginalFile);
    }
}
