using log4net;
using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Text;

namespace JPPhotoManager.Domain
{
    public class ImportNewAssetsService : IImportNewAssetsService
    {
        private readonly IAssetRepository assetRepository;
        private readonly IStorageService storageService;
        private readonly IDirectoryComparer directoryComparer;

        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        public ImportNewAssetsService(
            IAssetRepository assetRepository,
            IStorageService storageService,
            IDirectoryComparer directoryComparer)
        {
            this.assetRepository = assetRepository;
            this.storageService = storageService;
            this.directoryComparer = directoryComparer;
        }

        public List<ImportNewAssetsResult> Import(StatusChangeCallback callback)
        {
            List<ImportNewAssetsResult> result = new List<ImportNewAssetsResult>();
            var configuration = this.assetRepository.GetImportNewAssetsConfiguration();

            foreach (var import in configuration.Imports)
            {
                result.Add(this.Import(import.SourceDirectory, import.DestinationDirectory, callback));
            }

            return result;
        }

        private ImportNewAssetsResult Import(string sourceDirectory, string destinationDirectory, StatusChangeCallback callback)
        {
            ImportNewAssetsResult result = new ImportNewAssetsResult()
            {
                SourceDirectory = sourceDirectory,
                DestinationDirectory = destinationDirectory
            };

            if (!this.storageService.FolderExists(sourceDirectory))
            {
                result.Message = $"Source directory '{sourceDirectory}' not found.";
            }
            else if (!this.storageService.FolderExists(destinationDirectory))
            {
                result.Message = $"Destination directory '{destinationDirectory}' not found.";
            }
            else
            {
                string[] sourceFileNames = this.storageService.GetFileNames(sourceDirectory);
                string[] destinationFileNames = this.storageService.GetFileNames(destinationDirectory);
                string[] newFileNames = this.directoryComparer.GetNewFileNames(sourceFileNames, destinationFileNames);

                foreach (string newImage in newFileNames)
                {
                    string sourcePath = Path.Combine(sourceDirectory, newImage);
                    string destinationPath = Path.Combine(destinationDirectory, newImage);

                    if (this.storageService.CopyImage(sourcePath, destinationPath))
                    {
                        result.ImportedImages++;
                        callback(new StatusChangeCallbackEventArgs { NewStatus = $"Image '{sourcePath}' imported to '{destinationPath}'" });
                    }
                }

                switch (result.ImportedImages)
                {
                    case 0:
                        result.Message = $"No images imported from '{sourceDirectory}' to '{destinationDirectory}'.";
                        break;

                    case 1:
                        result.Message = $"{result.ImportedImages} image imported from '{sourceDirectory}' to '{destinationDirectory}'.";
                        break;

                    default:
                        result.Message = $"{result.ImportedImages} images imported from '{sourceDirectory}' to '{destinationDirectory}'.";
                        break;
                }
            }

            return result;
        }
    }
}
