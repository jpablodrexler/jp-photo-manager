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

        public List<ImportNewAssetsResult> Import()
        {
            List<ImportNewAssetsResult> result = new List<ImportNewAssetsResult>();
            var configuration = this.assetRepository.GetImportNewAssetsConfiguration();

            foreach (var import in configuration.Imports)
            {
                result.Add(this.Import(import.SourceDirectory, import.DestinationDirectory));
            }

            return result;
        }

        private ImportNewAssetsResult Import(string sourceDirectory, string destinationDirectory)
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
                    }
                }

                result.Message = $"{result.ImportedImages} images imported from '{sourceDirectory}' to '{destinationDirectory}'.";
            }

            return result;
        }
    }
}
