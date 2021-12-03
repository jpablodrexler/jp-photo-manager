namespace JPPhotoManager.Domain.Interfaces
{
    public interface IFindDuplicatedAssetsService
    {
        List<List<Asset>> GetDuplicatedAssets();
    }
}