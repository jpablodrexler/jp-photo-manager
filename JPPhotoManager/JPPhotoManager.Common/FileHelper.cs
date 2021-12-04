using System.IO;
using System.Text.Json;

namespace JPPhotoManager.Common
{
    public static class FileHelper
    {
        public static T ReadObjectFromJsonFile<T>(string jsonFilePath)
        {
            T result = default(T);
            string json;

            if (File.Exists(jsonFilePath))
            {
                using (StreamReader reader = new(jsonFilePath))
                {
                    json = reader.ReadToEnd();
                }

                result = JsonSerializer.Deserialize<T>(json);
            }

            return result;
        }

        public static void WriteObjectToJsonFile(object anObject, string jsonFilePath)
        {
            string json = JsonSerializer.Serialize(anObject, new JsonSerializerOptions { WriteIndented = true });

            using (StreamWriter writer = new(jsonFilePath, false))
            {
                writer.Write(json);
            }
        }
    }
}
