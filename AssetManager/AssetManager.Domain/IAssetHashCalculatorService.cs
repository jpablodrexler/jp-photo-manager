namespace AssetManager.Domain
{
    public interface IAssetHashCalculatorService
    {
        string CalculateHash(byte[] imageBytes);
    }
}
