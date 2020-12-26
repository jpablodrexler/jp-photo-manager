using System.Collections.Generic;
using System.Diagnostics;

namespace JPPhotoManager.Domain
{
    public class RemoveDuplicatedAssetsService : IRemoveDuplicatedAssetsService
    {
        private readonly IStorageService storageService;
        private readonly IMoveAssetsService moveAssetsService;

        public RemoveDuplicatedAssetsService(
            IStorageService storageService,
            IMoveAssetsService moveAssetsService)
        {
            this.storageService = storageService;
            this.moveAssetsService = moveAssetsService;
        }

        public void RemoveDuplicatesFromParentFolder(List<List<Asset>> duplicatedAssetCollectionSets)
        {
            foreach (var duplicatedSet in duplicatedAssetCollectionSets) // TODO: IF MULTIPLE DUPLICATED SETS ARE PROCESSED, THIS THROWS AN ERROR OF MODIFIED COLLECTION
            {
                if (duplicatedSet.Count == 2) // TODO: MAKE IT WORK WITH MORE THAN 2 ASSETS PER SET
                {
                    RemoveDuplicatesFromParentFolder(duplicatedSet);
                }
            }
        }

        // TODO: THIS METHOD SHOULD RUN IN THE BACKGROUND
        // TODO: ADD LOGIC SO THE CONDITION OF PARENT FOLDER CAN CONSIDER N LEVELS
        private void RemoveDuplicatesFromParentFolder(List<Asset> duplicatedSet)
        {
            // TODO: THIS CONDITION SHOULD BE ENCAPSULATED IN A STRATEGY PATTERN

            // If the first directory is parent of the second directory
            if (duplicatedSet[0].Folder.IsParentOf(duplicatedSet[1].Folder, storageService))
            {
                // TODO: TEMPORARY CODE, IMPLEMENT A LIST TO DISPLAY AS RESULT IN A DATAGRID.
                Debug.WriteLine($"Deleting {duplicatedSet[0].FullPath}, keeping {duplicatedSet[1].FullPath}");

                // The asset in the first directory (the parent) is removed.
                this.moveAssetsService.DeleteAsset(duplicatedSet[0], deleteFile: true);
            }

            // TODO: THIS CONDITION SHOULD BE ENCAPSULATED IN A STRATEGY PATTERN

            // If the second directory is parent of the first directory
            if (duplicatedSet[1].Folder.IsParentOf(duplicatedSet[0].Folder, storageService))
            {
                // TODO: TEMPORARY CODE, IMPLEMENT A LIST TO DISPLAY AS RESULT IN A DATAGRID.
                Debug.WriteLine($"Deleting {duplicatedSet[1].FullPath}, keeping {duplicatedSet[0].FullPath}");

                // The asset in the second directory (the parent) is removed.
                this.moveAssetsService.DeleteAsset(duplicatedSet[1], deleteFile: true);
            }
        }
    }
}
