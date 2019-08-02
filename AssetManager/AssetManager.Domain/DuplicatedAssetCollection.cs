﻿using System.Collections.Generic;

namespace AssetManager.Domain
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
                return this.Count > 0 ? $"{this[0].FileName} ({this.Count} duplicates)" : string.Empty;
            }
        }
    }
}
