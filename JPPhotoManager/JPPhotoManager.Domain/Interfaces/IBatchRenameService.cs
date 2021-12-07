namespace JPPhotoManager.Domain.Interfaces
{
    public interface IBatchRenameService
    {
        BatchRenameResult BatchRename(Asset[] sourceAssets, string batchFormat);
    }
}
