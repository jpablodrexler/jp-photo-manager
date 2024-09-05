using JPPhotoManager.Domain.Interfaces.Repositories;
using JPPhotoManager.Domain.Interfaces.Services;
using System.IO;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.Domain.Services
{
    public class ConvertAssetsService : IConvertAssetsService
    {
        private readonly IAssetRepository _assetRepository;
        private readonly IConvertAssetsConfigurationRepository _convertAssetsConfigurationRepository;
        private readonly IStorageService _storageService;
        private readonly IUniqueFileNameProviderService _uniqueFileNameProviderService;
        private readonly IDirectoryComparer _directoryComparer;

        public ConvertAssetsService(
            IAssetRepository assetRepository,
            IConvertAssetsConfigurationRepository convertAssetsConfigurationRepository,
            IStorageService storageService,
            IUniqueFileNameProviderService uniqueFileNameProviderService,
            IDirectoryComparer directoryComparer)
        {
            _assetRepository = assetRepository;
            _convertAssetsConfigurationRepository = convertAssetsConfigurationRepository;
            _storageService = storageService;
            _uniqueFileNameProviderService = uniqueFileNameProviderService;
            _directoryComparer = directoryComparer;
        }

        public async Task<List<ConvertAssetsResult>> ExecuteAsync(ProcessStatusChangedCallback callback)
        {
            return await Task.Run(() =>
            {
                List<ConvertAssetsResult> result = new();
                var configuration = _convertAssetsConfigurationRepository.GetConvertAssetsConfiguration();

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
            bool deleteSourceAssets,
            ProcessStatusChangedCallback callback,
            List<ConvertAssetsResult> resultList)
        {
            ConvertAssetsResult result = new()
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

                    string[] sourceFileNames = _storageService.GetFileNames(sourceDirectory, "*.png");
                    
                    foreach (string sourceFileName in sourceFileNames)
                    {
                        string sourcePath = Path.Combine(sourceDirectory, sourceFileName);
                        byte[] imageBytes = _storageService.GetFileBytes(sourcePath);
                        ushort? exifOrientation = _storageService.GetExifOrientation(imageBytes);
                        Rotation rotation = exifOrientation.HasValue ? _storageService.GetImageRotation(exifOrientation.Value) : Rotation.Rotate0;
                        BitmapImage originalImage = _storageService.LoadBitmapImage(imageBytes, rotation);

                        JpegBitmapEncoder encoder = new JpegBitmapEncoder();
                        encoder.QualityLevel = 100; // TODO: Could be parametrized.
                        encoder.Frames.Add(BitmapFrame.Create(originalImage));

                        string destinationFileName = Path.ChangeExtension(Path.GetFileName(sourceFileName), "jpg");
                        string destinationPath = _uniqueFileNameProviderService.GetUniqueDestinationPath(destinationDirectory, destinationFileName);

                        using (FileStream fileStream = new FileStream(destinationPath, FileMode.Create))
                        {
                            encoder.Save(fileStream);
                        }

                        if (deleteSourceAssets)
                        {
                            File.Delete(sourcePath);
                        }

                        result.ConvertedImages++;
                        callback(new ProcessStatusChangedCallbackEventArgs { NewStatus = $"'{sourcePath}' => '{destinationPath}'" });
                    }
                    
                    switch (result.ConvertedImages)
                    {
                        case 0:
                            result.Message = $"No images converted from '{sourceDirectory}' to '{destinationDirectory}'.";
                            break;

                        case 1:
                            result.Message = $"{result.ConvertedImages} image converted from '{sourceDirectory}' to '{destinationDirectory}'.";
                            break;

                        default:
                            result.Message = $"{result.ConvertedImages} images converted from '{sourceDirectory}' to '{destinationDirectory}'.";
                            break;
                    }

                    resultList.Add(result);

                    if (includeSubFolders)
                    {
                        List<DirectoryInfo> subdirectories = _storageService.GetSubDirectories(sourceDirectory);

                        if (subdirectories != null)
                        {
                            foreach (var subdir in subdirectories)
                            {
                                Execute(subdir.FullName,
                                    Path.Combine(destinationDirectory, subdir.Name),
                                    includeSubFolders,
                                    deleteSourceAssets,
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
    }
}
