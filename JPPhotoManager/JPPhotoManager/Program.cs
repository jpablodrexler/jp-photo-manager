using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.ViewModels;
using log4net;
using SimpleInjector;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

namespace JPPhotoManager
{
    static class Program
    {
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        [STAThread]
        static void Main()
        {
            try
            {
                var container = new Container();
                container.RegisterSingleton<IUserConfigurationService, UserConfigurationService>();
                container.RegisterSingleton<IStorageService, StorageService>();
                container.RegisterSingleton<IAssetRepository, AssetRepository>();
                container.Register<IAssetHashCalculatorService, AssetHashCalculatorService>();
                container.Register<ICatalogAssetsService, CatalogAssetsService>();
                container.Register<IFindDuplicatedAssetsService, FindDuplicatedAssetsService>();
                container.Register<IJPPhotoManagerApplication, JPPhotoManagerApplication>();
                container.Register<MainWindow>();
                container.Register<ApplicationViewModel>();
                container.Verify();

                var app = new App();
                var mainWindow = container.GetInstance<MainWindow>();
                app.Run(mainWindow);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }
    }
}
