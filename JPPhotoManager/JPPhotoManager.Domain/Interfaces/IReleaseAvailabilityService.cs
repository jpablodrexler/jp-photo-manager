namespace JPPhotoManager.Domain.Interfaces
{
    public interface IReleaseAvailabilityService
    {
        Task<Release> GetLatestReleaseAsync();
    }
}
