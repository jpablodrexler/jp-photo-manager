namespace JPPhotoManager.Domain.Interfaces
{
    public interface IRemoveDuplicatedAssetsService
    {
        void RemoveDuplicatesFromParentFolder(List<List<Asset>> duplicatedAssets);
    }
}
