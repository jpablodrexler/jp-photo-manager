using JPPhotoManager.Domain;
using Octokit;

namespace JPPhotoManager.Infrastructure
{
    public class GitHubReleaseAvailabilityService : IReleaseAvailabilityService
    {
        private readonly IUserConfigurationService userConfigurationService;

        public GitHubReleaseAvailabilityService(IUserConfigurationService userConfigurationService)
        {
            this.userConfigurationService = userConfigurationService;
        }

        public async Task<Domain.Release> GetLatestRelease()
        {
            var repositoryName = userConfigurationService.GetRepositoryName();
            var repositoryOwner = userConfigurationService.GetRepositoryOwner();
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
