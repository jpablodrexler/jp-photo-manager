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

        public async Task<List<ImportNewAssetsResult>> Import(StatusChangeCallback callback)
        {
            return await Task.Run(() =>
            {
                List<ImportNewAssetsResult> result = new List<ImportNewAssetsResult>();
                var configuration = assetRepository.GetImportNewAssetsConfiguration();

                foreach (var import in configuration.Imports)
                {
                    Import(import.SourceDirectory, import.DestinationDirectory, import.IncludeSubFolders, callback, result);
                }

                return result;
            });
        }

        private void Import(string sourceDirectory, string destinationDirectory, bool includeSubFolders, StatusChangeCallback callback, List<ImportNewAssetsResult> resultList)
        {
            ImportNewAssetsResult result = new ImportNewAssetsResult()
            {
                SourceDirectory = sourceDirectory,
                DestinationDirectory = destinationDirectory
            };

            if (!storageService.FolderExists(sourceDirectory))
            {
                result.Message = $"Source directory '{sourceDirectory}' not found.";
                resultList.Add(result);
            }
            else
            {
                try
                {
                    if (!storageService.FolderExists(destinationDirectory))
                    {
                        storageService.CreateDirectory(destinationDirectory);
                    }

                    string[] sourceFileNames = storageService.GetFileNames(sourceDirectory);
                    string[] destinationFileNames = storageService.GetFileNames(destinationDirectory);
                    string[] newFileNames = directoryComparer.GetNewFileNames(sourceFileNames, destinationFileNames);
                    newFileNames = GetFilesNotAlreadyInDestinationSubDirectories(newFileNames, destinationDirectory);

                    foreach (string newImage in newFileNames)
                    {
                        string sourcePath = Path.Combine(sourceDirectory, newImage);
                        string destinationPath = Path.Combine(destinationDirectory, newImage);

                        if (storageService.CopyImage(sourcePath, destinationPath))
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
                        var subdirectories = storageService.GetSubDirectories(sourceDirectory);

                        if (subdirectories != null)
                        {
                            foreach (var subdir in subdirectories)
                            {
                                Import(subdir.FullName, Path.Combine(destinationDirectory, subdir.Name), includeSubFolders, callback, resultList);
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
            List<DirectoryInfo> destinationSubDirectories = storageService.GetRecursiveSubDirectories(destinationDirectory);

            if (destinationSubDirectories != null)
            {
                foreach (var dir in destinationSubDirectories)
                {
                    string[] destinationFileNames = storageService.GetFileNames(dir.FullName);
                    newFileNames = directoryComparer.GetNewFileNames(newFileNames, destinationFileNames);
                }
            }

            return newFileNames;
        }
    }
}
