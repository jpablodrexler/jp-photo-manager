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

        public BatchRenameResult BatchRename(Asset[] sourceAssets, string batchFormat, bool overwriteExistingTargetFiles)
        {
            BatchRenameResult batchRenameResult = new();

            if (Asset.IsValidBatchFormat(batchFormat))
            {
                for (int i = 0; i < sourceAssets.Length; i++)
                {
                    string targetPath = sourceAssets[i].ComputeTargetPath(
                        batchFormat,
                        i + 1,
                        Thread.CurrentThread.CurrentCulture,
                        storageService,
                        overwriteExistingTargetFiles);

                    if (!string.IsNullOrEmpty(targetPath)
                        && storageService.MoveImage(sourceAssets[i].FullPath, targetPath))
                    {
                        batchRenameResult.SourceAssets.Add(sourceAssets[i]);
                        batchRenameResult.TargetPaths.Add(targetPath);
                    }
                }
            }

            return batchRenameResult;
        }
    }
}
