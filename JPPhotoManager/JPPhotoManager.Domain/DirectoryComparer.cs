using JPPhotoManager.Domain.Interfaces;

namespace JPPhotoManager.Domain
{
    public class DirectoryComparer : IDirectoryComparer
    {
        private IStorageService storageService;

        public DirectoryComparer(IStorageService storageService)
        {
            this.storageService = storageService;
        }

        public string[] GetNewFileNames(string[] fileNames, List<Asset> cataloguedAssets)
        {
            return fileNames.Except(cataloguedAssets.Select(ca => ca.FileName))
                            .Where(f => f.EndsWith(".jpg", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".jpeg", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".jfif", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".png", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".gif", StringComparison.InvariantCultureIgnoreCase))
                            .ToArray();
        }

        public string[] GetNewFileNames(string[] sourceFileNames, string[] destinationFileNames)
        {
            return sourceFileNames.Except(destinationFileNames)
                            .Where(f => f.EndsWith(".jpg", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".jpeg", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".jfif", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".png", StringComparison.InvariantCultureIgnoreCase)
                                || f.EndsWith(".gif", StringComparison.InvariantCultureIgnoreCase))
                            .ToArray();
        }

        public string[] GetUpdatedFileNames(string[] fileNames, List<Asset> cataloguedAssets)
        {
            foreach (Asset asset in cataloguedAssets)
            {
                storageService.GetFileInformation(asset);
            }

            return cataloguedAssets
                .Where(ca => ca.FileCreationDateTime > ca.ThumbnailCreationDateTime ||
                    ca.FileModificationDateTime > ca.ThumbnailCreationDateTime)
                .Select(ca => ca.FileName)
                .ToArray();
        }

        public string[] GetDeletedFileNames(string[] fileNames, List<Asset> cataloguedAssets)
        {
            return cataloguedAssets.Select(ca => ca.FileName).Except(fileNames).ToArray();
        }
    }
}
