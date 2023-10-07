using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Domain.Interfaces.Repositories;
using JPPhotoManager.Domain.Interfaces.Services;

namespace JPPhotoManager.Domain.Services
{
    public class FindDuplicatedAssetsService : IFindDuplicatedAssetsService
    {
        private readonly IAssetRepository _assetRepository;
        private readonly IStorageService _storageService;

        public FindDuplicatedAssetsService(
            IAssetRepository assetRepository,
            IStorageService storageService)
        {
            _assetRepository = assetRepository;
            _storageService = storageService;
        }

        /// <summary>
        /// Detects duplicated assets in the catalog.
        /// </summary>
        /// <returns>A list of duplicated sets of assets (corresponding to the same image),
        /// where each item is a list of duplicated assets.</returns>
        public List<List<Asset>> GetDuplicatedAssets()
        {
            List<List<Asset>> result = new();
            List<Asset> assets = new(_assetRepository.GetCataloguedAssets());
            var assetGroups = assets.GroupBy(a => a.Hash);
            assetGroups = assetGroups.Where(g => g.Count() > 1);

            foreach (var group in assetGroups)
            {
                result.Add(group.OrderByDescending(g => g.FileCreationDateTime).ToList());
            }

            // Removes stale assets, whose files no longer exists.
            foreach (List<Asset> duplicatedSet in result)
            {
                List<Asset> assetsToRemove = new();

                for (int i = 0; i < duplicatedSet.Count; i++)
                {
                    if (!_storageService.FileExists(duplicatedSet[i].FullPath))
                    {
                        assetsToRemove.Add(duplicatedSet[i]);
                    }
                }

                foreach (Asset asset in assetsToRemove)
                {
                    duplicatedSet.Remove(asset);
                }
            }

            result = result.Where(r => r.Count > 1).ToList();

            // Loads the file information for each asset.
            foreach (List<Asset> duplicatedSet in result)
            {
                foreach (Asset asset in duplicatedSet)
                {
                    _storageService.GetFileInformation(asset);
                }
            }

            return result;
        }
    }
}
