using System.Collections.Generic;

namespace AssetManager.Domain
{
    public interface IFindDuplicatedAssetsService
    {
        List<DuplicatedAssetCollection> GetDuplicatedAssets();
    }
}