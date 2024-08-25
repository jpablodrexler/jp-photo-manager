namespace JPPhotoManager.Domain.Interfaces.Services
{
    public interface IConvertAssetsService
    {
        Task<List<ConvertAssetsResult>> ExecuteAsync(ProcessStatusChangedCallback callback);
    }
}
