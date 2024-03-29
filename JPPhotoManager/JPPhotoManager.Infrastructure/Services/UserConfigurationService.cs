﻿using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Domain.Interfaces.Services;
using Microsoft.Extensions.Configuration;
using Microsoft.Win32;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Runtime.InteropServices;

namespace JPPhotoManager.Infrastructure.Services
{
    public class UserConfigurationService : IUserConfigurationService
    {
        private const int SPI_SETDESKWALLPAPER = 20;
        private const int SPIF_UPDATEINIFILE = 0x01;
        private const int SPIF_SENDWININICHANGE = 0x02;
        private const string INITIAL_DIRECTORY_KEY = "appsettings:InitialDirectory";
        private const string ADDITIONAL_PHOTOS_DIRECTORIES_KEY = "appsettings:AdditionalPhotosDirectories";
        private const string MY_PICTURES_VALUE = "{MyPictures}";
        private const string APPLICATION_DATA_DIRECTORY_KEY = "appsettings:ApplicationDataDirectory";
        private const string APPLICATION_DATA_PLACEHOLDER = "{ApplicationData}";
        private const string CATALOG_BATCH_SIZE_KEY = "appsettings:CatalogBatchSize";
        private const string CATALOG_COOLDOWN_MINUTES = "appsettings:CatalogCooldownMinutes";
        private const string THUMBNAILS_DICTIONARY_ENTRIES_TO_KEEP = "appsettings:ThumbnailsDictionaryEntriesToKeep";
        private const string APPLICATION_NAME = "JPPhotoManager";
        private const string REPOSITORY_OWNER = "appsettings:Repository:Owner";
        private const string REPOSITORY_NAME = "appsettings:Repository:Name";
        private const string FILE_FORMAT_PLACEHOLDER = "{FileFormat}";
        public const string FILE_FORMAT = "v2";

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        static extern int SystemParametersInfo(int uAction, int uParam, string lpvParam, int fuWinIni);

        private readonly IConfigurationRoot _configuration;

        public UserConfigurationService(IConfigurationRoot configuration)
        {
            _configuration = configuration;
        }

        public string GetAppFilesDirectory() => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), GetApplicationDataFolder());

        public string GetBinaryFilesDirectory() => Path.Combine(GetAppFilesDirectory(), "Thumbnails");

        public string GetDatabaseDirectory() => GetAppFilesDirectory();

        public string GetPicturesDirectory()
        {
            return Environment.GetFolderPath(Environment.SpecialFolder.MyPictures);
        }

        public string GetOneDriveDirectory()
        {
            return Environment.GetEnvironmentVariable("OneDrive", EnvironmentVariableTarget.User);
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
            string version = "v" + GetProductVersion();
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

            AboutInformation aboutInformation = new()
            {
                Product = product,
                Author = copyright,
                Version = version
            };

            return aboutInformation;
        }

        private string GetProductVersion()
        {
            FileVersionInfo fileVersionInfo = FileVersionInfo.GetVersionInfo(GetType().Assembly.Location);

            return fileVersionInfo.ProductVersion;
        }

        public string GetInitialFolder()
        {
            string result = _configuration.GetValue<string>(INITIAL_DIRECTORY_KEY);

            if (string.IsNullOrEmpty(result))
            {
                result = Environment.GetFolderPath(Environment.SpecialFolder.MyPictures);
            }

            result = result.Replace(MY_PICTURES_VALUE, Environment.GetFolderPath(Environment.SpecialFolder.MyPictures));

            return result;
        }

        public string[] AdditionalPhotosDirectories()
        {
            string[] result;
            string additionalPicturesDirectoriesValue = _configuration.GetValue<string>(ADDITIONAL_PHOTOS_DIRECTORIES_KEY);

            if (string.IsNullOrWhiteSpace(additionalPicturesDirectoriesValue))
                return Array.Empty<string>();

            additionalPicturesDirectoriesValue = additionalPicturesDirectoriesValue.Replace(MY_PICTURES_VALUE, Environment.GetFolderPath(Environment.SpecialFolder.MyPictures));
            result = additionalPicturesDirectoriesValue.Split(';');

            return result;
        }

        public string GetApplicationDataFolder()
        {
            string result = _configuration.GetValue<string>(APPLICATION_DATA_DIRECTORY_KEY);

            if (string.IsNullOrEmpty(result))
            {
                result = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), APPLICATION_NAME);
            }

            result = result.Replace(APPLICATION_DATA_PLACEHOLDER, Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData));
            result = result.Replace(FILE_FORMAT_PLACEHOLDER, FILE_FORMAT);

            return result;
        }

        public int GetCatalogBatchSize()
        {
            return _configuration.GetValue<int>(CATALOG_BATCH_SIZE_KEY);
        }

        public int GetCatalogCooldownMinutes()
        {
            return _configuration.GetValue<int>(CATALOG_COOLDOWN_MINUTES);
        }

        public int GetThumbnailsDictionaryEntriesToKeep()
        {
            return _configuration.GetValue<int>(THUMBNAILS_DICTIONARY_ENTRIES_TO_KEEP);
        }

        public string GetRepositoryOwner()
        {
            return _configuration.GetValue<string>(REPOSITORY_OWNER);
        }

        public string GetRepositoryName()
        {
            return _configuration.GetValue<string>(REPOSITORY_NAME);
        }

        public string[] GetRootCatalogFolderPaths()
        {
            // TODO: Validate if some of the root folders are not valid or don't exist any longer.
            List<string> rootPaths = new()
            {
                GetOneDriveDirectory(),
                GetPicturesDirectory()
            };

            rootPaths.AddRange(AdditionalPhotosDirectories());

            return rootPaths.ToArray();
        }
    }
}
