using System.Collections.Generic;

namespace AssetManager.Domain
{
    public interface ICatalogAssetsService
    {
        void CatalogImages(CatalogChangeCallback callback);
        Asset CreateThumbnail(Dictionary<string, byte[]> thumbnails, string imagePath);
    }
}
