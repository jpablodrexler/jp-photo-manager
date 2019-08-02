using AssetManager.Domain;
using AssetManager.Infrastructure;
using System.Collections.Generic;

namespace AssetManager.Test
{
    internal class MockAssetRepository : AssetRepository
    {
        private readonly Dictionary<string, byte[]> thumbnails;

        internal MockAssetRepository(Dictionary<string, byte[]> thumbnails,
            IStorageService storageService,
            IUserConfigurationService userConfigurationService) : base(
                storageService, userConfigurationService)
        {
            this.thumbnails = thumbnails;
        }

        public void AddFakeAsset(Asset asset)
        {
            base.AddAsset(asset);
        }

        public override Dictionary<string, byte[]> GetThumbnails(string thumbnailsFileName, out bool isNewFile)
        {
            isNewFile = false;
            return this.thumbnails;
        }
    }
}
