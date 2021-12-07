using JPPhotoManager.Domain.Interfaces;
using System.IO;

namespace JPPhotoManager.Domain
{
    public class BatchRenameService : IBatchRenameService
    {
        private readonly IStorageService storageService;

        public BatchRenameService(IStorageService storageService)
        {
            this.storageService = storageService;
        }

        public BatchRenameResult BatchRename(Asset[] sourceAssets, string batchFormat)
        {
            BatchRenameResult batchRenameResult = new();

            for (int i = 0; i < sourceAssets.Length; i++)
            {
                string newName = sourceAssets[i].ComputeTargetFileName(batchFormat, i + 1);
                string sourcePath = sourceAssets[i].FullPath;
                string destinationPath = Path.Combine(sourceAssets[i].Folder.Path, newName);

                if (storageService.MoveImage(sourcePath, destinationPath))
                {
                    batchRenameResult.SourceAssets.Add(sourceAssets[i]);
                    batchRenameResult.TargetFileNames.Add(newName);
                }
            }

            return batchRenameResult;
        }
    }
}
