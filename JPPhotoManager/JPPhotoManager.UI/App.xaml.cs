using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI.ViewModels;
using JPPhotoManager.UI.Windows;
using log4net;
using log4net.Config;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using System;
using System.Configuration;
using System.IO;
using System.Reflection;
using System.Windows;

namespace JPPhotoManager.UI
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    [ExcludeFromCodeCoverage]
    public partial class App : System.Windows.Application
    {
        private readonly ServiceProvider serviceProvider;
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        // TODO: Add a global exception handler.

        public App()
        {
            var logRepository = LogManager.GetRepository(Assembly.GetEntryAssembly());
            XmlConfigurator.Configure(logRepository, new FileInfo("log4net.config"));

            var serviceCollection = new ServiceCollection();
            ConfigureServices(serviceCollection);
            serviceProvider = serviceCollection.BuildServiceProvider();
        }

        private void App_OnStartup(object sender, StartupEventArgs e)
        {
            try
            {
                if (!serviceProvider.GetService<Application.IApplication>().IsAlreadyRunning())
                {
                    var context = serviceProvider.GetService<AppDbContext>();
                    context.Database.Migrate();

                    var mainWindow = serviceProvider.GetService<MainWindow>();
                    mainWindow.Show();
                }
                else
                {
                    Shutdown();
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
                MessageBox.Show("The application failed to initialize.", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                throw;
            }
        }

        private void ConfigureServices(IServiceCollection services)
        {
            IConfigurationBuilder builder = new ConfigurationBuilder()
                .AddJsonFile("appsettings.json", optional: true, reloadOnChange: true);

            IConfigurationRoot configuration = builder.Build();

            var connectionString = configuration.GetConnectionString("SqliteConnection");
            connectionString = connectionString.Replace("{ApplicationData}", Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData));
            connectionString = connectionString.Replace("\\", "/");

            services.AddSingleton(configuration);
            services.AddDbContext<AppDbContext>(options => options.UseSqlite(connectionString));
            services.AddSingleton<SyncLock>();
            services.AddSingleton<IDirectoryComparer, DirectoryComparer>();
            services.AddSingleton<IProcessService, ProcessService>();
            services.AddSingleton<IUserConfigurationService, UserConfigurationService>();
            services.AddSingleton<IStorageService, StorageService>();
            services.AddSingleton<IBatchRenameService, BatchRenameService>();
            services.AddSingleton<IFolderRepository, FolderRepository>();
            services.AddSingleton<IAssetRepository, AssetRepository>();
            services.AddSingleton<IRecentTargetPathRepository, RecentTargetPathRepository>();
            services.AddSingleton<IAssetHashCalculatorService, AssetHashCalculatorService>();
            services.AddSingleton<ICatalogAssetsService, CatalogAssetsService>();
            services.AddSingleton<IMoveAssetsService, MoveAssetsService>();
            services.AddSingleton<IFindDuplicatedAssetsService, FindDuplicatedAssetsService>();
            services.AddSingleton<ISyncAssetsService, SyncAssetsService>();
            services.AddSingleton<IReleaseAvailabilityService, GitHubReleaseAvailabilityService>();
            services.AddSingleton<INewReleaseNotificationService, NewReleaseNotificationService>();
            services.AddSingleton<Application.IApplication, Application.Application>();
            services.AddSingleton<MainWindow>();
            services.AddSingleton<ApplicationViewModel>();
        }
    }
}
