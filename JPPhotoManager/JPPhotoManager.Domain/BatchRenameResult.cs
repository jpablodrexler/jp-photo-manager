using JPPhotoManager.Domain.Entities;

namespace JPPhotoManager.Domain
{
    public class BatchRenameResult
    {
        public BatchRenameResult()
        {
            SourceAssets = new List<Asset>();
            TargetPaths = new List<string>();
        }

        public List<Asset> SourceAssets { get; set; }
        public List<string> TargetPaths { get; set; }
    }
}
