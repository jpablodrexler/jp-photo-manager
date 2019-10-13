using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.ViewModels;
using log4net;
using log4net.Config;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Threading.Tasks;
using System.Windows;

namespace JPPhotoManager
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : System.Windows.Application
    {
        private readonly ServiceProvider serviceProvider;

        // TODO: Check the assembly version.
        // TODO: Add a global exception handler.
        
        public App()
        {
            var logRepository = LogManager.GetRepository(Assembly.GetEntryAssembly());
            XmlConfigurator.Configure(logRepository, new FileInfo("log4net.config"));

            var serviceCollection = new ServiceCollection();
            ConfigureServices(serviceCollection);
            this.serviceProvider = serviceCollection.BuildServiceProvider();
        }

        private void App_OnStartup(object sender, StartupEventArgs e)
        {
            var mainWindow = this.serviceProvider.GetService<MainWindow>();
            mainWindow.Show();
        }

        private void ConfigureServices(IServiceCollection services)
        {
            IConfigurationBuilder builder = new ConfigurationBuilder()
                .AddJsonFile("appsettings.json", optional: true, reloadOnChange: true);

            IConfigurationRoot configuration = builder.Build();

            services.AddSingleton(configuration);
            services.AddSingleton<IUserConfigurationService, UserConfigurationService>();
            services.AddSingleton<IStorageService, StorageService>();
            services.AddSingleton<IAssetRepository, AssetRepository>();
            services.AddSingleton<IAssetHashCalculatorService, AssetHashCalculatorService>();
            services.AddSingleton<ICatalogAssetsService, CatalogAssetsService>();
            services.AddSingleton<IFindDuplicatedAssetsService, FindDuplicatedAssetsService>();
            services.AddSingleton<IJPPhotoManagerApplication, JPPhotoManagerApplication>();
            services.AddSingleton<MainWindow>();
            services.AddSingleton<ApplicationViewModel>();
        }
    }
}
