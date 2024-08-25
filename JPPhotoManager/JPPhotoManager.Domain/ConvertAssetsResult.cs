namespace JPPhotoManager.Domain
{
    public class ConvertAssetsResult
    {
        public string SourceDirectory { get; set; }
        public string DestinationDirectory { get; set; }
        public int ConvertedImages { get; set; }
        public string Message { get; set; }
    }
}
