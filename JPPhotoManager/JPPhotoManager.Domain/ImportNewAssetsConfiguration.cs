using System.Collections.Generic;
using System.Linq;

namespace JPPhotoManager.Domain
{
    public class ImportNewAssetsConfiguration
    {
        public ImportNewAssetsConfiguration()
        {
            this.Imports = new List<ImportNewAssetsDirectoriesDefinition>();
        }

        public List<ImportNewAssetsDirectoriesDefinition> Imports { get; }

        public void Validate()
        {
            var validImports = this.Imports.Where(d => d.IsValid()).ToList();
            this.Imports.Clear();
            this.Imports.AddRange(validImports);
        }

        public void Normalize()
        {
            this.Imports.ForEach(d => d.Normalize());
        }
    }
}
