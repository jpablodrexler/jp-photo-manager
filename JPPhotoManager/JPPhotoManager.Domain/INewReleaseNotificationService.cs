namespace JPPhotoManager.Domain
{
    public interface INewReleaseNotificationService
    {
        Task<Release> CheckNewRelease();
    }
}
