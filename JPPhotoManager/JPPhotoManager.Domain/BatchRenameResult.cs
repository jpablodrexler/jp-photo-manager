namespace JPPhotoManager.Domain
{
    public class BatchRenameResult
    {
        public BatchRenameResult()
        {
            SourceAssets = new List<Asset>();
            TargetFileNames = new List<string>();
        }

        public List<Asset> SourceAssets { get; set; }
        public List<string> TargetFileNames { get; set; }
    }
}
