using System.Threading;

namespace JPPhotoManager.Domain
{
    public interface ICatalogAssetsService
    {
        void CatalogImages(CatalogChangeCallback callback);
        Asset CreateAsset(string directoryName, string fileName);
    }
}
