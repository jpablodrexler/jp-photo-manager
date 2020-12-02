using System.Collections.Generic;

namespace JPPhotoManager.Domain
{
    public interface IRemoveDuplicatedAssetsService
    {
        void RemoveDuplicatesFromParentFolder(List<DuplicatedAssetCollection> duplicatedAssetCollectionSets);
    }
}
