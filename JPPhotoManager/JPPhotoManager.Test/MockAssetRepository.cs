using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using System.Collections.Generic;

namespace JPPhotoManager.Test
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

        public void AddFakeAsset(Asset asset, byte[] thumbnailData)
        {
            base.AddAsset(asset, thumbnailData);
        }

        protected override Dictionary<string, byte[]> GetThumbnails(string thumbnailsFileName, out bool isNewFile)
        {
            isNewFile = false;
            return this.thumbnails;
        }
    }
}
