using JPPhotoManager.Domain.Entities;

namespace JPPhotoManager.Domain.Interfaces.Repositories
{
    public interface IConvertAssetsConfigurationRepository
    {
        ConvertAssetsConfiguration GetConvertAssetsConfiguration();
        void SaveConvertAssetsConfiguration(ConvertAssetsConfiguration convertAssetsConfiguration);
    }
}
