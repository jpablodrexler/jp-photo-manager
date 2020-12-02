using System.Collections.Generic;

namespace JPPhotoManager.Domain
{
    public class DuplicatedAssetCollection : List<Asset>
    {
        public DuplicatedAssetCollection(List<Asset> assets) : base(assets)
        {
        }

        public string Description
        {
            get
            {
                return $"{this[0].FileName} ({this.Count} duplicates)";
            }
        }

        public bool HasDuplicates
        {
            get
            {
                return this.Count > 1;
            }
        }
    }
}
