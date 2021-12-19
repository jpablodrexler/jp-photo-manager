namespace JPPhotoManager.Domain.Interfaces
{
    public interface IImportNewAssetsService
    {
        Task<List<ImportNewAssetsResult>> ImportAsync(ProcessStatusChangedCallback callback);
    }
}