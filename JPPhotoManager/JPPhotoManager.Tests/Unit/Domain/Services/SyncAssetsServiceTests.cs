﻿using Autofac;
using Autofac.Extras.Moq;
using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Domain.Interfaces.Repositories;
using JPPhotoManager.Domain.Interfaces.Services;
using JPPhotoManager.Domain.Services;
using Moq;
using System.IO;
using Xunit;

namespace JPPhotoManager.Tests.Unit.Domain.Services
{
    public class SyncAssetsServiceTests
    {
        [Fact]
        public async void SyncAssetsSourceEmptyDestinationEmptyTest()
        {
            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                });
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            mock.Mock<ISyncAssetsConfigurationRepository>().Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

            mock.Mock<IStorageService>().Setup(s => s.FolderExists(sourceDirectory))
            .Returns(true);

            mock.Mock<IStorageService>().Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

            SyncAssetsService syncAssetsService = mock.Container.Resolve<SyncAssetsService>();

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            mock.Mock<ISyncAssetsConfigurationRepository>().Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            mock.Mock<IStorageService>().Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            mock.Mock<IStorageService>().Verify(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()), Times.Never);
            result.Should().ContainSingle();
            result[0].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame");
            result[0].SyncedImages.Should().Be(0);
            result[0].Message.Should().Be(@"No images synced from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.");
            statusChanges.Should().BeEmpty();
        }

        [Fact]
        public async void SyncAssetsSourceNotEmptyDestinationEmptyTest()
        {
            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                });
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";

            string[] sourceFileNames = new string[]
            {
                    "NewImage1.jpg",
                    "NewImage2.jpg",
                    "NewImage3.jpg"
            };

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            mock.Mock<ISyncAssetsConfigurationRepository>().Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

            mock.Mock<IStorageService>().Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            mock.Mock<IStorageService>().Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

            mock.Mock<IStorageService>().Setup(s => s.GetFileNames(sourceDirectory))
                .Returns(sourceFileNames);

            mock.Mock<IStorageService>().Setup(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()))
                .Returns(true);

            SyncAssetsService syncAssetsService = mock.Container.Resolve<SyncAssetsService>();

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            mock.Mock<ISyncAssetsConfigurationRepository>().Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            mock.Mock<IStorageService>().Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            mock.Mock<IStorageService>().Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            mock.Mock<IStorageService>().Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage2.jpg", @"C:\Images\MyGame\NewImage2.jpg"), Times.Once);
            mock.Mock<IStorageService>().Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage3.jpg", @"C:\Images\MyGame\NewImage3.jpg"), Times.Once);
            result.Should().ContainSingle();
            result[0].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame");
            result[0].SyncedImages.Should().Be(3);
            result[0].Message.Should().Be(@"3 images synced from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.");
            statusChanges.Should().HaveCount(3);
            statusChanges[0].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage1.jpg' => 'C:\Images\MyGame\NewImage1.jpg'");
            statusChanges[1].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage2.jpg' => 'C:\Images\MyGame\NewImage2.jpg'");
            statusChanges[2].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage3.jpg' => 'C:\Images\MyGame\NewImage3.jpg'");
        }

        [Fact]
        public async void SyncedAssetsSourceNotEmptyDestinationNotEmptyMultipleNewImagesTest()
        {
            using var mock = AutoMock.GetLoose(
                cfg =>
                {
                    cfg.RegisterType<DirectoryComparer>().As<IDirectoryComparer>().SingleInstance();
                });
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

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            mock.Mock<ISyncAssetsConfigurationRepository>().Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

            mock.Mock<IStorageService>().Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            mock.Mock<IStorageService>().Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

            mock.Mock<IStorageService>().Setup(s => s.GetFileNames(sourceDirectory))
                .Returns(sourceFileNames);

            mock.Mock<IStorageService>().Setup(s => s.GetFileNames(destinationDirectory))
                .Returns(destinationFileNames);

            mock.Mock<IStorageService>().Setup(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()))
                .Returns(true);

            SyncAssetsService syncAssetsService = mock.Container.Resolve<SyncAssetsService>();

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            mock.Mock<ISyncAssetsConfigurationRepository>().Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            mock.Mock<IStorageService>().Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            mock.Mock<IStorageService>().Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            mock.Mock<IStorageService>().Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage2.jpg", @"C:\Images\MyGame\NewImage2.jpg"), Times.Once);
            mock.Mock<IStorageService>().Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage3.jpg", @"C:\Images\MyGame\NewImage3.jpg"), Times.Once);
            result.Should().ContainSingle();
            result[0].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame");
            result[0].SyncedImages.Should().Be(3);
            result[0].Message.Should().Be(@"3 images synced from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.");
            statusChanges.Should().HaveCount(3);
            statusChanges[0].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage1.jpg' => 'C:\Images\MyGame\NewImage1.jpg'");
            statusChanges[1].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage2.jpg' => 'C:\Images\MyGame\NewImage2.jpg'");
            statusChanges[2].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage3.jpg' => 'C:\Images\MyGame\NewImage3.jpg'");
        }

        [Fact]
        public async void SyncAssetsSourceNotEmptyDestinationNotEmptyOneNewImageTest()
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

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            Mock<IAssetRepository> assetRepositoryMock = new();
            Mock<ISyncAssetsConfigurationRepository> syncAssetsConfigurationRepositoryMock = new();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new();
            Mock<IStorageService> storageServiceMock = new();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new();

            syncAssetsConfigurationRepositoryMock.Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

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

            SyncAssetsService syncAssetsService = new(
                assetRepositoryMock.Object,
                syncAssetsConfigurationRepositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer(storageServiceMock.Object));

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            syncAssetsConfigurationRepositoryMock.Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            result.Should().ContainSingle();
            result[0].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame");
            result[0].SyncedImages.Should().Be(1);
            result[0].Message.Should().Be(@"1 image synced from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.");
            statusChanges.Should().ContainSingle();
            statusChanges[0].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage1.jpg' => 'C:\Images\MyGame\NewImage1.jpg'");
        }

        [Fact]
        public async void SyncAssetsSourceNotEmptyDestinationNotEmptyTwoDefinitionsTest()
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

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = firstSourceDirectory,
                    DestinationDirectory = firstDestinationDirectory
                });

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = secondSourceDirectory,
                    DestinationDirectory = secondDestinationDirectory
                });

            Mock<IAssetRepository> assetRepositoryMock = new();
            Mock<ISyncAssetsConfigurationRepository> syncAssetsConfigurationRepositoryMock = new();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new();
            Mock<IStorageService> storageServiceMock = new();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new();

            syncAssetsConfigurationRepositoryMock.Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

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

            SyncAssetsService syncAssetsService = new(
                assetRepositoryMock.Object,
                syncAssetsConfigurationRepositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer(storageServiceMock.Object));

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            syncAssetsConfigurationRepositoryMock.Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(firstSourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(secondSourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyFirstGame\Screenshots\NewImage1.jpg", @"C:\Images\MyFirstGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyFirstGame\Screenshots\NewImage2.jpg", @"C:\Images\MyFirstGame\NewImage2.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyFirstGame\Screenshots\NewImage3.jpg", @"C:\Images\MyFirstGame\NewImage3.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MySecondGame\Screenshots\NewImage1.jpg", @"C:\Images\MySecondGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MySecondGame\Screenshots\NewImage2.jpg", @"C:\Images\MySecondGame\NewImage2.jpg"), Times.Once);
            result.Should().HaveCount(2);
            result[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            result[0].SyncedImages.Should().Be(3);
            result[0].Message.Should().Be(@"3 images synced from 'C:\MyFirstGame\Screenshots' to 'C:\Images\MyFirstGame'.");
            result[1].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            result[1].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
            result[1].SyncedImages.Should().Be(2);
            result[1].Message.Should().Be(@"2 images synced from 'C:\MySecondGame\Screenshots' to 'C:\Images\MySecondGame'.");
            statusChanges.Should().HaveCount(5);
            statusChanges[0].NewStatus.Should().Be(@$"'C:\MyFirstGame\Screenshots\NewImage1.jpg' => 'C:\Images\MyFirstGame\NewImage1.jpg'");
            statusChanges[1].NewStatus.Should().Be(@$"'C:\MyFirstGame\Screenshots\NewImage2.jpg' => 'C:\Images\MyFirstGame\NewImage2.jpg'");
            statusChanges[2].NewStatus.Should().Be(@$"'C:\MyFirstGame\Screenshots\NewImage3.jpg' => 'C:\Images\MyFirstGame\NewImage3.jpg'");
            statusChanges[3].NewStatus.Should().Be(@$"'C:\MySecondGame\Screenshots\NewImage1.jpg' => 'C:\Images\MySecondGame\NewImage1.jpg'");
            statusChanges[4].NewStatus.Should().Be(@$"'C:\MySecondGame\Screenshots\NewImage2.jpg' => 'C:\Images\MySecondGame\NewImage2.jpg'");
        }

        [Fact]
        public async void SyncAssetsSourceNotEmptyDestinationNotEmptySomeSourceImagesInTargetSubDirectoriesSingleLevelWithoutSourceSubDirectories()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";
            string destinationSubDirectory1 = @"C:\Images\MyGame\SubDirectory1";
            string destinationSubDirectory2 = @"C:\Images\MyGame\SubDirectory2";

            string[] sourceFileNames = new string[]
            {
                "ExistingImage1.jpg",
                "ExistingImage2.jpg",
                "ExistingImage3.jpg",
                "ExistingImage4.jpg",
                "ExistingImage5.jpg",
                "NewImage1.jpg",
                "NewImage2.jpg",
                "NewImage3.jpg"
            };

            string[] destinationFileNames = new string[]
            {
                "ExistingImage1.jpg"
            };

            string[] destinationSubDirectory1FileNames = new string[]
            {
                "ExistingImage2.jpg",
                "ExistingImage3.jpg"
            };

            string[] destinationSubDirectory2FileNames = new string[]
            {
                "ExistingImage4.jpg",
                "ExistingImage5.jpg"
            };

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            Mock<IAssetRepository> assetRepositoryMock = new();
            Mock<ISyncAssetsConfigurationRepository> syncAssetsConfigurationRepositoryMock = new();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new();
            Mock<IStorageService> storageServiceMock = new();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new();

            syncAssetsConfigurationRepositoryMock.Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.GetSubDirectories(destinationDirectory)).Returns(
                new List<DirectoryInfo>
                {
                    new DirectoryInfo(destinationSubDirectory1),
                    new DirectoryInfo(destinationSubDirectory2)
                });

            storageServiceMock.Setup(s => s.GetRecursiveSubDirectories(destinationDirectory)).Returns(
                new List<DirectoryInfo>
                {
                    new DirectoryInfo(destinationSubDirectory1),
                    new DirectoryInfo(destinationSubDirectory2)
                });

            storageServiceMock.Setup(s => s.GetFileNames(sourceDirectory))
                .Returns(sourceFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationDirectory))
                .Returns(destinationFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationSubDirectory1))
                .Returns(destinationSubDirectory1FileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationSubDirectory2))
                .Returns(destinationSubDirectory2FileNames);

            storageServiceMock.Setup(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()))
                .Returns(true);

            SyncAssetsService syncAssetsService = new(
                assetRepositoryMock.Object,
                syncAssetsConfigurationRepositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer(storageServiceMock.Object));

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            syncAssetsConfigurationRepositoryMock.Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(destinationDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(destinationSubDirectory1), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(destinationSubDirectory2), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage2.jpg", @"C:\Images\MyGame\NewImage2.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage3.jpg", @"C:\Images\MyGame\NewImage3.jpg"), Times.Once);
            result.Should().ContainSingle();
            result[0].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame");
            result[0].SyncedImages.Should().Be(3);
            result[0].Message.Should().Be(@"3 images synced from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.");
            statusChanges.Should().HaveCount(3);
            statusChanges[0].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage1.jpg' => 'C:\Images\MyGame\NewImage1.jpg'");
            statusChanges[1].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage2.jpg' => 'C:\Images\MyGame\NewImage2.jpg'");
            statusChanges[2].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage3.jpg' => 'C:\Images\MyGame\NewImage3.jpg'");
        }

        [Fact]
        public async void SyncAssetsSourceNotEmptyDestinationNotEmptySomeSourceImagesInTargetSubDirectoriesMultipleLevelsWithoutSourceSubDirectories()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";
            string destinationSubDirectory1 = @"C:\Images\MyGame\SubDirectory1";
            string destinationSubSubDirectory1 = @"C:\Images\MyGame\SubDirectory1\AnotherSubDirectory";
            string destinationSubDirectory2 = @"C:\Images\MyGame\SubDirectory2";

            string[] sourceFileNames = new string[]
            {
                "ExistingImage1.jpg",
                "ExistingImage2.jpg",
                "ExistingImage3.jpg",
                "ExistingImage4.jpg",
                "ExistingImage5.jpg",
                "ExistingImage6.jpg",
                "ExistingImage7.jpg",
                "NewImage1.jpg",
                "NewImage2.jpg",
                "NewImage3.jpg"
            };

            string[] destinationFileNames = new string[]
            {
                "ExistingImage1.jpg"
            };

            string[] destinationSubDirectory1FileNames = new string[]
            {
                "ExistingImage2.jpg",
                "ExistingImage3.jpg"
            };

            string[] destinationSubSubDirectory1FileNames = new string[]
            {
                "ExistingImage4.jpg",
                "ExistingImage5.jpg"
            };

            string[] destinationSubDirectory2FileNames = new string[]
            {
                "ExistingImage6.jpg",
                "ExistingImage7.jpg"
            };

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            Mock<IAssetRepository> assetRepositoryMock = new();
            Mock<ISyncAssetsConfigurationRepository> syncAssetsConfigurationRepositoryMock = new();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new();
            Mock<IStorageService> storageServiceMock = new();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new();

            syncAssetsConfigurationRepositoryMock.Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.GetSubDirectories(destinationDirectory)).Returns(
                new List<DirectoryInfo>
                {
                    new DirectoryInfo(destinationSubDirectory1),
                    new DirectoryInfo(destinationSubDirectory2)
                });

            storageServiceMock.Setup(s => s.GetSubDirectories(destinationSubDirectory1)).Returns(
                new List<DirectoryInfo>
                {
                    new DirectoryInfo(destinationSubSubDirectory1)
                });

            storageServiceMock.Setup(s => s.GetRecursiveSubDirectories(destinationDirectory)).Returns(
                new List<DirectoryInfo>
                {
                    new DirectoryInfo(destinationSubDirectory1),
                    new DirectoryInfo(destinationSubSubDirectory1),
                    new DirectoryInfo(destinationSubDirectory2)
                });

            storageServiceMock.Setup(s => s.GetFileNames(sourceDirectory))
                .Returns(sourceFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationDirectory))
                .Returns(destinationFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationSubDirectory1))
                .Returns(destinationSubDirectory1FileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationSubSubDirectory1))
                .Returns(destinationSubSubDirectory1FileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationSubDirectory2))
                .Returns(destinationSubDirectory2FileNames);

            storageServiceMock.Setup(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()))
                .Returns(true);

            SyncAssetsService syncAssetsService = new(
                assetRepositoryMock.Object,
                syncAssetsConfigurationRepositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer(storageServiceMock.Object));

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            syncAssetsConfigurationRepositoryMock.Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(destinationDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(destinationSubDirectory1), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(destinationSubDirectory2), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage2.jpg", @"C:\Images\MyGame\NewImage2.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage3.jpg", @"C:\Images\MyGame\NewImage3.jpg"), Times.Once);
            result.Should().ContainSingle();
            result[0].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame");
            result[0].SyncedImages.Should().Be(3);
            result[0].Message.Should().Be(@"3 images synced from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.");
            statusChanges.Should().HaveCount(3);
            statusChanges[0].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage1.jpg' => 'C:\Images\MyGame\NewImage1.jpg'");
            statusChanges[1].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage2.jpg' => 'C:\Images\MyGame\NewImage2.jpg'");
            statusChanges[2].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage3.jpg' => 'C:\Images\MyGame\NewImage3.jpg'");
        }

        [Fact]
        public async void SyncAssetsSourceNotEmptyDestinationNotEmptySomeSourceImagesInTargetSubDirectoriesWithSourceSubDirectories()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string sourceSubDirectory = @"C:\MyGame\Screenshots\SubDirectory";
            string destinationDirectory = @"C:\Images\MyGame";
            string destinationSubDirectory1 = @"C:\Images\MyGame\SubDirectory1";
            string destinationSubDirectory2 = @"C:\Images\MyGame\SubDirectory2";

            string[] sourceFileNames = new string[]
            {
                "ExistingImage1.jpg",
                "ExistingImage2.jpg",
                "ExistingImage3.jpg",
                "ExistingImage4.jpg",
                "ExistingImage5.jpg",
                "NewImage1.jpg",
                "NewImage2.jpg",
                "NewImage3.jpg"
            };

            string[] sourceSubDirectoryFileNames = new string[]
            {
                "NewImage4.jpg",
                "NewImage5.jpg"
            };

            string[] destinationFileNames = new string[]
            {
                "ExistingImage1.jpg"
            };

            string[] destinationSubDirectory1FileNames = new string[]
            {
                "ExistingImage2.jpg",
                "ExistingImage3.jpg"
            };

            string[] destinationSubDirectory2FileNames = new string[]
            {
                "ExistingImage4.jpg",
                "ExistingImage5.jpg"
            };

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory,
                    IncludeSubFolders = true
                });

            Mock<IAssetRepository> assetRepositoryMock = new();
            Mock<ISyncAssetsConfigurationRepository> syncAssetsConfigurationRepositoryMock = new();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new();
            Mock<IStorageService> storageServiceMock = new();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new();

            syncAssetsConfigurationRepositoryMock.Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(sourceSubDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.GetSubDirectories(sourceDirectory)).Returns(
                new List<DirectoryInfo>
                {
                    new DirectoryInfo(sourceSubDirectory)
                });

            storageServiceMock.Setup(s => s.GetSubDirectories(sourceSubDirectory)).Returns(new List<DirectoryInfo>());

            storageServiceMock.Setup(s => s.GetSubDirectories(destinationDirectory)).Returns(
                new List<DirectoryInfo>
                {
                    new DirectoryInfo(destinationSubDirectory1),
                    new DirectoryInfo(destinationSubDirectory2)
                });

            storageServiceMock.Setup(s => s.GetRecursiveSubDirectories(destinationDirectory)).Returns(
                new List<DirectoryInfo>
                {
                    new DirectoryInfo(destinationSubDirectory1),
                    new DirectoryInfo(destinationSubDirectory2)
                });

            storageServiceMock.Setup(s => s.GetFileNames(sourceDirectory))
                .Returns(sourceFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(sourceSubDirectory))
                .Returns(sourceSubDirectoryFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationDirectory))
                .Returns(destinationFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationSubDirectory1))
                .Returns(destinationSubDirectory1FileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationSubDirectory2))
                .Returns(destinationSubDirectory2FileNames);

            storageServiceMock.Setup(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()))
                .Returns(true);

            SyncAssetsService syncAssetsService = new(
                assetRepositoryMock.Object,
                syncAssetsConfigurationRepositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer(storageServiceMock.Object));

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            syncAssetsConfigurationRepositoryMock.Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceSubDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(destinationDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(destinationSubDirectory1), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(destinationSubDirectory2), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage2.jpg", @"C:\Images\MyGame\NewImage2.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage3.jpg", @"C:\Images\MyGame\NewImage3.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\SubDirectory\NewImage4.jpg", @"C:\Images\MyGame\SubDirectory\NewImage4.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\SubDirectory\NewImage5.jpg", @"C:\Images\MyGame\SubDirectory\NewImage5.jpg"), Times.Once);
            result.Should().HaveCount(2);
            result[0].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame");
            result[0].SyncedImages.Should().Be(3);
            result[0].Message.Should().Be(@"3 images synced from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.");
            result[1].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots\SubDirectory");
            result[1].DestinationDirectory.Should().Be(@"C:\Images\MyGame\SubDirectory");
            result[1].SyncedImages.Should().Be(2);
            result[1].Message.Should().Be(@"2 images synced from 'C:\MyGame\Screenshots\SubDirectory' to 'C:\Images\MyGame\SubDirectory'.");
            statusChanges.Should().HaveCount(5);
            statusChanges[0].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage1.jpg' => 'C:\Images\MyGame\NewImage1.jpg'");
            statusChanges[1].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage2.jpg' => 'C:\Images\MyGame\NewImage2.jpg'");
            statusChanges[2].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage3.jpg' => 'C:\Images\MyGame\NewImage3.jpg'");
            statusChanges[3].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\SubDirectory\NewImage4.jpg' => 'C:\Images\MyGame\SubDirectory\NewImage4.jpg'");
            statusChanges[4].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\SubDirectory\NewImage5.jpg' => 'C:\Images\MyGame\SubDirectory\NewImage5.jpg'");
        }

        [Fact]
        public async void SyncAssetsSourceNotEmptyDestinationNotEmptyAllSourceImagesInTargetSubDirectories()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";
            string destinationSubDirectory1 = @"C:\Images\MyGame\SubDirectory1";
            string destinationSubDirectory2 = @"C:\Images\MyGame\SubDirectory2";

            string[] sourceFileNames = new string[]
            {
                "ExistingImage1.jpg",
                "ExistingImage2.jpg",
                "ExistingImage3.jpg",
                "ExistingImage4.jpg",
                "ExistingImage5.jpg"
            };

            string[] destinationFileNames = new string[]
            {
                "ExistingImage1.jpg"
            };

            string[] destinationSubDirectory1FileNames = new string[]
            {
                "ExistingImage2.jpg",
                "ExistingImage3.jpg"
            };

            string[] destinationSubDirectory2FileNames = new string[]
            {
                "ExistingImage4.jpg",
                "ExistingImage5.jpg"
            };

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            Mock<IAssetRepository> assetRepositoryMock = new();
            Mock<ISyncAssetsConfigurationRepository> syncAssetsConfigurationRepositoryMock = new();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new();
            Mock<IStorageService> storageServiceMock = new();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new();

            syncAssetsConfigurationRepositoryMock.Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.GetSubDirectories(destinationDirectory)).Returns(
                new List<DirectoryInfo>
                {
                    new DirectoryInfo(destinationSubDirectory1),
                    new DirectoryInfo(destinationSubDirectory2)
                });

            storageServiceMock.Setup(s => s.GetRecursiveSubDirectories(destinationDirectory)).Returns(
                new List<DirectoryInfo>
                {
                    new DirectoryInfo(destinationSubDirectory1),
                    new DirectoryInfo(destinationSubDirectory2)
                });

            storageServiceMock.Setup(s => s.GetFileNames(sourceDirectory))
                .Returns(sourceFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationDirectory))
                .Returns(destinationFileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationSubDirectory1))
                .Returns(destinationSubDirectory1FileNames);

            storageServiceMock.Setup(s => s.GetFileNames(destinationSubDirectory2))
                .Returns(destinationSubDirectory2FileNames);

            storageServiceMock.Setup(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()))
                .Returns(true);

            SyncAssetsService syncAssetsService = new(
                assetRepositoryMock.Object,
                syncAssetsConfigurationRepositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer(storageServiceMock.Object));

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            syncAssetsConfigurationRepositoryMock.Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(destinationDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(destinationSubDirectory1), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(destinationSubDirectory2), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()), Times.Never);
            result.Should().ContainSingle();
            result[0].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame");
            result[0].SyncedImages.Should().Be(0);
            result[0].Message.Should().Be(@"No images synced from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.");
            statusChanges.Should().BeEmpty();
        }

        [Fact]
        public void ValidateAllValidDefinitionsTest()
        {
            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyFirstGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MyFirstGame"
                });

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MySecondGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MySecondGame"
                });

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                });

            syncConfiguration.Validate();

            syncConfiguration.Definitions.Should().HaveCount(3);
            syncConfiguration.Definitions[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            syncConfiguration.Definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            syncConfiguration.Definitions[1].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            syncConfiguration.Definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
            syncConfiguration.Definitions[2].SourceDirectory.Should().Be(@"\\MyServer\Images");
            syncConfiguration.Definitions[2].DestinationDirectory.Should().Be(@"C:\Images");
        }

        [Fact]
        public void ValidateOneInvalidDefinitionTest()
        {
            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyFirstGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MyFirstGame"
                });

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MySecondGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MySecondGame"
                });

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"http://www.some-site.com",
                    DestinationDirectory = @"ftp://some-location.com"
                });

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"InvalidValue",
                    DestinationDirectory = @"InvalidValue"
                });

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"Invalid@Value.com",
                    DestinationDirectory = @"Invalid@Value.com"
                });

            syncConfiguration.Validate();

            syncConfiguration.Definitions.Should().HaveCount(2);
            syncConfiguration.Definitions[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            syncConfiguration.Definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            syncConfiguration.Definitions[1].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            syncConfiguration.Definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
        }

        [Fact]
        public void NormalizeTest()
        {
            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyFirstGame\Screenshots",
                    DestinationDirectory = @"C:\Images\\\MyFirstGame\"
                });

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\\\MySecondGame\Screenshots\\",
                    DestinationDirectory = @"C:\Images\MySecondGame\\\\\"
                });

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Screenshots\\\",
                    DestinationDirectory = @"C:\Images\\\\\"
                });

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\\\\MyServer\Screenshots\\\",
                    DestinationDirectory = @"C:\Images\\\\\"
                });

            syncConfiguration.Normalize();

            syncConfiguration.Definitions.Should().HaveCount(4);
            syncConfiguration.Definitions[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            syncConfiguration.Definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            syncConfiguration.Definitions[1].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            syncConfiguration.Definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
            syncConfiguration.Definitions[2].SourceDirectory.Should().Be(@"\\MyServer\Screenshots");
            syncConfiguration.Definitions[2].DestinationDirectory.Should().Be(@"C:\Images");
            syncConfiguration.Definitions[3].SourceDirectory.Should().Be(@"\\MyServer\Screenshots");
            syncConfiguration.Definitions[3].DestinationDirectory.Should().Be(@"C:\Images");
        }

        [Fact]
        public async void SyncAssetsInexistentSourceDirectory()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            Mock<IAssetRepository> assetRepositoryMock = new();
            Mock<ISyncAssetsConfigurationRepository> syncAssetsConfigurationRepositoryMock = new();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new();
            Mock<IStorageService> storageServiceMock = new();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new();

            syncAssetsConfigurationRepositoryMock.Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(false);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(true);

            SyncAssetsService syncAssetsService = new(
                assetRepositoryMock.Object,
                syncAssetsConfigurationRepositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer(storageServiceMock.Object));

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            syncAssetsConfigurationRepositoryMock.Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Never);
            storageServiceMock.Verify(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()), Times.Never);
            result.Should().ContainSingle();
            result[0].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame");
            result[0].SyncedImages.Should().Be(0);
            result[0].Message.Should().Be(@"Source directory 'C:\MyGame\Screenshots' not found.");
            statusChanges.Should().BeEmpty();
        }

        [Fact]
        public async void SyncAssetsInexistentDestinationDirectory()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"C:\Images\MyGame";

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            Mock<IAssetRepository> assetRepositoryMock = new();
            Mock<ISyncAssetsConfigurationRepository> syncAssetsConfigurationRepositoryMock = new();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new();
            Mock<IStorageService> storageServiceMock = new();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new();

            syncAssetsConfigurationRepositoryMock.Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(false);

            SyncAssetsService syncAssetsService = new(
                assetRepositoryMock.Object,
                syncAssetsConfigurationRepositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer(storageServiceMock.Object));

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            syncAssetsConfigurationRepositoryMock.Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.CreateDirectory(destinationDirectory), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()), Times.Never);
            result.Should().ContainSingle();
            result[0].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame");
            result[0].SyncedImages.Should().Be(0);
            result[0].Message.Should().Be(@"No images synced from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.");
            statusChanges.Should().BeEmpty();
        }

        [Fact]
        public async void SyncAssetsInaccessibleDestinationDirectory()
        {
            string sourceDirectory = @"C:\MyGame\Screenshots";
            string destinationDirectory = @"\\MyServer\MyGame";

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory
                });

            Mock<IAssetRepository> assetRepositoryMock = new();
            Mock<ISyncAssetsConfigurationRepository> syncAssetsConfigurationRepositoryMock = new();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new();
            Mock<IStorageService> storageServiceMock = new();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new();

            syncAssetsConfigurationRepositoryMock.Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

            storageServiceMock.Setup(s => s.FolderExists(sourceDirectory))
                .Returns(true);

            storageServiceMock.Setup(s => s.FolderExists(destinationDirectory))
                .Returns(false);

            storageServiceMock.Setup(s => s.CreateDirectory(destinationDirectory))
                .Throws(new DirectoryNotFoundException());

            SyncAssetsService syncAssetsService = new(
                assetRepositoryMock.Object,
                syncAssetsConfigurationRepositoryMock.Object,
                storageServiceMock.Object,
                new DirectoryComparer(storageServiceMock.Object));

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            syncAssetsConfigurationRepositoryMock.Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Never);
            storageServiceMock.Verify(s => s.CopyImage(It.IsAny<string>(), It.IsAny<string>()), Times.Never);
            result.Should().ContainSingle();
            result[0].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"\\MyServer\MyGame");
            result[0].SyncedImages.Should().Be(0);
            result[0].Message.Should().Be(@"Destination directory '\\MyServer\MyGame' not found.");
            statusChanges.Should().BeEmpty();
        }

        [Fact]
        public async void SyncAssetsSourceNotEmptyDestinationEmptyIncludingSubFoldersTest()
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

            SyncAssetsConfiguration syncConfiguration = new();

            syncConfiguration.Definitions.Add(
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = sourceDirectory,
                    DestinationDirectory = destinationDirectory,
                    IncludeSubFolders = true
                });

            Mock<IAssetRepository> assetRepositoryMock = new();
            Mock<ISyncAssetsConfigurationRepository> syncAssetsConfigurationRepository = new();
            Mock<IAssetHashCalculatorService> hashCalculatorMock = new();
            Mock<IStorageService> storageServiceMock = new();
            Mock<IUserConfigurationService> userConfigurationServiceMock = new();

            syncAssetsConfigurationRepository.Setup(r => r.GetSyncAssetsConfiguration())
                .Returns(syncConfiguration);

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

            SyncAssetsService syncAssetsService = new(
                assetRepositoryMock.Object,
                syncAssetsConfigurationRepository.Object,
                storageServiceMock.Object,
                new DirectoryComparer(storageServiceMock.Object));

            var statusChanges = new List<ProcessStatusChangedCallbackEventArgs>();

            var result = await syncAssetsService.ExecuteAsync(e => statusChanges.Add(e));

            syncAssetsConfigurationRepository.Verify(r => r.GetSyncAssetsConfiguration(), Times.Once);
            storageServiceMock.Verify(s => s.GetFileNames(sourceDirectory), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage1.jpg", @"C:\Images\MyGame\NewImage1.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage2.jpg", @"C:\Images\MyGame\NewImage2.jpg"), Times.Once);
            storageServiceMock.Verify(s => s.CopyImage(@"C:\MyGame\Screenshots\NewImage3.jpg", @"C:\Images\MyGame\NewImage3.jpg"), Times.Once);
            result.Should().HaveCount(2);
            result[0].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots");
            result[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame");
            result[0].SyncedImages.Should().Be(3);
            result[0].Message.Should().Be(@"3 images synced from 'C:\MyGame\Screenshots' to 'C:\Images\MyGame'.");
            result[1].SourceDirectory.Should().Be(@"C:\MyGame\Screenshots\SubDirectory");
            result[1].DestinationDirectory.Should().Be(@"C:\Images\MyGame\SubDirectory");
            result[1].SyncedImages.Should().Be(0);
            result[1].Message.Should().Be(@"No images synced from 'C:\MyGame\Screenshots\SubDirectory' to 'C:\Images\MyGame\SubDirectory'.");
            statusChanges.Should().HaveCount(3);
            statusChanges[0].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage1.jpg' => 'C:\Images\MyGame\NewImage1.jpg'");
            statusChanges[1].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage2.jpg' => 'C:\Images\MyGame\NewImage2.jpg'");
            statusChanges[2].NewStatus.Should().Be(@$"'C:\MyGame\Screenshots\NewImage3.jpg' => 'C:\Images\MyGame\NewImage3.jpg'");
        }
    }
}
