using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Infrastructure.Services;
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
        public DbSet<RecentTargetPath> RecentTargetPaths { get; set; }

        /// <inheritdoc/>
        protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
        {
            // Overriding this method is needed to execute the EF commands.

            if (!optionsBuilder.IsConfigured)
            {
                IConfigurationBuilder builder = new ConfigurationBuilder()
                    .AddJsonFile("appsettings.json", optional: true, reloadOnChange: true);

                IConfigurationRoot configuration = builder.Build();

                var connectionString = configuration.GetConnectionString("SqliteConnection");
                connectionString = connectionString.Replace("{ApplicationData}", Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData));
                connectionString = connectionString.Replace("{FileFormat}", UserConfigurationService.FILE_FORMAT);
                connectionString = connectionString.Replace("\\", "/");
                optionsBuilder.UseSqlite(connectionString);
            }

            base.OnConfiguring(optionsBuilder);
        }
    }
}
