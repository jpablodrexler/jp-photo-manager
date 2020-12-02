using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Interfaces;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI.ViewModels;
using JPPhotoManager.UI.Windows;
using log4net;
using log4net.Config;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using SimplePortableDatabase;
using System;
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

            services.AddSingleton(configuration);
            services.AddSingleton<IDatabase, Database>();
            services.AddSingleton<IDirectoryComparer, DirectoryComparer>();
            services.AddSingleton<IProcessService, ProcessService>();
            services.AddSingleton<IUserConfigurationService, UserConfigurationService>();
            services.AddSingleton<IStorageService, StorageService>();
            services.AddSingleton<IBatchRenameService, BatchRenameService>();
            services.AddSingleton<IAssetRepository, AssetRepository>();
            services.AddSingleton<IAssetHashCalculatorService, AssetHashCalculatorService>();
            services.AddSingleton<ICatalogAssetsService, CatalogAssetsService>();
            services.AddSingleton<IMoveAssetsService, MoveAssetsService>();
            services.AddSingleton<IFindDuplicatedAssetsService, FindDuplicatedAssetsService>();
            services.AddSingleton<IRemoveDuplicatedAssetsService, RemoveDuplicatedAssetsService>();
            services.AddSingleton<IImportNewAssetsService, ImportNewAssetsService>();
            services.AddSingleton<IReleaseAvailabilityService, GitHubReleaseAvailabilityService>();
            services.AddSingleton<INewReleaseNotificationService, NewReleaseNotificationService>();
            services.AddSingleton<Application.IApplication, Application.Application>();
            services.AddSingleton<MainWindow>();
            services.AddSingleton<ApplicationViewModel>();
        }
    }
}
