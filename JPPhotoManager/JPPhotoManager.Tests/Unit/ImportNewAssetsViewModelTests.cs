using FluentAssertions;
using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using Moq;
using System.Collections.ObjectModel;
using System.Windows;
using Xunit;

namespace JPPhotoManager.Tests.Unit
{
    public class ImportNewAssetsViewModelTests
    {
        [Fact]
        public void ViewModelTest()
        {
            var imports = new ObservableCollection<ImportNewAssetsDirectoriesDefinition>
            {
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame1\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame1"
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame2\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame2"
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame3\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame3"
                }
            };

            Mock<IApplication> mock = new();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            ImportNewAssetsViewModel viewModel = new(mock.Object)
            {
                Imports = imports
            };

            viewModel.Imports.Should().HaveCount(3);
            viewModel.Imports[0].SourceDirectory.Should().Be(@"C:\MyGame1\Screenshots");
            viewModel.Imports[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame1");
            viewModel.Imports[1].SourceDirectory.Should().Be(@"C:\MyGame2\Screenshots");
            viewModel.Imports[1].DestinationDirectory.Should().Be(@"C:\Images\MyGame2");
            viewModel.Imports[2].SourceDirectory.Should().Be(@"C:\MyGame3\Screenshots");
            viewModel.Imports[2].DestinationDirectory.Should().Be(@"C:\Images\MyGame3");
        }

        [Fact]
        public void DeleteDefinitionTest()
        {
            var imports = new ObservableCollection<ImportNewAssetsDirectoriesDefinition>
            {
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame1\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame1"
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame2\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame2"
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame3\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame3"
                }
            };

            Mock<IApplication> mock = new();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            ImportNewAssetsViewModel viewModel = new(mock.Object)
            {
                Imports = imports
            };

            viewModel.DeleteDefinition(viewModel.Imports[1]);

            viewModel.Imports.Should().HaveCount(2);
            viewModel.Imports[0].SourceDirectory.Should().Be(@"C:\MyGame1\Screenshots");
            viewModel.Imports[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame1");
            viewModel.Imports[1].SourceDirectory.Should().Be(@"C:\MyGame3\Screenshots");
            viewModel.Imports[1].DestinationDirectory.Should().Be(@"C:\Images\MyGame3");
        }

        [Fact]
        public void AdvanceStepTest()
        {
            var results = new ObservableCollection<ImportNewAssetsResult>
            {
                new ImportNewAssetsResult
                {
                    SourceDirectory = @"C:\MyGame1\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame1",
                    ImportedImages = 2,
                    Message = "2 images imported"
                }
            };

            Mock<IApplication> mock = new();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");
            ImportNewAssetsViewModel viewModel = new(mock.Object);

            viewModel.Step.Should().Be(ImportNewAssetsStepEnum.Configure);
            viewModel.CanConfigure.Should().BeTrue();
            viewModel.CanViewResults.Should().BeFalse();
            viewModel.InputVisible.Should().Be(Visibility.Visible);
            viewModel.ResultsVisible.Should().Be(Visibility.Hidden);

            viewModel.AdvanceStep();
            viewModel.Results = results;

            viewModel.Step.Should().Be(ImportNewAssetsStepEnum.Import);
            viewModel.CanConfigure.Should().BeFalse();
            viewModel.CanViewResults.Should().BeTrue();
            viewModel.InputVisible.Should().Be(Visibility.Visible);
            viewModel.ResultsVisible.Should().Be(Visibility.Hidden);

            viewModel.AdvanceStep();

            viewModel.Step.Should().Be(ImportNewAssetsStepEnum.ViewResults);
            viewModel.CanConfigure.Should().BeFalse();
            viewModel.CanViewResults.Should().BeFalse();
            viewModel.InputVisible.Should().Be(Visibility.Hidden);
            viewModel.ResultsVisible.Should().Be(Visibility.Visible);

            viewModel.AdvanceStep();

            viewModel.Step.Should().Be(ImportNewAssetsStepEnum.ViewResults);
            viewModel.CanConfigure.Should().BeFalse();
            viewModel.CanViewResults.Should().BeFalse();
            viewModel.InputVisible.Should().Be(Visibility.Hidden);
            viewModel.ResultsVisible.Should().Be(Visibility.Visible);
        }

