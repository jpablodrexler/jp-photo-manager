using JPPhotoManager.Domain;
using Microsoft.EntityFrameworkCore;

namespace JPPhotoManager.Infrastructure
{
    public class AppDbContext : DbContext
    {
        public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
        {
            
        }

        public DbSet<Asset> Assets { get; set; }
        public DbSet<Folder> Folders { get; set; }
    }
}
