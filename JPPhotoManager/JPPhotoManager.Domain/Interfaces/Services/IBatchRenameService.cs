using JPPhotoManager.Domain.Entities;

namespace JPPhotoManager.Domain.Interfaces.Services
{
    public interface IBatchRenameService
    {
        BatchRenameResult BatchRename(Asset[] sourceAssets, string batchFormat, bool overwriteExistingTargetFiles);
    }
}
