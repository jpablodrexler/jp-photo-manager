namespace JPPhotoManager.Domain
{
    public class SyncAssetsResult
    {
        public string SourceDirectory { get; set; }
        public string DestinationDirectory { get; set; }
        public int SyncedImages { get; set; }
        public string Message { get; set; }
    }
}
