namespace JPPhotoManager.Domain
{
    public class Release
    {
        public string Name { get; set; }
        public DateTimeOffset? PublishedOn { get; set; }
        public bool IsNewRelease { get; set; }
        public bool Success { get; set; }
        public string DownloadUrl { get; set; }
    }
}
