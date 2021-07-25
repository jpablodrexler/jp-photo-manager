using System.Collections.Generic;

namespace JPPhotoManager.Domain
{
    public interface IFindDuplicatedAssetsService
    {
        List<List<Asset>> GetDuplicatedAssets();
    }
}