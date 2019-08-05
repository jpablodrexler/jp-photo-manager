using System.Collections.Generic;

namespace JPPhotoManager.Domain
{
    public interface IFindDuplicatedAssetsService
    {
        List<DuplicatedAssetCollection> GetDuplicatedAssets();
    }
}