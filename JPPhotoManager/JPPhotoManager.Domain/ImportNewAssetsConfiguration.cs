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

        public List<ImportNewAssetsDirectoriesDefinition> Imports { get; set; }

        public void Validate()
        {
            this.Imports = this.Imports.Where(d => d.IsValid()).ToList();
        }

        public void Normalize()
        {
            this.Imports.ForEach(d => d.Normalize());
        }
    }
}
