using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;

namespace JPPhotoManager.Infrastructure
{
    public class RecentTargetPathRepository : IRecentTargetPathRepository
    {
        private readonly AppDbContext _appDbContext;
        private object _syncLock;

        public RecentTargetPathRepository(AppDbContext appDbContext)
        {
            _appDbContext = appDbContext;
            _syncLock = new object();
        }

        public List<string> GetRecentTargetPaths()
        {
            return _appDbContext.RecentTargetPaths.Select(x => x.Path).ToList();
        }

        public void SaveRecentTargetPaths(List<string> recentTargetPaths)
        {
            lock (_syncLock)
            {
                var paths = recentTargetPaths.Select(x => new RecentTargetPath { Path = x });

                _appDbContext
                    .RecentTargetPaths
                    .RemoveRange(_appDbContext.RecentTargetPaths);

                _appDbContext
                    .RecentTargetPaths
                    .AddRange(paths);

                _appDbContext.SaveChanges();
            }
        }
    }
}
