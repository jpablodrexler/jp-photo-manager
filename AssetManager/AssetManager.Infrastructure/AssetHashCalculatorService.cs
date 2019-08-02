using AssetManager.Domain;
using System.Security.Cryptography;
using System.Text;

namespace AssetManager.Infrastructure
{
    public class AssetHashCalculatorService : IAssetHashCalculatorService
    {
        public string CalculateHash(byte[] imageBytes)
        {
            StringBuilder hashBuilder = new StringBuilder();
            byte[] hash = SHA512.Create().ComputeHash(imageBytes);

            foreach (byte hashByte in hash)
            {
                hashBuilder.Append(string.Format("{0:x2}", hashByte));
            }

            return hashBuilder.ToString();
        }
    }
}
