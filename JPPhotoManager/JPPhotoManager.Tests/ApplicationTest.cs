using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using Microsoft.Extensions.Configuration;
using Moq;
using SimplePortableDatabase;
using System;
using System.Collections.Generic;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class ApplicationTest
    {
        private string dataDirectory;
        private IConfigurationRoot configuration;

        public ApplicationTest()
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

            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);

            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            IStorageService storageService = new StorageService(userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            IFindDuplicatedAssetsService findDuplicatedAssetsService = new FindDuplicatedAssetsService(
                mockRepository.Object,
                storageService);
            IImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                mockRepository.Object,
                storageService,
                directoryComparer);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                mockRepository.Object,
                assetHashCalculatorService,
                storageService,
                userConfigurationService,
                directoryComparer);
            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                mockRepository.Object,
                storageService,
                catalogAssetsService);
            Application.Application app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
                mockRepository.Object,
                userConfigurationService,
                storageService);
            Asset[] assets = app.GetAssets(directory);
            assets.Should().BeEquivalentTo(expectedResult);

            mockRepository.VerifyAll();
        }

        [Fact]
        public void GetImagesOnEmptyStringTest()
        {
            string directory = string.Empty;
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            Mock<IAssetRepository> mockRepository = new Mock<IAssetRepository>();
            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            IStorageService storageService = new StorageService(userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            IFindDuplicatedAssetsService findDuplicatedAssetsService = new FindDuplicatedAssetsService(
                mockRepository.Object,
                storageService);
            IImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                mockRepository.Object,
                storageService,
                directoryComparer);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                mockRepository.Object,
                assetHashCalculatorService,
                storageService,
                userConfigurationService,
                directoryComparer);
            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                mockRepository.Object,
                storageService,
                catalogAssetsService);
            Application.Application app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
                mockRepository.Object,
                userConfigurationService,
                storageService);

            Func<Asset[]> function = () => app.GetAssets(directory);
            function.Should().Throw<ArgumentException>();
        }

        [Fact]
        public void GetImagesOnNullStringTest()
        {
            string directory = null;
            Mock<IAssetRepository> mockRepository = new Mock<IAssetRepository>();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            IStorageService storageService = new StorageService(userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            IFindDuplicatedAssetsService findDuplicatedAssetsService = new FindDuplicatedAssetsService(
                mockRepository.Object,
                storageService);
            IImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                mockRepository.Object,
                storageService,
                directoryComparer);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                mockRepository.Object,
                assetHashCalculatorService,
                storageService,
                userConfigurationService,
                directoryComparer);
            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                mockRepository.Object,
                storageService,
                catalogAssetsService);
            Application.Application app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
                mockRepository.Object,
                userConfigurationService,
                storageService);

            Func<Asset[]> function = () => app.GetAssets(directory);
            function.Should().Throw<ArgumentException>();
        }

        [Fact]
        public void GetDuplicatedAssetsWithDuplicatesTest()
        {
            IDatabase database = new Database();
            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            Folder folder = repository.AddFolder(dataDirectory);
            IFindDuplicatedAssetsService findDuplicatedAssetsService = new FindDuplicatedAssetsService(
                repository,
                storageService);
            IImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repository,
                storageService,
                directoryComparer);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                repository,
                assetHashCalculatorService,
                storageService,
                userConfigurationService,
                directoryComparer);
            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                repository,
                storageService,
                catalogAssetsService);
            Application.Application app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
                repository,
                userConfigurationService,
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

            Console.WriteLine("database.DataDirectory: " + database.DataDirectory);
            Console.WriteLine("database.Separator: " + database.Separator);

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
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
                repository,
                userConfigurationService,
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
            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);
            
            IFindDuplicatedAssetsService findDuplicatedAssetsService = new FindDuplicatedAssetsService(
                repository,
                storageService);
            IImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repository,
                storageService,
                directoryComparer);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                repository,
                assetHashCalculatorService,
                storageService,
                userConfigurationService,
                directoryComparer);
            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                repository,
                storageService,
                catalogAssetsService);
            Application.Application app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
                repository,
                userConfigurationService,
                storageService);

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
            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);
            IFindDuplicatedAssetsService findDuplicatedAssetsService = new FindDuplicatedAssetsService(
                repository,
                storageService);
            IImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repository,
                storageService,
                directoryComparer);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                repository,
                assetHashCalculatorService,
                storageService,
                userConfigurationService,
                directoryComparer);
            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                repository,
                storageService,
                catalogAssetsService);
            Application.Application app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
                repository,
                userConfigurationService,
                storageService);

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
            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            Folder folder = repository.AddFolder(dataDirectory);
            IFindDuplicatedAssetsService findDuplicatedAssetsService = new FindDuplicatedAssetsService(
                repository,
                storageService);
            IImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repository,
                storageService,
                directoryComparer);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                repository,
                assetHashCalculatorService,
                storageService,
                userConfigurationService,
                directoryComparer);
            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                repository,
                storageService,
                catalogAssetsService);
            Application.Application app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
                repository,
                userConfigurationService,
                storageService);

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            Folder folder = repository.AddFolder(dataDirectory);

            Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();
            hashCalculator.Setup(h => h.CalculateHash(It.IsAny<byte[]>())).Returns("abcd1234");

            IFindDuplicatedAssetsService findDuplicatedAssetsService = new FindDuplicatedAssetsService(
                repository,
                storageService);
            IImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repository,
                storageService,
                directoryComparer);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                repository,
                hashCalculator.Object,
                storageService,
                userConfigurationService,
                directoryComparer);
            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                repository,
                storageService,
                catalogAssetsService);
            Application.Application app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
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

            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            catalogAssetsService = new CatalogAssetsService(
                repository,
                assetHashCalculatorService,
                storageService,
                userConfigurationService,
                directoryComparer);
            moveAssetsService = new MoveAssetsService(
                repository,
                storageService,
                catalogAssetsService);
            app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
                repository,
                userConfigurationService,
                storageService);

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
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            IAssetRepository repository = new AssetRepository(database, storageService, userConfigurationService);
            repository.AddFolder(dataDirectory);

            Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();
            hashCalculator.Setup(h => h.CalculateHash(It.IsAny<byte[]>())).Returns("abcd1234");

            IFindDuplicatedAssetsService findDuplicatedAssetsService = new FindDuplicatedAssetsService(
                repository,
                storageService);
            IImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repository,
                storageService,
                directoryComparer);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                repository,
                hashCalculator.Object,
                storageService,
                userConfigurationService,
                directoryComparer);
            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                repository,
                storageService,
                catalogAssetsService);
            Application.Application app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
                repository,
                userConfigurationService,
                storageService);

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

            Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();
            hashCalculator.Setup(h => h.CalculateHash(It.IsAny<byte[]>())).Returns("abcd1234");

            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            IDirectoryComparer directoryComparer = new DirectoryComparer();
            UnencapsulatedAssetRepository repository = new UnencapsulatedAssetRepository(database, storageService, userConfigurationService);
            Folder folder = repository.AddFolder(dataDirectory);

            IFindDuplicatedAssetsService findDuplicatedAssetsService = new FindDuplicatedAssetsService(
                repository,
                storageService);
            IImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repository,
                storageService,
                directoryComparer);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                repository,
                hashCalculator.Object,
                storageService,
                userConfigurationService,
                directoryComparer);
            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                repository,
                storageService,
                catalogAssetsService);
            Application.Application app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
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
                    storageService,
                    userConfigurationService,
                    directoryComparer);

            app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
                repository,
                userConfigurationService,
                storageService);

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
            IUserConfigurationService userConfigurationService = new UserConfigurationService(configuration);
            IStorageService storageService = new StorageService(userConfigurationService);
            UnencapsulatedAssetRepository repository = new UnencapsulatedAssetRepository(database, storageService, userConfigurationService);
            Folder folder = repository.AddFolder(dataDirectory);
            Mock<IAssetHashCalculatorService> hashCalculator = new Mock<IAssetHashCalculatorService>();

            IDirectoryComparer directoryComparer = new DirectoryComparer();
            IFindDuplicatedAssetsService findDuplicatedAssetsService = new FindDuplicatedAssetsService(
                repository,
                storageService);
            IImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repository,
                storageService,
                directoryComparer);
            ICatalogAssetsService catalogAssetsService = new CatalogAssetsService(
                repository,
                hashCalculator.Object,
                storageService,
                userConfigurationService,
                directoryComparer);
            IMoveAssetsService moveAssetsService = new MoveAssetsService(
                repository,
                storageService,
                catalogAssetsService);
            Application.Application app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
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

            IAssetHashCalculatorService assetHashCalculatorService = new AssetHashCalculatorService();
            userConfigurationService = new UserConfigurationService(configuration);
            storageService = new StorageService(userConfigurationService);
            directoryComparer = new DirectoryComparer();
            findDuplicatedAssetsService = new FindDuplicatedAssetsService(
                repository,
                storageService);
            importNewAssetsService = new ImportNewAssetsService(
                repository,
                storageService,
                directoryComparer);
            catalogAssetsService = new CatalogAssetsService(
                repository,
                assetHashCalculatorService,
                storageService,
                userConfigurationService,
                directoryComparer);
            moveAssetsService = new MoveAssetsService(
                repository,
                storageService,
                catalogAssetsService);
            app = new Application.Application(
                importNewAssetsService,
                catalogAssetsService,
                moveAssetsService,
                findDuplicatedAssetsService,
                repository,
                userConfigurationService,
                storageService);

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
