namespace JPPhotoManager.Domain.Interfaces
{
    public interface ISyncAssetsConfigurationRepository
    {
        SyncAssetsConfiguration GetSyncAssetsConfiguration();
        void SaveSyncAssetsConfiguration(SyncAssetsConfiguration syncAssetsConfiguration);
    }
}
