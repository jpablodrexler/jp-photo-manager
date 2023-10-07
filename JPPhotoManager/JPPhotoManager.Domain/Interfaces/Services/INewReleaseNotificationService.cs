namespace JPPhotoManager.Domain.Interfaces.Services
{
    public interface INewReleaseNotificationService
    {
        Task<Release> CheckNewReleaseAsync();
    }
}
