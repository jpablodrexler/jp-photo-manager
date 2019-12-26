﻿using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Moq;
using System;
using System.Collections.Generic;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class AssetRepositoryTest
    {
        private string dataDirectory;
        private string assetDataSetPath;
        private IConfigurationRoot configuration;

        public AssetRepositoryTest()
        {
            dataDirectory = Path.GetDirectoryName(typeof(AssetRepositoryTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");
            assetDataSetPath = Path.Combine(dataDirectory, $"AssetCatalog{Guid.NewGuid()}.json");

            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", dataDirectory)
                .MockGetValue("appsettings:CatalogBatchSize", "100");
            
            configuration = configurationMock.Object;

            if (File.Exists(assetDataSetPath))
            {
                File.Delete(assetDataSetPath);
            }
        }

        [Fact]
        public void FolderExistsTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            bool folderExists = repository.FolderExists(dataDirectory);
            Assert.False(folderExists);
            repository.AddFolder(dataDirectory);
            folderExists = repository.FolderExists(dataDirectory);
            Assert.True(folderExists);
        }

        [Fact]
        public void HasChangesInitiallyFalseTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));

            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            Assert.False(repository.HasChanges());
        }

        [Fact]
        public void HasChangesTrueAfterChangeTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));

            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");
            Assert.True(repository.HasChanges());
        }

        [Fact]
        public void HasChangesFalseAfterSaveTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));

            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");
            repository.SaveCatalog(null);
            Assert.False(repository.HasChanges());
        }

        [Fact]
        public void IsAssetCataloguedImageInCatalogTest()
        {
            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));

            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            bool isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Image 2.jpg");
            Assert.False(isCatalogued);
            repository.AddFolder(dataDirectory);
            catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");
            isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Image 2.jpg");
            Assert.True(isCatalogued);
        }

        [Fact]
        public void DeleteNonExistingAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            string imagePath = Path.Combine(dataDirectory, "Non Existing Image.jpg");
            Assert.False(File.Exists(imagePath));
            
            repository.DeleteAsset(dataDirectory, "Non Existing Image.jpg");
            bool isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Non Existing Image.jpg");
            Assert.False(isCatalogued);
            
            repository.DeleteAsset(dataDirectory, "Non Existing Image.jpg");
            isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Non Existing Image.jpg");
            Assert.False(isCatalogued);
        }

        [Fact]
        public void DeleteExistingAssetTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            string imagePath = Path.Combine(dataDirectory, "Image 3.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 3.jpg");

            // The asset should no longer be catalogued, but the image should still be in the filesystem.
            repository.DeleteAsset(dataDirectory, "Image 3.jpg");
            bool isCatalogued = repository.IsAssetCatalogued(dataDirectory, "Image 3.jpg");
            Assert.False(isCatalogued);
            Assert.True(File.Exists(imagePath));
        }

        [Fact]
        public void SaveAndGetImportNewAssetsConfigurationTest()
        {
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            AssetRepository repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);

            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration
            {
                Imports = new List<ImportNewAssetsDirectoriesDefinition>
                {
                    new ImportNewAssetsDirectoriesDefinition
                    {
                        SourceDirectory = @"C:\MyFirstGame\Screenshots",
                        DestinationDirectory = @"C:\Images\MyFirstGame"
                    },
                    new ImportNewAssetsDirectoriesDefinition
                    {
                        SourceDirectory = @"C:\MySecondGame\Screenshots",
                        DestinationDirectory = @"C:\Images\MySecondGame"
                    }
                }
            };

            repository.SetImportNewAssetsConfiguration(importConfiguration);
            repository.SaveCatalog(null);

            repository = new AssetRepository(new StorageService(userConfigurationService));
            repository.Initialize(this.assetDataSetPath);
            importConfiguration = repository.GetImportNewAssetsConfiguration();

            Assert.Equal(2, importConfiguration.Imports.Count);
            Assert.Equal(@"C:\MyFirstGame\Screenshots", importConfiguration.Imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyFirstGame", importConfiguration.Imports[0].DestinationDirectory);
            Assert.Equal(@"C:\MySecondGame\Screenshots", importConfiguration.Imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MySecondGame", importConfiguration.Imports[1].DestinationDirectory);
        }
    }
}