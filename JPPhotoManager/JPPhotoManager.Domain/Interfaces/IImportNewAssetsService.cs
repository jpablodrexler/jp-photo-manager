namespace JPPhotoManager.Domain.Interfaces
{
    public interface IImportNewAssetsService
    {
        Task<List<ImportNewAssetsResult>> Import(StatusChangeCallback callback);
    }
}