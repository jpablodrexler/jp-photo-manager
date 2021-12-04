namespace JPPhotoManager.Domain
{
    public delegate void StatusChangeCallback(StatusChangeCallbackEventArgs e);
    public delegate void CatalogChangeCallback(CatalogChangeCallbackEventArgs e);

    public class StatusChangeCallbackEventArgs
    {
        public string NewStatus { get; set; }
    }

    public class CatalogChangeCallbackEventArgs
    {
        public Asset Asset { get; set; }
        public Folder Folder { get; set; }
        public List<Asset> CataloguedAssets { get; set; }
        public ReasonEnum Reason { get; set; }
        public string Message { get; set; }
        public Exception Exception { get; set; }
    }
}
