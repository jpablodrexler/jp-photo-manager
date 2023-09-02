using JPPhotoManager.Domain;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;

namespace JPPhotoManager.Infrastructure
{
    public class AppDbContext : DbContext
    {
        public AppDbContext() : base()
        {

        }

        public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
        {
            
        }

        public DbSet<Asset> Assets { get; set; }
        public DbSet<Folder> Folders { get; set; }
        public DbSet<SyncAssetsDirectoriesDefinition> SyncAssetsDirectoriesDefinitions { get; set; }

        // Overriding this method is needed to execute the EF commands.
        protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
        {
            IConfigurationBuilder builder = new ConfigurationBuilder()
                .AddJsonFile("appsettings.json", optional: true, reloadOnChange: true);

            IConfigurationRoot configuration = builder.Build();
            optionsBuilder.UseSqlite(configuration.GetConnectionString("SqliteConnection"));
            base.OnConfiguring(optionsBuilder);
        }
    }
}
