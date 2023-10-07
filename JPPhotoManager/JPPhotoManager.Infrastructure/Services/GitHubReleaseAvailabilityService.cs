using JPPhotoManager.Domain.Interfaces.Services;
using Octokit;

namespace JPPhotoManager.Infrastructure.Services
{
    public class GitHubReleaseAvailabilityService : IReleaseAvailabilityService
    {
        private readonly IUserConfigurationService _userConfigurationService;

        public GitHubReleaseAvailabilityService(IUserConfigurationService userConfigurationService)
        {
            _userConfigurationService = userConfigurationService;
        }

        public async Task<Domain.Release> GetLatestReleaseAsync()
        {
            var repositoryName = _userConfigurationService.GetRepositoryName();
            var repositoryOwner = _userConfigurationService.GetRepositoryOwner();
            var github = new GitHubClient(new ProductHeaderValue(repositoryName));
            var release = await github.Repository.Release.GetLatest(repositoryOwner, repositoryName);

            return new Domain.Release
            {
                Name = release.Name,
                PublishedOn = release.PublishedAt,
                DownloadUrl = release.HtmlUrl
            };
        }
    }
}
