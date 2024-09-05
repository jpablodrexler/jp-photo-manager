namespace JPPhotoManager.Domain.Interfaces.Services
{
    public interface IUniqueFileNameProviderService
    {
        string GetUniqueDestinationPath(string destinationDirectory, string destinationFileName);
    }
}
