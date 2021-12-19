namespace JPPhotoManager.Domain.Interfaces
{
    public interface ICatalogAssetsService
    {
        Task CatalogAssetsAsync(CatalogChangeCallback callback);
        Asset CreateAsset(string directoryName, string fileName);
    }
}
