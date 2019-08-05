using System.Collections.Generic;

namespace JPPhotoManager.Domain
{
    public interface ICatalogAssetsService
    {
        void CatalogImages(CatalogChangeCallback callback);
        Asset CreateThumbnail(Dictionary<string, byte[]> thumbnails, string imagePath);
    }
}
