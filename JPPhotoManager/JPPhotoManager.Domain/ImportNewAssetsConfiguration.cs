using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

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
            Imports = Imports.Where(d => d.IsValid()).ToList();
        }

        public void Normalize()
        {
            Imports.ForEach(d => d.Normalize());
        }
    }
}
