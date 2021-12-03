using JPPhotoManager.Domain.Interfaces;
using System.IO;

namespace JPPhotoManager.Domain
{
    public class ImportNewAssetsService : IImportNewAssetsService
    {
        private readonly IAssetRepository assetRepository;
        private readonly IStorageService storageService;
        private readonly IDirectoryComparer directoryComparer;

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
                this.Import(import.SourceDirectory, import.DestinationDirectory, import.IncludeSubFolders, callback, result);
            }

            return result;
        }

        private void Import(string sourceDirectory, string destinationDirectory, bool includeSubFolders, StatusChangeCallback callback, List<ImportNewAssetsResult> resultList)
        {
            ImportNewAssetsResult result = new ImportNewAssetsResult()
            {
                SourceDirectory = sourceDirectory,
                DestinationDirectory = destinationDirectory
            };

            if (!this.storageService.FolderExists(sourceDirectory))
            {
                result.Message = $"Source directory '{sourceDirectory}' not found.";
                resultList.Add(result);
            }
            else
            {
                try
                {
                    if (!this.storageService.FolderExists(destinationDirectory))
                    {
                        this.storageService.CreateDirectory(destinationDirectory);
                    }

                    string[] sourceFileNames = this.storageService.GetFileNames(sourceDirectory);
                    string[] destinationFileNames = this.storageService.GetFileNames(destinationDirectory);
                    string[] newFileNames = this.directoryComparer.GetNewFileNames(sourceFileNames, destinationFileNames);
                    newFileNames = GetFilesNotAlreadyInDestinationSubDirectories(newFileNames, destinationDirectory);

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

                    resultList.Add(result);

                    if (includeSubFolders)
                    {
                        var subdirectories = this.storageService.GetSubDirectories(sourceDirectory);

                        if (subdirectories != null)
                        {
                            foreach (var subdir in subdirectories)
                            {
                                this.Import(subdir.FullName, Path.Combine(destinationDirectory, subdir.Name), includeSubFolders, callback, resultList);
                            }
                        }
                    }
                }
                catch (DirectoryNotFoundException)
                {
                    result.Message = $"Destination directory '{destinationDirectory}' not found.";
                    resultList.Add(result);
                }
                catch (IOException ex)
                {
                    result.Message = ex.Message;
                    resultList.Add(result);
                }
            }
        }

        private string[] GetFilesNotAlreadyInDestinationSubDirectories(string[] newFileNames, string destinationDirectory)
        {
            List<DirectoryInfo> destinationSubDirectories = this.storageService.GetRecursiveSubDirectories(destinationDirectory);

            if (destinationSubDirectories != null)
            {
                foreach (var dir in destinationSubDirectories)
                {
                    string[] destinationFileNames = this.storageService.GetFileNames(dir.FullName);
                    newFileNames = this.directoryComparer.GetNewFileNames(newFileNames, destinationFileNames);
                }
            }

            return newFileNames;
        }
    }
}
