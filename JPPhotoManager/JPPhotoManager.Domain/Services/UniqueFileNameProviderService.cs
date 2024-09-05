using JPPhotoManager.Domain.Interfaces.Services;
using System.IO;

namespace JPPhotoManager.Domain.Services
{
    public class UniqueFileNameProviderService : IUniqueFileNameProviderService
    {
        private readonly IStorageService _storageService;

        public UniqueFileNameProviderService(IStorageService storageService)
        {
            _storageService = storageService;
        }

        public string GetUniqueDestinationPath(string destinationDirectory, string destinationFileName)
        {
            string uniqueFileName = destinationFileName;
            int sequence = 0;
            bool exists = _storageService.FileExists(Path.Combine(destinationDirectory, destinationFileName));

            while (exists)
            {
                sequence++;
                uniqueFileName = Path.GetFileNameWithoutExtension(destinationFileName) + "_" + sequence + Path.GetExtension(destinationFileName);
                exists = _storageService.FileExists(Path.Combine(destinationDirectory, uniqueFileName));
            }

            return uniqueFileName;
        }
    }
}
