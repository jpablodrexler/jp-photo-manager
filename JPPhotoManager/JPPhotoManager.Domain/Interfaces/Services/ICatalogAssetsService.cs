using JPPhotoManager.Domain.Entities;

namespace JPPhotoManager.Domain.Interfaces.Services
{
    public interface ICatalogAssetsService
    {
        Task CatalogAssetsAsync(CatalogChangeCallback callback);
        Asset CreateAsset(string directoryName, string fileName);
    }
}
