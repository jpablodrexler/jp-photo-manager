namespace JPPhotoManager.Domain.Interfaces.Services
{
    public interface IAssetHashCalculatorService
    {
        string CalculateHash(byte[] imageBytes);
    }
}
