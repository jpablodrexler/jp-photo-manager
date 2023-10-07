namespace JPPhotoManager.Domain.Interfaces.Repositories
{
    public interface IRecentTargetPathRepository
    {
        List<string> GetRecentTargetPaths();
        void SaveRecentTargetPaths(List<string> recentTargetPaths);
    }
}
