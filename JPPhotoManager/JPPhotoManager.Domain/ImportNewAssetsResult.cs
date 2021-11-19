namespace JPPhotoManager.Domain
{
    public class ImportNewAssetsResult
    {
        public string SourceDirectory { get; set; }
        public string DestinationDirectory { get; set; }
        public int ImportedImages { get; set; }
        public string Message { get; set; }
    }
}
