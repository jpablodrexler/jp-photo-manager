using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;

namespace JPPhotoManager.Infrastructure
{
    public class SyncAssetsConfigurationRepository : ISyncAssetsConfigurationRepository
    {
        private readonly AppDbContext _appDbContext;
        private readonly SyncLock _syncLock;

        public SyncAssetsConfigurationRepository(AppDbContext appDbContext, SyncLock syncLock)
        {
            _appDbContext = appDbContext;
            _syncLock = syncLock;
        }

        public SyncAssetsConfiguration GetSyncAssetsConfiguration()
        {
            SyncAssetsConfiguration result = new();

            lock (_syncLock)
            {
                result.Definitions = _appDbContext
                    .SyncAssetsDirectoriesDefinitions
                    .OrderBy(d => d.Order)
                    .ToList();
            }

            return result;
        }

        public void SaveSyncAssetsConfiguration(SyncAssetsConfiguration syncAssetsConfiguration)
        {
            lock (_syncLock)
            {
                var existingDefinitions = _appDbContext.SyncAssetsDirectoriesDefinitions.ToList();
                var definitionsToDelete = existingDefinitions.Except(syncAssetsConfiguration.Definitions).ToList();
                _appDbContext.SyncAssetsDirectoriesDefinitions.RemoveRange(definitionsToDelete);

                for (int i = 0; i < syncAssetsConfiguration.Definitions.Count; i++)
                {
                    var definition = syncAssetsConfiguration.Definitions[i];
                    definition.Order = i;

                    if (definition.Id > 0)
                    {
                        _appDbContext.SyncAssetsDirectoriesDefinitions.Update(definition);
                    }
                    else
                    {
                        _appDbContext.SyncAssetsDirectoriesDefinitions.Add(definition);
                    }
                }

                _appDbContext.SaveChanges();
            }
        }
    }
}
