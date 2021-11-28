using log4net;
using System.Reflection;

namespace JPPhotoManager.Domain
{
    public class NewReleaseNotificationService : INewReleaseNotificationService
    {
        private readonly IUserConfigurationService userConfigurationService;
        private readonly IReleaseAvailabilityService releaseAvailabilityService;

        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        public NewReleaseNotificationService(IUserConfigurationService userConfigurationService,
            IReleaseAvailabilityService releaseAvailabilityService)
        {
            this.userConfigurationService = userConfigurationService;
            this.releaseAvailabilityService = releaseAvailabilityService;
        }

        public async Task<Release> CheckNewRelease()
        {
            Release latestRelease;

            try
            {
                var aboutInformation = this.userConfigurationService.GetAboutInformation(this.GetType().Assembly);
                latestRelease = await this.releaseAvailabilityService.GetLatestRelease();

                if (latestRelease != null)
                {
                    latestRelease.IsNewRelease = !string.IsNullOrEmpty(aboutInformation.Version)
                        && !string.IsNullOrEmpty(latestRelease.Name)
                        && string.Compare(aboutInformation.Version, latestRelease.Name, StringComparison.OrdinalIgnoreCase) != 0;
                    latestRelease.Success = true;
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);

                latestRelease = new Release
                {
                    IsNewRelease = false,
                    Success = false
                };
            }

            return latestRelease;
        }
    }
}
