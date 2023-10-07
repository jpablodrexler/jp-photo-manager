namespace JPPhotoManager.Domain.Interfaces.Services
{
    public interface IReleaseAvailabilityService
    {
        Task<Release> GetLatestReleaseAsync();
    }
}
