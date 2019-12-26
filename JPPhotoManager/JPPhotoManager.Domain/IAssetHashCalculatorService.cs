namespace JPPhotoManager.Domain
{
    public interface IAssetHashCalculatorService
    {
        string CalculateHash(byte[] imageBytes);
    }
}
