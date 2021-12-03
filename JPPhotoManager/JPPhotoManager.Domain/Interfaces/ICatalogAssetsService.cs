namespace JPPhotoManager.Domain.Interfaces
{
    public interface ICatalogAssetsService
    {
        void CatalogAssets(CatalogChangeCallback callback);
        Asset CreateAsset(string directoryName, string fileName);
    }
}
