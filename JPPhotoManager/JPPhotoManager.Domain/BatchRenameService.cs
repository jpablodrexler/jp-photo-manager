using JPPhotoManager.Domain.Interfaces;

namespace JPPhotoManager.Domain
{
    public class BatchRenameService : IBatchRenameService
    {
        private readonly IStorageService _storageService;

        public BatchRenameService(IStorageService storageService)
        {
            _storageService = storageService;
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
                        _storageService,
                        overwriteExistingTargetFiles);

                    if (!string.IsNullOrEmpty(targetPath)
                        && _storageService.MoveImage(sourceAssets[i].FullPath, targetPath))
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
