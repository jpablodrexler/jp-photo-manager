using JPPhotoManager.Domain.Interfaces;
using System.IO;

namespace JPPhotoManager.Domain
{
    public class SyncAssetsService : ISyncAssetsService
    {
        private readonly IAssetRepository _assetRepository;
        private readonly IStorageService _storageService;
        private readonly IDirectoryComparer _directoryComparer;

        public SyncAssetsService(
            IAssetRepository assetRepository,
            IStorageService storageService,
            IDirectoryComparer directoryComparer)
        {
            _assetRepository = assetRepository;
            _storageService = storageService;
            _directoryComparer = directoryComparer;
        }

        public async Task<List<SyncAssetsResult>> ExecuteAsync(ProcessStatusChangedCallback callback)
        {
            return await Task.Run(() =>
            {
                List<SyncAssetsResult> result = new();
                var configuration = _assetRepository.GetSyncAssetsConfiguration();

                foreach (var definition in configuration.Definitions)
                {
                    Execute(definition.SourceDirectory,
                        definition.DestinationDirectory,
                        definition.IncludeSubFolders,
                        definition.DeleteAssetsNotInSource,
                        callback,
                        result);
                }

                return result;
            });
        }

        private void Execute(string sourceDirectory,
            string destinationDirectory,
            bool includeSubFolders,
            bool deleteAssetsNotInSource,
            ProcessStatusChangedCallback callback,
            List<SyncAssetsResult> resultList)
        {
            SyncAssetsResult result = new()
            {
                SourceDirectory = sourceDirectory,
                DestinationDirectory = destinationDirectory
            };

            if (!_storageService.FolderExists(sourceDirectory))
            {
                result.Message = $"Source directory '{sourceDirectory}' not found.";
                resultList.Add(result);
            }
            else
            {
                try
                {
                    if (!_storageService.FolderExists(destinationDirectory))
                    {
                        _storageService.CreateDirectory(destinationDirectory);
                    }

                    string[] sourceFileNames = _storageService.GetFileNames(sourceDirectory);
                    string[] destinationFileNames = _storageService.GetFileNames(destinationDirectory);
                    string[] newFileNames = _directoryComparer.GetNewFileNames(sourceFileNames, destinationFileNames);
                    newFileNames = GetFilesNotAlreadyInDestinationSubDirectories(newFileNames, destinationDirectory);

                    foreach (string newImage in newFileNames)
                    {
                        string sourcePath = Path.Combine(sourceDirectory, newImage);
                        string destinationPath = Path.Combine(destinationDirectory, newImage);

                        if (_storageService.CopyImage(sourcePath, destinationPath))
                        {
                            result.SyncedImages++;
                            callback(new ProcessStatusChangedCallbackEventArgs { NewStatus = $"'{sourcePath}' => '{destinationPath}'" });
                        }
                    }

                    // TODO: Detect and sync updated assets.

                    if (deleteAssetsNotInSource)
                    {
                        string[] deletedFileNames = _directoryComparer.GetDeletedFileNames(sourceFileNames, destinationFileNames);

                        foreach (string deletedImage in deletedFileNames)
                        {
                            string destinationPath = Path.Combine(destinationDirectory, deletedImage);
                            _storageService.DeleteFile(destinationDirectory, deletedImage);
                            result.SyncedImages++;
                            callback(new ProcessStatusChangedCallbackEventArgs { NewStatus = $"Deleted '{destinationPath}'" });
                        }
                    }
                    
                    switch (result.SyncedImages)
                    {
                        case 0:
                            result.Message = $"No images synced from '{sourceDirectory}' to '{destinationDirectory}'.";
                            break;

                        case 1:
                            result.Message = $"{result.SyncedImages} image synced from '{sourceDirectory}' to '{destinationDirectory}'.";
                            break;

                        default:
                            result.Message = $"{result.SyncedImages} images synced from '{sourceDirectory}' to '{destinationDirectory}'.";
                            break;
                    }

                    resultList.Add(result);

                    if (includeSubFolders)
                    {
                        var subdirectories = _storageService.GetSubDirectories(sourceDirectory);

                        if (subdirectories != null)
                        {
                            foreach (var subdir in subdirectories)
                            {
                                Execute(subdir.FullName,
                                    Path.Combine(destinationDirectory, subdir.Name),
                                    includeSubFolders,
                                    deleteAssetsNotInSource,
                                    callback,
                                    resultList);
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
            List<DirectoryInfo> destinationSubDirectories = _storageService.GetRecursiveSubDirectories(destinationDirectory);

            if (destinationSubDirectories != null)
            {
                foreach (var dir in destinationSubDirectories)
                {
                    string[] destinationFileNames = _storageService.GetFileNames(dir.FullName);
                    newFileNames = _directoryComparer.GetNewFileNames(newFileNames, destinationFileNames);
                }
            }

            return newFileNames;
        }
    }
}
