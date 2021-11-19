namespace JPPhotoManager.Domain
{
    public interface IImportNewAssetsService
    {
        List<ImportNewAssetsResult> Import(StatusChangeCallback callback);
    }
}