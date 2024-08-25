namespace JPPhotoManager.Domain.Entities
{
    public class ConvertAssetsConfiguration
    {
        public ConvertAssetsConfiguration()
        {
            Definitions = new List<ConvertAssetsDirectoriesDefinition>();
        }

        public List<ConvertAssetsDirectoriesDefinition> Definitions { get; set; }

        public void Validate()
        {
            var validDefinitions = Definitions.Where(d => d.IsValid()).ToList();
            Definitions.Clear();
            Definitions.AddRange(validDefinitions);
        }

        public void Normalize()
        {
            Definitions.ForEach(d => d.Normalize());
        }
    }
}
