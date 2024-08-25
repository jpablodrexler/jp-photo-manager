using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Domain.Interfaces.Repositories;

namespace JPPhotoManager.Infrastructure.Repositories
{
    public class ConvertAssetsConfigurationRepository : IConvertAssetsConfigurationRepository
    {
        private readonly AppDbContext _appDbContext;
        private readonly SyncLock _syncLock;

        public ConvertAssetsConfigurationRepository(AppDbContext appDbContext, SyncLock syncLock)
        {
            _appDbContext = appDbContext;
            _syncLock = syncLock;
        }

        public ConvertAssetsConfiguration GetConvertAssetsConfiguration()
        {
            ConvertAssetsConfiguration result = new();

            lock (_syncLock)
            {
                result.Definitions = _appDbContext
                    .ConvertAssetsDirectoriesDefinitions
                    .OrderBy(d => d.Order)
                    .ToList();
            }

            return result;
        }

        public void SaveConvertAssetsConfiguration(ConvertAssetsConfiguration convertAssetsConfiguration)
        {
            lock (_syncLock)
            {
                var existingDefinitions = _appDbContext.ConvertAssetsDirectoriesDefinitions.ToList();
                var definitionsToDelete = existingDefinitions.Except(convertAssetsConfiguration.Definitions).ToList();
                _appDbContext.ConvertAssetsDirectoriesDefinitions.RemoveRange(definitionsToDelete);

                for (int i = 0; i < convertAssetsConfiguration.Definitions.Count; i++)
                {
                    var definition = convertAssetsConfiguration.Definitions[i];
                    definition.Order = i;

                    if (definition.Id > 0)
                    {
                        _appDbContext.ConvertAssetsDirectoriesDefinitions.Update(definition);
                    }
                    else
                    {
                        _appDbContext.ConvertAssetsDirectoriesDefinitions.Add(definition);
                    }
                }

                _appDbContext.SaveChanges();
            }
        }
    }
}
