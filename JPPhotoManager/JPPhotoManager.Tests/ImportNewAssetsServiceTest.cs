using JPPhotoManager.Domain;
using Moq;
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class ImportNewAssetsServiceTest
    {
        [Fact]
        public void ImportNewImagesSourceEmptyDestinationEmptyTest()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";

            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration();

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            Mock<IAssetRepository> repositoryMock = new Mock<IAssetRepository>();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new Mock<IAssetHashCalculatorService>();
            Mock<IStorageService> storageServiceMock = new Mock<IStorageService>();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new Mock<IUserConfigurationService>();

            repositoryMock.Setup(r => r.GetImportNewAssetsConfiguration())
                .Returns(importConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

            ImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer());

            var statusChanges = new List<StatusChangeCallbackEventArgs>();
            
            var result = importNewAssetsService.Import(e => statusChanges.Add(e));

            repositoryMock.Verify(r => r.GetImportNewAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()), Times.Never);
            Assert.Single(result);
            Assert.Equal(@"C:\MyGame\Screenshots", result[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame", result[0].DestinationDirectory);
            Assert.Equal(0, result[0].ImportedImages);
            Assert.Equal(@"No images imported from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.", result[0].Message);
            Assert.Empty(statusChanges);
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

            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration();

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            Mock<IAssetRepository> repositoryMock = new Mock<IAssetRepository>();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new Mock<IAssetHashCalculatorService>();
            Mock<IStorageService> storageServiceMock = new Mock<IStorageService>();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new Mock<IUserConfigurationService>();

            repositoryMock.Setup(r => r.GetImportNewAssetsConfiguration())
                .Returns(importConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.GetFileNames(sourceDirectory))
                .Returns(sourceFileNames);

            storageServiceMock.Setup(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()))
                .Returns(true);

            ImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer());

            var statusChanges = new List<StatusChangeCallbackEventArgs>();

            var result = importNewAssetsService.Import(e => statusChanges.Add(e));

            repositoryMock.Verify(r => r.GetImportNewAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage2.jpg", @"C:\Images\MyGame\NewImage2.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage3.jpg", @"C:\Images\MyGame\NewImage3.jpg"), Times.Once);
            Assert.Single(result);
            Assert.Equal(@"C:\MyGame\Screenshots", result[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame", result[0].DestinationDirectory);
            Assert.Equal(3, result[0].ImportedImages);
            Assert.Equal(@"3 images imported from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.", result[0].Message);
            Assert.Equal(3, statusChanges.Count);
            Assert.Equal(@$"Image 'C:\MyGame\Screenshots\NewImage1.jpg' imported to 'C:\Images\MyGame\NewImage1.jpg'", statusChanges[0].NewStatus);
            Assert.Equal(@$"Image 'C:\MyGame\Screenshots\NewImage2.jpg' imported to 'C:\Images\MyGame\NewImage2.jpg'", statusChanges[1].NewStatus);
            Assert.Equal(@$"Image 'C:\MyGame\Screenshots\NewImage3.jpg' imported to 'C:\Images\MyGame\NewImage3.jpg'", statusChanges[2].NewStatus);
        }

        [Fact]
        public void ImportNewImagesSourceNotEmptyDestinationNotEmptyMultipleNewImagesTest()
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

            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration();

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            Mock<IAssetRepository> repositoryMock = new Mock<IAssetRepository>();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new Mock<IAssetHashCalculatorService>();
            Mock<IStorageService> storageServiceMock = new Mock<IStorageService>();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new Mock<IUserConfigurationService>();

            repositoryMock.Setup(r => r.GetImportNewAssetsConfiguration())
                .Returns(importConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

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

            var statusChanges = new List<StatusChangeCallbackEventArgs>();

            var result = importNewAssetsService.Import(e => statusChanges.Add(e));

            repositoryMock.Verify(r => r.GetImportNewAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage2.jpg", @"C:\Images\MyGame\NewImage2.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage3.jpg", @"C:\Images\MyGame\NewImage3.jpg"), Times.Once);
            Assert.Single(result);
            Assert.Equal(@"C:\MyGame\Screenshots", result[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame", result[0].DestinationDirectory);
            Assert.Equal(3, result[0].ImportedImages);
            Assert.Equal(@"3 images imported from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.", result[0].Message);
            Assert.Equal(3, statusChanges.Count);
            Assert.Equal(@$"Image 'C:\MyGame\Screenshots\NewImage1.jpg' imported to 'C:\Images\MyGame\NewImage1.jpg'", statusChanges[0].NewStatus);
            Assert.Equal(@$"Image 'C:\MyGame\Screenshots\NewImage2.jpg' imported to 'C:\Images\MyGame\NewImage2.jpg'", statusChanges[1].NewStatus);
            Assert.Equal(@$"Image 'C:\MyGame\Screenshots\NewImage3.jpg' imported to 'C:\Images\MyGame\NewImage3.jpg'", statusChanges[2].NewStatus);
        }

        [Fact]
        public void ImportNewImagesSourceNotEmptyDestinationNotEmptyOneNewImageTest()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";

            string[] sourceFileNames = new string[]
            {
                "ExistingImage1.jpg",
                "ExistingImage2.jpg",
                "ExistingImage3.jpg",
                "NewImage1.jpg"
            };

            string[] destinationFileNames = new string[]
            {
                "ExistingImage1.jpg",
                "ExistingImage2.jpg",
                "ExistingImage3.jpg"
            };

            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration();

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });
            
            Mock<IAssetRepository> repositoryMock = new Mock<IAssetRepository>();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new Mock<IAssetHashCalculatorService>();
            Mock<IStorageService> storageServiceMock = new Mock<IStorageService>();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new Mock<IUserConfigurationService>();

            repositoryMock.Setup(r => r.GetImportNewAssetsConfiguration())
                .Returns(importConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

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

            var statusChanges = new List<StatusChangeCallbackEventArgs>();

            var result = importNewAssetsService.Import(e => statusChanges.Add(e));

            repositoryMock.Verify(r => r.GetImportNewAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            Assert.Single(result);
            Assert.Equal(@"C:\MyGame\Screenshots", result[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame", result[0].DestinationDirectory);
            Assert.Equal(1, result[0].ImportedImages);
            Assert.Equal(@"1 image imported from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.", result[0].Message);
            Assert.Single(statusChanges);
            Assert.Equal(@$"Image 'C:\MyGame\Screenshots\NewImage1.jpg' imported to 'C:\Images\MyGame\NewImage1.jpg'", statusChanges[0].NewStatus);
        }

        [Fact]
        public void ImportNewImagesSourceNotEmptyDestinationNotEmptyTwoDefinitionsTest()
        {
            string firstSourceDirectory = @"C:\MyFirstGame\Screenshots";
            string firstDestinationDirectory = @"C:\Images\MyFirstGame";
            string secondSourceDirectory = @"C:\MySecondGame\Screenshots";
            string secondDestinationDirectory = @"C:\Images\MySecondGame";

            string[] firstSourceFileNames = new string[]
            {
                "ExistingImage1.jpg",
                "ExistingImage2.jpg",
                "ExistingImage3.jpg",
                "NewImage1.jpg",
                "NewImage2.jpg",
                "NewImage3.jpg"
            };

            string[] secondSourceFileNames = new string[]
            {
                "ExistingImage1.jpg",
                "ExistingImage2.jpg",
                "NewImage1.jpg",
                "NewImage2.jpg"
            };

            string[] firstDestinationFileNames = new string[]
            {
                "ExistingImage1.jpg",
                "ExistingImage2.jpg",
                "ExistingImage3.jpg"
            };

            string[] secondDestinationFileNames = new string[]
            {
                "ExistingImage1.jpg",
                "ExistingImage2.jpg"
            };

            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration();

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = firstSourceDirectory,
                    DestinationDirectory = firstDestinationDirectory
                });

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = secondSourceDirectory,
                    DestinationDirectory = secondDestinationDirectory
                });
            
            Mock<IAssetRepository> repositoryMock = new Mock<IAssetRepository>();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new Mock<IAssetHashCalculatorService>();
            Mock<IStorageService> storageServiceMock = new Mock<IStorageService>();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new Mock<IUserConfigurationService>();

            repositoryMock.Setup(r => r.GetImportNewAssetsConfiguration())
                .Returns(importConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(firstSourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(firstDestinationDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(secondSourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(secondDestinationDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.GetFileNames(firstSourceDirectory))
                .Returns(firstSourceFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(firstDestinationDirectory))
                .Returns(firstDestinationFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(secondSourceDirectory))
                .Returns(secondSourceFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(secondDestinationDirectory))
                .Returns(secondDestinationFileNames);

            storageServiceMock.Setup(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()))
                .Returns(true);

            ImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer());

            var statusChanges = new List<StatusChangeCallbackEventArgs>();

            var result = importNewAssetsService.Import(e => statusChanges.Add(e));

            repositoryMock.Verify(r => r.GetImportNewAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(firstSourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(secondSourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyFirstGame\Screenshots\NewImage1.jpg", @"C:\Images\MyFirstGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyFirstGame\Screenshots\NewImage2.jpg", @"C:\Images\MyFirstGame\NewImage2.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyFirstGame\Screenshots\NewImage3.jpg", @"C:\Images\MyFirstGame\NewImage3.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MySecondGame\Screenshots\NewImage1.jpg", @"C:\Images\MySecondGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MySecondGame\Screenshots\NewImage2.jpg", @"C:\Images\MySecondGame\NewImage2.jpg"), Times.Once);
            Assert.Equal(2, result.Count);
            Assert.Equal(@"C:\MyFirstGame\Screenshots", result[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyFirstGame", result[0].DestinationDirectory);
            Assert.Equal(3, result[0].ImportedImages);
            Assert.Equal(@"3 images imported from 'C:\MyFirstGame\Screenshots' to 'C:\Images\MyFirstGame'.", result[0].Message);
            Assert.Equal(@"C:\MySecondGame\Screenshots", result[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MySecondGame", result[1].DestinationDirectory);
            Assert.Equal(2, result[1].ImportedImages);
            Assert.Equal(@"2 images imported from 'C:\MySecondGame\Screenshots' to 'C:\Images\MySecondGame'.", result[1].Message);
            Assert.Equal(5, statusChanges.Count);
            Assert.Equal(@$"Image 'C:\MyFirstGame\Screenshots\NewImage1.jpg' imported to 'C:\Images\MyFirstGame\NewImage1.jpg'", statusChanges[0].NewStatus);
            Assert.Equal(@$"Image 'C:\MyFirstGame\Screenshots\NewImage2.jpg' imported to 'C:\Images\MyFirstGame\NewImage2.jpg'", statusChanges[1].NewStatus);
            Assert.Equal(@$"Image 'C:\MyFirstGame\Screenshots\NewImage3.jpg' imported to 'C:\Images\MyFirstGame\NewImage3.jpg'", statusChanges[2].NewStatus);
            Assert.Equal(@$"Image 'C:\MySecondGame\Screenshots\NewImage1.jpg' imported to 'C:\Images\MySecondGame\NewImage1.jpg'", statusChanges[3].NewStatus);
            Assert.Equal(@$"Image 'C:\MySecondGame\Screenshots\NewImage2.jpg' imported to 'C:\Images\MySecondGame\NewImage2.jpg'", statusChanges[4].NewStatus);
        }

        [Fact]
        public void ValidateAllValidDefinitionsTest()
        {
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

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                });
            
            importConfiguration.Validate();

            Assert.Equal(3, importConfiguration.Imports.Count);
            Assert.Equal(@"C:\MyFirstGame\Screenshots", importConfiguration.Imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyFirstGame", importConfiguration.Imports[0].DestinationDirectory);
            Assert.Equal(@"C:\MySecondGame\Screenshots", importConfiguration.Imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MySecondGame", importConfiguration.Imports[1].DestinationDirectory);
            Assert.Equal(@"\\MyServer\Images", importConfiguration.Imports[2].SourceDirectory);
            Assert.Equal(@"C:\Images", importConfiguration.Imports[2].DestinationDirectory);
        }

        [Fact]
        public void ValidateOneInvalidDefinitionTest()
        {
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

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"http://www.some-site.com",
                    DestinationDirectory = @"ftp://some-location.com"
                });

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"InvalidValue",
                    DestinationDirectory = @"InvalidValue"
                });

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"Invalid@Value.com",
                    DestinationDirectory = @"Invalid@Value.com"
                });
            
            importConfiguration.Validate();

            Assert.Equal(2, importConfiguration.Imports.Count);
            Assert.Equal(@"C:\MyFirstGame\Screenshots", importConfiguration.Imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyFirstGame", importConfiguration.Imports[0].DestinationDirectory);
            Assert.Equal(@"C:\MySecondGame\Screenshots", importConfiguration.Imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MySecondGame", importConfiguration.Imports[1].DestinationDirectory);
        }

        [Fact]
        public void NormalizeTest()
        {
            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration();

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyFirstGame\Screenshots",
                    DestinationDirectory = @"C:\Images\\\MyFirstGame\"
                });

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\\\MySecondGame\Screenshots\\",
                    DestinationDirectory = @"C:\Images\MySecondGame\\\\\"
                });

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Screenshots\\\",
                    DestinationDirectory = @"C:\Images\\\\\"
                });

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\\\\MyServer\Screenshots\\\",
                    DestinationDirectory = @"C:\Images\\\\\"
                });
            
            importConfiguration.Normalize();

            Assert.Equal(4, importConfiguration.Imports.Count);
            Assert.Equal(@"C:\MyFirstGame\Screenshots", importConfiguration.Imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyFirstGame", importConfiguration.Imports[0].DestinationDirectory);
            Assert.Equal(@"C:\MySecondGame\Screenshots", importConfiguration.Imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MySecondGame", importConfiguration.Imports[1].DestinationDirectory);
            Assert.Equal(@"\\MyServer\Screenshots", importConfiguration.Imports[2].SourceDirectory);
            Assert.Equal(@"C:\Images", importConfiguration.Imports[2].DestinationDirectory);
            Assert.Equal(@"\\MyServer\Screenshots", importConfiguration.Imports[3].SourceDirectory);
            Assert.Equal(@"C:\Images", importConfiguration.Imports[3].DestinationDirectory);
        }

        [Fact]
        public void ImportNewImagesInexistentSourceDirectory()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";

            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration();

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });
            
            Mock<IAssetRepository> repositoryMock = new Mock<IAssetRepository>();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new Mock<IAssetHashCalculatorService>();
            Mock<IStorageService> storageServiceMock = new Mock<IStorageService>();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new Mock<IUserConfigurationService>();

            repositoryMock.Setup(r => r.GetImportNewAssetsConfiguration())
                .Returns(importConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(false);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

            ImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer());

            var statusChanges = new List<StatusChangeCallbackEventArgs>();

            var result = importNewAssetsService.Import(e => statusChanges.Add(e));

            repositoryMock.Verify(r => r.GetImportNewAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Never);
            storageServiceMock.Verify(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()), Times.Never);
            Assert.Single(result);
            Assert.Equal(@"C:\MyGame\Screenshots", result[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame", result[0].DestinationDirectory);
            Assert.Equal(0, result[0].ImportedImages);
            Assert.Equal(@"Source directory 'C:\MyGame\Screenshots' not found.", result[0].Message);
            Assert.Empty(statusChanges);
        }

        [Fact]
        public void ImportNewImagesInexistentDestinationDirectory()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";

            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration();

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });
            
            Mock<IAssetRepository> repositoryMock = new Mock<IAssetRepository>();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new Mock<IAssetHashCalculatorService>();
            Mock<IStorageService> storageServiceMock = new Mock<IStorageService>();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new Mock<IUserConfigurationService>();

            repositoryMock.Setup(r => r.GetImportNewAssetsConfiguration())
                .Returns(importConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(false);

            ImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer());

            var statusChanges = new List<StatusChangeCallbackEventArgs>();

            var result = importNewAssetsService.Import(e => statusChanges.Add(e));

            repositoryMock.Verify(r => r.GetImportNewAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.CreateDirectory(destinationDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()), Times.Never);
            Assert.Single(result);
            Assert.Equal(@"C:\MyGame\Screenshots", result[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame", result[0].DestinationDirectory);
            Assert.Equal(0, result[0].ImportedImages);
            Assert.Equal(@"No images imported from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.", result[0].Message);
            Assert.Empty(statusChanges);
        }

        [Fact]
        public void ImportNewImagesSourceNotEmptyDestinationEmptyIncludingSubFoldersTest()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string sourceSubDirectory = @"C:\MyGame\Screenshots\SubDirectory";
            string destinationDirectory = @"C:\Images\MyGame";

            string[] sourceFileNames = new string[]
            {
                "NewImage1.jpg",
                "NewImage2.jpg",
                "NewImage3.jpg"
            };

            ImportNewAssetsConfiguration importConfiguration = new ImportNewAssetsConfiguration();

            importConfiguration.Imports.Add(
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory,
                    IncludeSubFolders = true
                });

            Mock<IAssetRepository> repositoryMock = new Mock<IAssetRepository>();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new Mock<IAssetHashCalculatorService>();
            Mock<IStorageService> storageServiceMock = new Mock<IStorageService>();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new Mock<IUserConfigurationService>();

            repositoryMock.Setup(r => r.GetImportNewAssetsConfiguration())
                .Returns(importConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(sourceSubDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.GetFileNames(sourceDirectory))
                .Returns(sourceFileNames);

            storageServiceMock.Setup(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()))
                .Returns(true);

            storageServiceMock.Setup(s => s.GetSubDirectories(sourceDirectory)).Returns(
                new List<DirectoryInfo>
                {
                    new DirectoryInfo(sourceSubDirectory)
                });

            storageServiceMock.Setup(s => s.GetSubDirectories(sourceSubDirectory)).Returns(new List<DirectoryInfo>());

            ImportNewAssetsService importNewAssetsService = new ImportNewAssetsService(
                repositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer());

            var statusChanges = new List<StatusChangeCallbackEventArgs>();

            var result = importNewAssetsService.Import(e => statusChanges.Add(e));

            repositoryMock.Verify(r => r.GetImportNewAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage2.jpg", @"C:\Images\MyGame\NewImage2.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage3.jpg", @"C:\Images\MyGame\NewImage3.jpg"), Times.Once);
            Assert.Equal(2, result.Count);
            Assert.Equal(@"C:\MyGame\Screenshots", result[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame", result[0].DestinationDirectory);
            Assert.Equal(3, result[0].ImportedImages);
            Assert.Equal(@"3 images imported from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.", result[0].Message);
            Assert.Equal(@"C:\MyGame\Screenshots\SubDirectory", result[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame\SubDirectory", result[1].DestinationDirectory);
            Assert.Equal(0, result[1].ImportedImages);
            Assert.Equal(@"No images imported from 'C:\MyGame\Screenshots\SubDirectory' to 'C:\Images\MyGame\SubDirectory'.", result[1].Message);
            Assert.Equal(3, statusChanges.Count);
            Assert.Equal(@$"Image 'C:\MyGame\Screenshots\NewImage1.jpg' imported to 'C:\Images\MyGame\NewImage1.jpg'", statusChanges[0].NewStatus);
            Assert.Equal(@$"Image 'C:\MyGame\Screenshots\NewImage2.jpg' imported to 'C:\Images\MyGame\NewImage2.jpg'", statusChanges[1].NewStatus);
            Assert.Equal(@$"Image 'C:\MyGame\Screenshots\NewImage3.jpg' imported to 'C:\Images\MyGame\NewImage3.jpg'", statusChanges[2].NewStatus);
        }
    }
}
