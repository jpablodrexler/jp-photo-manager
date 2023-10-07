namespace JPPhotoManager.Domain.Interfaces.Services
{
    public interface ISyncAssetsService
    {
        Task<List<SyncAssetsResult>> ExecuteAsync(ProcessStatusChangedCallback callback);
    }
}