        [Fact]
        public void MoveUpDefinitionTest()
        {
            var imports = new ObservableCollection<ImportNewAssetsDirectoriesDefinition>
            {
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame1\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame1"
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame2\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame2"
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame3\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame3"
                }
            };

            Mock<IApplication> mock = new();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            ImportNewAssetsViewModel viewModel = new(mock.Object)
            {
                Imports = imports
            };

            viewModel.MoveUpDefinition(viewModel.Imports[1]);

            viewModel.Imports.Should().HaveCount(3);
            viewModel.Imports[0].SourceDirectory.Should().Be(@"C:\MyGame2\Screenshots");
            viewModel.Imports[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame2");
            viewModel.Imports[1].SourceDirectory.Should().Be(@"C:\MyGame1\Screenshots");
            viewModel.Imports[1].DestinationDirectory.Should().Be(@"C:\Images\MyGame1");
            viewModel.Imports[2].SourceDirectory.Should().Be(@"C:\MyGame3\Screenshots");
            viewModel.Imports[2].DestinationDirectory.Should().Be(@"C:\Images\MyGame3");
        }

        [Fact]
        public void MoveDownDefinitionTest()
        {
            var imports = new ObservableCollection<ImportNewAssetsDirectoriesDefinition>
            {
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame1\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame1"
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame2\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame2"
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame3\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame3"
                }
            };

            Mock<IApplication> mock = new();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            ImportNewAssetsViewModel viewModel = new(mock.Object)
            {
                Imports = imports
            };

            viewModel.MoveDownDefinition(viewModel.Imports[1]);

            viewModel.Imports.Should().HaveCount(3);
            viewModel.Imports[0].SourceDirectory.Should().Be(@"C:\MyGame1\Screenshots");
            viewModel.Imports[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame1");
            viewModel.Imports[1].SourceDirectory.Should().Be(@"C:\MyGame3\Screenshots");
            viewModel.Imports[1].DestinationDirectory.Should().Be(@"C:\Images\MyGame3");
            viewModel.Imports[2].SourceDirectory.Should().Be(@"C:\MyGame2\Screenshots");
            viewModel.Imports[2].DestinationDirectory.Should().Be(@"C:\Images\MyGame2");
        }

        [Fact]
        public void NotifyImportImageTest()
        {
            Mock<IApplication> mock = new();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            ImportNewAssetsViewModel viewModel = new(mock.Object);

            viewModel.NotifyImageImported(new StatusChangeCallbackEventArgs
            {
                NewStatus = @"2 images imported from C:\MyGame1\Screenshots to C:\Images\MyGame1"
            });

            viewModel.StatusMessages.Should().ContainSingle();
            viewModel.StatusMessages[0].Should().Be(@"2 images imported from C:\MyGame1\Screenshots to C:\Images\MyGame1");

            viewModel.NotifyImageImported(new StatusChangeCallbackEventArgs
            {
                NewStatus = @"2 images imported from C:\MyGame2\Screenshots to C:\Images\MyGame2"
            });

            viewModel.StatusMessages.Should().HaveCount(2);
            viewModel.StatusMessages[1].Should().Be(@"2 images imported from C:\MyGame2\Screenshots to C:\Images\MyGame2");

            viewModel.NotifyImageImported(new StatusChangeCallbackEventArgs
            {
                NewStatus = @"2 images imported from C:\MyGame3\Screenshots to C:\Images\MyGame3"
            });

            viewModel.StatusMessages.Should().HaveCount(3);
            viewModel.StatusMessages[2].Should().Be(@"2 images imported from C:\MyGame3\Screenshots to C:\Images\MyGame3");
        }
    }
}
