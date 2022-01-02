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

            if (Asset.IsValidBatchFormat(batchFormat))
            {
                for (int i = 0; i < sourceAssets.Length; i++)
                {
                    string newName = sourceAssets[i].ComputeTargetFileName(batchFormat, i + 1, Thread.CurrentThread.CurrentCulture);

                    if (!string.IsNullOrEmpty(newName))
                    {
                        string sourcePath = sourceAssets[i].FullPath;
                        string destinationPath = Path.Combine(sourceAssets[i].Folder.Path, newName);

                        if (storageService.MoveImage(sourcePath, destinationPath))
                        {
                            batchRenameResult.SourceAssets.Add(sourceAssets[i]);
                            batchRenameResult.TargetFileNames.Add(newName);
                        }
                    }
                }
            }

            return batchRenameResult;
        }
    }
}
