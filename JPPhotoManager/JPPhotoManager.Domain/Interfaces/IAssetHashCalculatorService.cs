namespace JPPhotoManager.Domain.Interfaces
{
    public interface IAssetHashCalculatorService
    {
        string CalculateHash(byte[] imageBytes);
    }
}
