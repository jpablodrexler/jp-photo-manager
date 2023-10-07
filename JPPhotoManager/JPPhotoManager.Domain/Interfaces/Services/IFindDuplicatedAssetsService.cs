using JPPhotoManager.Domain.Entities;

namespace JPPhotoManager.Domain.Interfaces.Services
{
    public interface IFindDuplicatedAssetsService
    {
        List<List<Asset>> GetDuplicatedAssets();
    }
}