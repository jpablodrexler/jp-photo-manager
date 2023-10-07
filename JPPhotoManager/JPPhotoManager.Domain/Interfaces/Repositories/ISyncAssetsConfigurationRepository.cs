using JPPhotoManager.Domain.Entities;

namespace JPPhotoManager.Domain.Interfaces.Repositories
{
    public interface ISyncAssetsConfigurationRepository
    {
        SyncAssetsConfiguration GetSyncAssetsConfiguration();
        void SaveSyncAssetsConfiguration(SyncAssetsConfiguration syncAssetsConfiguration);
    }
}
