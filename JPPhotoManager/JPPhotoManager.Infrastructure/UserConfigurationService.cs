using JPPhotoManager.Domain;
using Microsoft.Win32;
using System;
using System.Collections.Generic;
using System.Configuration;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;

namespace JPPhotoManager.Infrastructure
{
    public class UserConfigurationService : IUserConfigurationService
    {
        private const int SPI_SETDESKWALLPAPER = 20;
        private const int SPIF_UPDATEINIFILE = 0x01;
        private const int SPIF_SENDWININICHANGE = 0x02;
        private const string INITIAL_DIRECTORY_KEY = "InitialDirectory";
        private const string MY_PICTURES_VALUE = "{MyPictures}";
        private const string APPLICATION_DATA_DIRECTORY_KEY = "ApplicationDataDirectory";
        private const string APPLICATION_DATA_VALUE = "{ApplicationData}";
        private const string APPLICATION_NAME = "JPPhotoManager";

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        static extern int SystemParametersInfo(int uAction, int uParam, string lpvParam, int fuWinIni);

        public string GetPicturesDirectory()
        {
            return Environment.GetFolderPath(Environment.SpecialFolder.MyPictures);
        }

        public void SetAsWallpaper(Asset asset, WallpaperStyle style)
        {
            RegistryKey key = Registry.CurrentUser.OpenSubKey(@"Control Panel\Desktop", true);

            switch (style)
            {
                case WallpaperStyle.Fill:
                    key.SetValue(@"WallpaperStyle", "10");
                    key.SetValue(@"TileWallpaper", "0");
                    break;

                case WallpaperStyle.Fit:
                    key.SetValue(@"WallpaperStyle", "6");
                    key.SetValue(@"TileWallpaper", "0");
                    break;

                case WallpaperStyle.Stretch:
                    key.SetValue(@"WallpaperStyle", "2");
                    key.SetValue(@"TileWallpaper", "0");
                    break;

                case WallpaperStyle.Tile:
                    key.SetValue(@"WallpaperStyle", "0");
                    key.SetValue(@"TileWallpaper", "1");
                    break;

                case WallpaperStyle.Center:
                    key.SetValue(@"WallpaperStyle", "0");
                    key.SetValue(@"TileWallpaper", "0");
                    break;

                case WallpaperStyle.Span:
                    key.SetValue(@"WallpaperStyle", "22");
                    key.SetValue(@"TileWallpaper", "0");
                    break;
            }

            SystemParametersInfo(SPI_SETDESKWALLPAPER, 0, asset.FullPath, SPIF_UPDATEINIFILE | SPIF_SENDWININICHANGE);
        }

        public AboutInformation GetAboutInformation(Assembly assembly)
        {
            string product = null;
            string copyright = null;
            string version = "Version " + assembly.GetName().Version.ToString();
            var attrs = assembly.GetCustomAttributes(typeof(AssemblyProductAttribute));

            if (attrs.SingleOrDefault() is AssemblyProductAttribute assemblyProduct)
            {
                product = assemblyProduct.Product;
            }

            attrs = assembly.GetCustomAttributes(typeof(AssemblyCopyrightAttribute));

            if (attrs.SingleOrDefault() is AssemblyCopyrightAttribute assemblyCopyright)
            {
                copyright = assemblyCopyright.Copyright;
            }

            AboutInformation aboutInformation = new AboutInformation
            {
                Product = product,
                Author = copyright,
                Version = version
            };

            return aboutInformation;
        }

        public string GetInitialFolder()
        {
            string result = ConfigurationManager.AppSettings[INITIAL_DIRECTORY_KEY];

            if (string.IsNullOrEmpty(result))
            {
                result = Environment.GetFolderPath(Environment.SpecialFolder.MyPictures);
            }

            result = result.Replace(MY_PICTURES_VALUE, Environment.GetFolderPath(Environment.SpecialFolder.MyPictures));

            return result;
        }

        public string GetApplicationDataFolder()
        {
            string result = ConfigurationManager.AppSettings[APPLICATION_DATA_DIRECTORY_KEY];

            if (string.IsNullOrEmpty(result))
            {
                result = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), APPLICATION_NAME);
            }

            result = result.Replace(APPLICATION_DATA_VALUE, Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData));

            return result;
        }
    }
}
