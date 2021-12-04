namespace JPPhotoManager.Domain
{
    public class ImportNewAssetsConfiguration
    {
        public ImportNewAssetsConfiguration()
        {
            Imports = new List<ImportNewAssetsDirectoriesDefinition>();
        }

        public List<ImportNewAssetsDirectoriesDefinition> Imports { get; }

        public void Validate()
        {
            var validImports = Imports.Where(d => d.IsValid()).ToList();
            Imports.Clear();
            Imports.AddRange(validImports);
        }

        public void Normalize()
        {
            Imports.ForEach(d => d.Normalize());
        }
    }
}
