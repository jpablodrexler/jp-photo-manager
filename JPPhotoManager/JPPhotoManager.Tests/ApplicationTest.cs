using System;
using System.Collections.Generic;
using System.IO;
using CsvPortableDatabase;
using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Moq;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class ApplicationTest
    {
        private string dataDirectory;
        private string assetsDataFilePath;
        private string foldersDataFilePath;
        private string importsDataFilePath;
        private IConfigurationRoot configuration;

        public ApplicationTest()
        {
            dataDirectory = Path.GetDirectoryName(typeof(AssetRepositoryTest).Assembly.Location);
            dataDirectory = Path.Combine(dataDirectory, "TestFiles");
            assetsDataFilePath = Path.Combine(dataDirectory, $"asset.{Guid.NewGuid()}.db");
            foldersDataFilePath = Path.Combine(dataDirectory, $"folder.{Guid.NewGuid()}.db");
            importsDataFilePath = Path.Combine(dataDirectory, $"import.{Guid.NewGuid()}.db");

            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", dataDirectory)
                .MockGetValue("appsettings:ApplicationDataDirectory", dataDirectory)
                .MockGetValue("appsettings:CatalogBatchSize", "100")
                .MockGetValue("appsettings:AssetsDataFilePath", assetsDataFilePath)
                .MockGetValue("appsettings:FoldersDataFilePath", foldersDataFilePath)
                .MockGetValue("appsettings:ImportsDataFilePath", importsDataFilePath);

            configuration = configurationMock.Object;

            if (File.Exists(assetsDataFilePath))
            {
                File.Delete(assetsDataFilePath);
            }

            if (File.Exists(foldersDataFilePath))
            {
                File.Delete(foldersDataFilePath);
            }

            if (File.Exists(importsDataFilePath))
            {
                File.Delete(importsDataFilePath);
            }
        }

        [Fact]
        public void GetImagesTest()
        {
            string directory = @"D:\Imágenes";
            Asset[] expectedResult = new Asset[]
                {
                    new Asset
                    {
                        FileName = "dbzrrou-1d391dff-a336-4395-81a5-885a98685d93.jpg",
                        Folder = new Folder { Path = @"D:\Imágenes\" }
                    },
                    new Asset
                    {
                        FileName = "dbxb0an-d90d6335-7b9c-4a7b-84aa-71501c73f63b.jpg",
                        Folder = new Folder { Path = @"D:\Imágenes\" }
                    }
                };

            Mock<IAssetRepository> mockRepository = new Mock<IAssetRepository>();
            mockRepository.Setup(m => m.GetAssets(directory)).Returns(expectedResult);

            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                new CatalogAssetsService(
                    mockRepository.Object,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer()),
                new FindDuplicatedAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService)),
                mockRepository.Object,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));
            Asset[] assets = app.GetAssets(directory);
            assets.Should().BeEquivalentTo(expectedResult);

            mockRepository.VerifyAll();
        }

        [Fact]
        public void GetImagesOnEmptyStringTest()
        {
            string directory = string.Empty;
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            Mock<IAssetRepository> mockRepository = new Mock<IAssetRepository>();
            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                new CatalogAssetsService(
                    mockRepository.Object,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer()),
                new FindDuplicatedAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService)),
                mockRepository.Object,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            Func<Asset[]> function = () => app.GetAssets(directory);
            function.Should().Throw<ArgumentException>();
        }

        [Fact]
        public void GetImagesOnNullStringTest()
        {
            string directory = null;
            UserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            Mock<IAssetRepository> mockRepository = new Mock<IAssetRepository>();
            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    mockRepository.Object,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                new CatalogAssetsService(
                    mockRepository.Object,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer()),
                new FindDuplicatedAssetsService(
                    mockRepository.Object, new StorageService(userConfigurationService)),
                mockRepository.Object,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            Func<Asset[]> function = () => app.GetAssets(directory);
            function.Should().Throw<ArgumentException>();
        }

        [Fact]
        public void GetDuplicatedAssetsWithDuplicatesTest()
        {
            IDatabase database = new Database();
            database.Initialize(dataDirectory, ";");
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            Folder folder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    storageService,
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    storageService),
                repository,
                new UserConfigurationService(configuration),
                storageService);

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            Assert.True(File.Exists(imagePath));
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            Assert.True(File.Exists(imagePath));
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            Assert.True(File.Exists(imagePath));
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

            repository.SaveCatalog(folder);

            repository = new AssetRepository(database, storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    storageService,
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    storageService,
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    storageService),
                repository,
                new UserConfigurationService(configuration),
                storageService);

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            duplicatedAssetSets.Should().ContainSingle();

            DuplicatedAssetCollection duplicatedAssets = duplicatedAssetSets[0];
            duplicatedAssets.Should().HaveCount(2);
            duplicatedAssets[0].FileName.Should().Be("Image 2.jpg");
            duplicatedAssets[1].FileName.Should().Be("Image 2 duplicated.jpg");
            duplicatedAssets.Description.Should().Be("Image 2.jpg (2 duplicates)");

            repository.ContainsThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName).Should().BeTrue();
            repository.ContainsThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName).Should().BeTrue();
            repository.LoadThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName, duplicatedAssets[0].ThumbnailPixelWidth, duplicatedAssets[0].ThumbnailPixelHeight).Should().NotBeNull();
            repository.LoadThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName, duplicatedAssets[1].ThumbnailPixelWidth, duplicatedAssets[1].ThumbnailPixelHeight).Should().NotBeNull();
        }

        [Fact]
        public void GetDuplicatedAssetsWithoutDuplicatesTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            duplicatedAssetSets.Should().BeEmpty();
        }

        /// <summary>
        /// Tests an scenario when the user searches for duplicates before an
        /// old entry gets deleted from the catalog.
        /// </summary>
        [Fact]
        public void GetDuplicatedAssetsWithInexistingImageTest1()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
            File.Exists(imagePath).Should().BeFalse();

            Asset inexistentAsset = new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = asset.Folder,
                FolderId = asset.FolderId,
                Hash = asset.Hash
            };

            repository.AddAsset(inexistentAsset, null);
            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            duplicatedAssetSets.Should().BeEmpty();
        }

        /// <summary>
        /// Tests an scenario when the user searches for duplicates before an
        /// old entry gets deleted from the catalog.
        /// </summary>
        [Fact]
        public void GetDuplicatedAssetsWithInexistingImageTest2()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            Folder folder = repository.AddFolder(dataDirectory);

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
            File.Exists(imagePath).Should().BeFalse();

            repository.AddAsset(new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = folder,
                FolderId = folder.FolderId,
                Hash = "0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078"
            }, null);

            imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            duplicatedAssetSets.Should().BeEmpty();
        }

        [Fact]
        public void GetDuplicatedAssetsWithDuplicatesHashCollisionWithDuplicatedTest()
        {
            IDatabase database = new Database();
            database.Initialize(dataDirectory, ";");
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            Folder folder = repository.AddFolder(dataDirectory);

            Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();
            hashCalculator.Setup(h => h.CalculateHash(It.IsAny<byte[]>())).Returns("abcd1234");

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    hashCalculator.Object,
                    storageService,
                    userConfigurationService,
                    directoryComparer);

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    storageService,
                    directoryComparer),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    storageService),
                repository,
                userConfigurationService,
                storageService);

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

            repository.SaveCatalog(folder);

            repository = new AssetRepository(database, storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            duplicatedAssetSets.Should().ContainSingle();

            List<Asset> duplicatedAssets = duplicatedAssetSets[0];
            duplicatedAssets.Should().HaveCount(2);
            duplicatedAssets[0].FileName.Should().Be("Image 2.jpg");
            duplicatedAssets[1].FileName.Should().Be("Image 2 duplicated.jpg");

            repository.ContainsThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName).Should().BeTrue();
            repository.ContainsThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName).Should().BeTrue();
            repository.LoadThumbnail(duplicatedAssets[0].Folder.Path, duplicatedAssets[0].FileName, duplicatedAssets[0].ThumbnailPixelWidth, duplicatedAssets[0].ThumbnailPixelHeight).Should().NotBeNull();
            repository.LoadThumbnail(duplicatedAssets[1].Folder.Path, duplicatedAssets[1].FileName, duplicatedAssets[1].ThumbnailPixelWidth, duplicatedAssets[1].ThumbnailPixelHeight).Should().NotBeNull();
        }

        [Fact]
        public void GetDuplicatedAssetsWithDuplicatesHashCollisionWithNoDuplicatedTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();
            hashCalculator.Setup(h => h.CalculateHash(It.IsAny<byte[]>())).Returns("abcd1234");

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    hashCalculator.Object,
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            duplicatedAssetSets.Should().BeEmpty();
        }

        [Fact]
        public void AddAssetsToNonExistingFolderAddsFolderToCatalogTest()
        {
            IDatabase database = new Database();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            Folder folder = new Folder { FolderId = "1", Path = "C:\\Inexistent Folder" };

            string imagePath = Path.Combine(dataDirectory, "Inexistent Image.jpg");
            File.Exists(imagePath).Should().BeFalse();

            repository.AddAsset(new Asset
            {
                FileName = "Inexistent Image.jpg",
                Folder = folder,
                FolderId = folder.FolderId,
                Hash = "0b6d010f85544871c307bb3a96028402f55fa29094908cdd0f74a8ec8d3fc3d4fbec995d98b89aafef3dcf5581c018fbb50481e33c7e45aef552d66c922f4078"
            }, null);

            folder = repository.GetFolderByPath("C:\\Inexistent Folder");
            folder.Should().NotBeNull();
            folder.Path.Should().Be("C:\\Inexistent Folder");
            folder.Name.Should().Be("Inexistent Folder");
        }

        [Fact]
        public void GetDuplicatedAssetsWithDuplicatesThumbnailNotFoundTest()
        {
            IDatabase database = new Database();
            database.Initialize(dataDirectory, ";");
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            UnencapsulatedAssetRepository repository = new UnencapsulatedAssetRepository(database, storageService, userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            Folder folder = repository.AddFolder(dataDirectory);

            Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();
            hashCalculator.Setup(h => h.CalculateHash(It.IsAny<byte[]>())).Returns("abcd1234");

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    hashCalculator.Object,
                    storageService,
                    userConfigurationService,
                    directoryComparer);

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    storageService,
                    directoryComparer),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    storageService),
                repository,
                userConfigurationService,
                storageService);

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

            repository.SaveCatalog(folder);
            repository.RemoveThumbnail(folder.Path, "Image 2 duplicated.jpg");
            repository.SaveCatalog(folder);

            repository = new UnencapsulatedAssetRepository(database, storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            List<DuplicatedAssetCollection> duplicatedAssetSets = app.GetDuplicatedAssets();
            duplicatedAssetSets.Should().BeEmpty();

            repository.GetAssets(dataDirectory).Should().Contain(a => a.FileName == "Image 2.jpg");
            repository.GetAssets(dataDirectory).Should().NotContain(a => a.FileName == "Image 2 duplicated.jpg");
            repository.ContainsThumbnail(dataDirectory, "Image 2.jpg").Should().BeTrue();
            repository.ContainsThumbnail(dataDirectory, "Image 2 duplicated.jpg").Should().BeFalse();
            repository.LoadThumbnail(dataDirectory, asset.FileName, asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight).Should().NotBeNull();
            repository.LoadThumbnail(dataDirectory, duplicatedAsset.FileName, duplicatedAsset.ThumbnailPixelWidth, duplicatedAsset.ThumbnailPixelHeight).Should().BeNull();
        }

        [Fact]
        public void GetAssetsWithThumbnailNotFoundTest()
        {
            IDatabase database = new Database();
            database.Initialize(dataDirectory, ";");
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            UnencapsulatedAssetRepository repository = new UnencapsulatedAssetRepository(database, storageService, userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            Folder folder = repository.AddFolder(dataDirectory);
            Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();

            CatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                    repository,
                    hashCalculator.Object,
                    storageService,
                    userConfigurationService,
                    directoryComparer);

            Application.Application app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    storageService,
                    directoryComparer),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    storageService),
                repository,
                userConfigurationService,
                storageService);

            string imagePath = Path.Combine(dataDirectory, "Image 2.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset asset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 1.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset anotherAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 1.jpg");

            imagePath = Path.Combine(dataDirectory, "Image 2 duplicated.jpg");
            File.Exists(imagePath).Should().BeTrue();
            Asset duplicatedAsset = catalogAssetsService.CreateAsset(dataDirectory, "Image 2 duplicated.jpg");

            repository.SaveCatalog(folder);
            repository.RemoveThumbnail(folder.Path, "Image 2 duplicated.jpg");
            repository.SaveCatalog(folder);

            repository = new UnencapsulatedAssetRepository(database, storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            catalogAssetsService = new CatalogAssetsService(
                    repository,
                    new AssetHashCalculatorService(),
                    new StorageService(userConfigurationService),
                    new UserConfigurationService(configuration),
                    new DirectoryComparer());

            app = new Application.Application(
                new ImportNewAssetsService(
                    repository,
                    new StorageService(userConfigurationService),
                    new DirectoryComparer()),
                catalogAssetsService,
                new FindDuplicatedAssetsService(
                    repository,
                    new StorageService(userConfigurationService)),
                repository,
                new UserConfigurationService(configuration),
                new StorageService(userConfigurationService));

            Asset[] assets = app.GetAssets(dataDirectory);
            assets.Should().NotBeEmpty();

            repository.GetAssets(dataDirectory).Should().Contain(a => a.FileName == "Image 2.jpg");
            repository.GetAssets(dataDirectory).Should().NotContain(a => a.FileName == "Image 2 duplicated.jpg");
            repository.ContainsThumbnail(dataDirectory, "Image 2.jpg").Should().BeTrue();
            repository.ContainsThumbnail(dataDirectory, "Image 2 duplicated.jpg").Should().BeFalse();
            repository.LoadThumbnail(dataDirectory, asset.FileName, asset.ThumbnailPixelWidth, asset.ThumbnailPixelHeight).Should().NotBeNull();
            repository.LoadThumbnail(dataDirectory, duplicatedAsset.FileName, duplicatedAsset.ThumbnailPixelWidth, duplicatedAsset.ThumbnailPixelHeight).Should().BeNull();
        }
    }

    class UnencapsulatedAssetRepository : AssetRepository
    {
        internal UnencapsulatedAssetRepository(IDatabase database, IStorageService storageService, IUserConfigurationService userConfigurationService) : base(database, storageService, userConfigurationService)
        {

        }

        internal void RemoveThumbnail(string directoryName, string fileName)
        {
            if (Thumbnails.ContainsKey(directoryName) && Thumbnails[directoryName].ContainsKey(fileName))
            {
                Thumbnails[directoryName].Remove(fileName);
            }
        }
    }
}
