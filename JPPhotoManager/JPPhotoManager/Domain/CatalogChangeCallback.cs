using System;
using System.Collections.Generic;

namespace JPPhotoManager.Domain
{
    public delegate void CatalogChangeCallback(CatalogChangeCallbackEventArgs e);

    public class CatalogChangeCallbackEventArgs
    {
        public Asset Asset { get; set; }
        public List<Asset> CataloguedAssets { get; set; }
        public ReasonEnum Reason { get; set; }
        public string Message { get; set; }
        public Exception Exception { get; set; }
    }
}
