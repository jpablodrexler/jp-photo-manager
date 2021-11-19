namespace JPPhotoManager.Domain
{
    public interface ICatalogAssetsService
    {
        void CatalogAssets(CatalogChangeCallback callback);
        Asset CreateAsset(string directoryName, string fileName);
    }
}
