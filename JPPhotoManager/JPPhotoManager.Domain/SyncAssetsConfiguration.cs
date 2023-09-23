namespace JPPhotoManager.Domain
{
    public class SyncAssetsConfiguration
    {
        public SyncAssetsConfiguration()
        {
            Definitions = new List<SyncAssetsDirectoriesDefinition>();
        }

        public List<SyncAssetsDirectoriesDefinition> Definitions { get; set; }

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
