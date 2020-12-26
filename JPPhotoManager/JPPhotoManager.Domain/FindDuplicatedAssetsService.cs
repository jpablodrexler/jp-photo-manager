using System.Collections.Generic;
using System.Linq;

namespace JPPhotoManager.Domain
{
    public class FindDuplicatedAssetsService : IFindDuplicatedAssetsService
    {
        private readonly IAssetRepository assetRepository;
        private readonly IStorageService storageService;

        public FindDuplicatedAssetsService(
            IAssetRepository assetRepository,
            IStorageService storageService)
        {
            this.assetRepository = assetRepository;
            this.storageService = storageService;
        }

        /// <summary>
        /// Detects duplicated assets in the catalog.
        /// </summary>
        /// <returns>A list of duplicated sets of assets (corresponding to the same image),
        /// where each item is a list of duplicated assets.</returns>
        public List<List<Asset>> GetDuplicatedAssets()
        {
            List<List<Asset>> result = new List<List<Asset>>();
            List<Asset> assets = this.assetRepository.GetCataloguedAssets();
            var assetGroups = assets.GroupBy(a => a.Hash);
            assetGroups = assetGroups.Where(g => g.Count() > 1);

            // TODO: THIS THROWS AN INVALIDOPERATIONEXCEPTION IF NEW ASSETS ARE BEING CATALOGUED AT THE SAME TIME.
            foreach (var group in assetGroups)
            {
                result.Add(group.ToList());
            }

            // Removes stale assets, whose files no longer exists.
            foreach (List<Asset> duplicatedSet in result)
            {
                List<Asset> assetsToRemove = new List<Asset>();

                for (int i = 0; i < duplicatedSet.Count; i++)
                {
                    if (!storageService.FileExists(duplicatedSet[i].FullPath))
                    {
                        assetsToRemove.Add(duplicatedSet[i]);
                    }
                }

                foreach (Asset asset in assetsToRemove)
                {
                    duplicatedSet.Remove(asset);
                }
            }

            // Removes assets with same hash but different content.
            foreach (List<Asset> duplicatedSet in result)
            {
                List<Asset> assetsToRemove = new List<Asset>();

                for (int i = 1; i < duplicatedSet.Count; i++)
                {
                    if (!this.storageService.HasSameContent(duplicatedSet[0], duplicatedSet[i]))
                    {
                        assetsToRemove.Add(duplicatedSet[i]);
                    }
                }

                foreach (Asset asset in assetsToRemove)
                {
                    duplicatedSet.Remove(asset);
                }
            }

            result = result.Where(r => r.Count() > 1).ToList();

            return result;
        }
    }
}
