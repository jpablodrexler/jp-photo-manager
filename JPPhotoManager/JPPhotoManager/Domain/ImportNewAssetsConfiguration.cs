using System;
using System.Collections.Generic;
using System.Text;

namespace JPPhotoManager.Domain
{
    public class ImportNewAssetsDirectoriesDefinition
    {
        public string SourceDirectory { get; set; }
        public string DestinationDirectory { get; set; }
    }

    public class ImportNewAssetsConfiguration
    {
        public ImportNewAssetsConfiguration()
        {
            this.Imports = new List<ImportNewAssetsDirectoriesDefinition>();
        }

        public List<ImportNewAssetsDirectoriesDefinition> Imports { get; set; }
    }
}
