namespace JPPhotoManager.Domain.Interfaces
{
    public interface IRecentTargetPathRepository
    {
        List<string> GetRecentTargetPaths();
        void SaveRecentTargetPaths(List<string> recentTargetPaths);
    }
}
