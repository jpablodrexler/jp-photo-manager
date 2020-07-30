using CsvPortableDatabase;
using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Moq;
using System;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class AssetRepositoryTest
    {
        private string dataDirectory;
        private IConfigurationRoot configuration;

        public AssetRepositoryTest()
        {
            dataDirectory = Path.GetDirectoryName(typeof(AssetRepositoryTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");

            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", Path.Combine(dataDirectory, Guid.NewGuid().ToString()))
                .MockGetValue("appsettings:CatalogBatchSize", "100");
                
            configuration = configurationMock.Object;
        }

        [Fact]
        public void FolderExistsTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IAssetRepository repository = new AssetRepository(new Database(), new StorageService(userConfigurationService), userConfigurationService);
            bool folderExists = repository.FolderExists(dataDirectory);
            folderExists.Should().BeFalse();
            repository.AddFolder(dataDirectory);
            folderExists = repository.FolderExists(dataDirectory);
            folderExists.Should().BeTrue();
        }

        [Fact]
        public void HasChangesInitiallyFalseTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();

            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IAssetRepository repository = new AssetRepository(new Database(), new StorageService(userConfigurationService), userConfigurationService);
            repository.HasChanges().Should().BeFalse();
        }

        [Fact]
        public void HasChangesTrueAfterChangeTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();

            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IAssetRepository repository = new AssetRepository(new Database(), new StorageService(userConfigurationService), userConfigurationService);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");
            repository.HasChanges().Should().BeTrue();
        }

        [Fact]
        public void HasChangesFalseAfterSaveTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();

            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IAssetRepository repository = new AssetRepository(new Database(), new StorageService(userConfigurationService), userConfigurationService);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");
            repository.SaveCatalog(null);
            repository.HasChanges().Should().BeFalse();
        }

        [Fact]
        public void IsAssetCataloguedImageInCatalogTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();

            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IAssetRepository repository = new AssetRepository(new Database(), new StorageService(userConfigurationService), userConfigurationService);
            
            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            repository.IsAssetCatalogued(dataDirectory, "Image 2.jpg").Should().BeFalse();
            repository.AddFolder(dataDirectory);
            catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");
            repository.IsAssetCatalogued(dataDirectory, "Image 2.jpg").Should().BeTrue();
        }

        [Fact]
        public void DeleteNonExistingAssetTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IAssetRepository repository = new AssetRepository(new Database(), new StorageService(userConfigurationService), userConfigurationService);
            repository.AddFolder(dataDirectory);

            string imagePath = Path.Combine(dataDirectory, "Non Existing Image.jpg");
            File.Exists(imagePath).Should().BeFalse();
            
            repository.DeleteAsset(dataDirectory, "Non Existing Image.jpg");
            repository.IsAssetCatalogued(dataDirectory, "Non Existing Image.jpg").Should().BeFalse();
            
            repository.DeleteAsset(dataDirectory, "Non Existing Image.jpg");
            repository.IsAssetCatalogued(dataDirectory, "Non Existing Image.jpg").Should().BeFalse();
        }

        [Fact]
        public void DeleteExistingAssetTest()
        {
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IAssetRepository repository = new AssetRepository(new Database(), new StorageService(userConfigurationService), userConfigurationService);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            string imagePath = Path.Combine(dataDirectory, "Image 3.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 3.jpg");

            // The asset should no longer be catalogued, but the image should still be in the filesystem.
            repository.DeleteAsset(dataDirectory, "Image 3.jpg");
            repository.IsAssetCatalogued(dataDirectory, "Image 3.jpg").Should().BeFalse();
            File.Exists(imagePath).Should().BeTrue();
        }

        [Fact]
        public void SaveAndGetImportNewAssetsConfigurationTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IAssetRepository repository = new AssetRepository(database, new StorageService(userConfigurationService), userConfigurationService);
            
            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration();

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyFirstGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MyFirstGame"
                });

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MySecondGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MySecondGame"
                });
            
            repository.SetImportNewAssetsConfiguration(importConfiguration);
            repository.SaveCatalog(null);

            importConfiguration = repository.GetImportNewAssetsConfiguration();
            repository = new AssetRepository(new Database(), new StorageService(userConfigurationService), userConfigurationService);
            
            importConfiguration.Imports.Should().HaveCount(2);
            importConfiguration.Imports[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            importConfiguration.Imports[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            importConfiguration.Imports[1].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            importConfiguration.Imports[1].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
        }
    }
}
