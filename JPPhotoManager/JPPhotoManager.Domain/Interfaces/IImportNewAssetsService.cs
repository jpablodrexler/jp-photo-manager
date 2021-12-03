namespace JPPhotoManager.Domain.Interfaces
{
    public interface IImportNewAssetsService
    {
        List<ImportNewAssetsResult> Import(StatusChangeCallback callback);
    }
}