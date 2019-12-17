using JPPhotoManager.Domain;
using Moq;
using System;
using System.Collections.Generic;
using System.Text;
using Xunit;

namespace JPPhotoManager.Test
{
    public class ImportNewAssetsServiceTest
    {
        [Fact]
        public void ImportNewImagesSourceEmptyDestinationEmptyTest()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";

            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration
            {
                Imports = new List<ImportNewAssetsDirectoriesDefinition>
                {
                    new ImportNewAssetsDirectoriesDefinition
                    {
                        SourceDirectory = sourceDirectory,
                        DestinationDirectory = destinationDirectory
                    }
                }
            };

            Mock<IAssetRepository> repositoryMock = new Mock<IAssetRepository>();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new Mock<IAssetHashCalculatorService>();
            Mock<IStorageService> storageServiceMock = new Mock<IStorageService>();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new Mock<IUserConfigurationService>();

            repositoryMock.Setup(r => r.GetImportNewAssetsConfiguration())
                .Returns(importConfiguration);

            ImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer());

            var result = importNewAssetsService.Import();

            repositoryMock.Verify(r => r.GetImportNewAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()), Times.Never);
            Assert.Single(result);
            Assert.Equal(@"C:\MyGame\Screenshots", result[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame", result[0].DestinationDirectory);
            Assert.Equal(0, result[0].ImportedImages);
        }

        [Fact]
        public void ImportNewImagesSourceNotEmptyDestinationEmptyTest()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";

            string[] sourceFileNames = new string[]
            {
                "NewImage1.jpg",
                "NewImage2.jpg",
                "NewImage3.jpg"
            };

            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration
            {
                Imports = new List<ImportNewAssetsDirectoriesDefinition>
                {
                    new ImportNewAssetsDirectoriesDefinition
                    {
                        SourceDirectory = sourceDirectory,
                        DestinationDirectory = destinationDirectory
                    }
                }
            };

            Mock<IAssetRepository> repositoryMock = new Mock<IAssetRepository>();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new Mock<IAssetHashCalculatorService>();
            Mock<IStorageService> storageServiceMock = new Mock<IStorageService>();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new Mock<IUserConfigurationService>();

            repositoryMock.Setup(r => r.GetImportNewAssetsConfiguration())
                .Returns(importConfiguration);

            storageServiceMock.Setup(s => s.GetFileNames(sourceDirectory))
                .Returns(sourceFileNames);

            storageServiceMock.Setup(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()))
                .Returns(true);

            ImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer());

            var result = importNewAssetsService.Import();

            repositoryMock.Verify(r => r.GetImportNewAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage2.jpg", @"C:\Images\MyGame\NewImage2.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage3.jpg", @"C:\Images\MyGame\NewImage3.jpg"), Times.Once);
            Assert.Single(result);
            Assert.Equal(@"C:\MyGame\Screenshots", result[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame", result[0].DestinationDirectory);
            Assert.Equal(3, result[0].ImportedImages);
        }

        [Fact]
        public void ImportNewImagesSourceNotEmptyDestinationNotEmptyTest()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";

            string[] sourceFileNames = new string[]
            {
                "ExistingImage1.jpg",
                "ExistingImage2.jpg",
                "ExistingImage3.jpg",
                "NewImage1.jpg",
                "NewImage2.jpg",
                "NewImage3.jpg"
            };

            string[] destinationFileNames = new string[]
            {
                "ExistingImage1.jpg",
                "ExistingImage2.jpg",
                "ExistingImage3.jpg"
            };

            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration
            {
                Imports = new List<ImportNewAssetsDirectoriesDefinition>
                {
                    new ImportNewAssetsDirectoriesDefinition
                    {
                        SourceDirectory = sourceDirectory,
                        DestinationDirectory = destinationDirectory
                    }
                }
            };

            Mock<IAssetRepository> repositoryMock = new Mock<IAssetRepository>();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new Mock<IAssetHashCalculatorService>();
            Mock<IStorageService> storageServiceMock = new Mock<IStorageService>();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new Mock<IUserConfigurationService>();

            repositoryMock.Setup(r => r.GetImportNewAssetsConfiguration())
                .Returns(importConfiguration);

            storageServiceMock.Setup(s => s.GetFileNames(sourceDirectory))
                .Returns(sourceFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationDirectory))
                .Returns(destinationFileNames);

            storageServiceMock.Setup(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()))
                .Returns(true);

            ImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer());

            var result = importNewAssetsService.Import();

            repositoryMock.Verify(r => r.GetImportNewAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage2.jpg", @"C:\Images\MyGame\NewImage2.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage3.jpg", @"C:\Images\MyGame\NewImage3.jpg"), Times.Once);
            Assert.Single(result);
            Assert.Equal(@"C:\MyGame\Screenshots", result[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame", result[0].DestinationDirectory);
            Assert.Equal(3, result[0].ImportedImages);
        }
    }
}
