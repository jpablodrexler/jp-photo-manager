namespace JPPhotoManager.Domain
{
    public interface IReleaseAvailabilityService
    {
        Task<Release> GetLatestRelease();
    }
}
