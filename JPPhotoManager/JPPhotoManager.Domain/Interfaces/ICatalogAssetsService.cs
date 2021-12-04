namespace JPPhotoManager.Domain.Interfaces
{
    public interface ICatalogAssetsService
    {
        Task CatalogAssets(CatalogChangeCallback callback);
        Asset CreateAsset(string directoryName, string fileName);
    }
}
