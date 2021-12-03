namespace JPPhotoManager.Domain.Interfaces
{
    public interface INewReleaseNotificationService
    {
        Task<Release> CheckNewRelease();
    }
}
