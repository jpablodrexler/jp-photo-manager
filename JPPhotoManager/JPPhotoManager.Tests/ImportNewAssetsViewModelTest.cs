using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using Moq;
using System.Collections.ObjectModel;
using System.Windows;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class ImportNewAssetsViewModelTest
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
            
            Mock<IApplication> mock = new Mock<IApplication>();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            ImportNewAssetsViewModel viewModel = new ImportNewAssetsViewModel(mock.Object)
            {
                Imports = imports
            };

            Assert.Equal(3, viewModel.Imports.Count);
            Assert.Equal(@"C:\MyGame1\Screenshots", viewModel.Imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame1", viewModel.Imports[0].DestinationDirectory);
            Assert.Equal(@"C:\MyGame2\Screenshots", viewModel.Imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame2", viewModel.Imports[1].DestinationDirectory);
            Assert.Equal(@"C:\MyGame3\Screenshots", viewModel.Imports[2].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame3", viewModel.Imports[2].DestinationDirectory);
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

            Mock<IApplication> mock = new Mock<IApplication>();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            ImportNewAssetsViewModel viewModel = new ImportNewAssetsViewModel(mock.Object)
            {
                Imports = imports
            };

            viewModel.DeleteDefinition(viewModel.Imports[1]);

            Assert.Equal(2, viewModel.Imports.Count);
            Assert.Equal(@"C:\MyGame1\Screenshots", viewModel.Imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame1", viewModel.Imports[0].DestinationDirectory);
            Assert.Equal(@"C:\MyGame3\Screenshots", viewModel.Imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame3", viewModel.Imports[1].DestinationDirectory);
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

            Mock<IApplication> mock = new Mock<IApplication>();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");
            ImportNewAssetsViewModel viewModel = new ImportNewAssetsViewModel(mock.Object);

            Assert.Equal(ImportNewAssetsStepEnum.Configure, viewModel.Step);
            Assert.True(viewModel.CanConfigure);
            Assert.False(viewModel.CanViewResults);
            Assert.Equal(Visibility.Visible, viewModel.InputVisible);
            Assert.Equal(Visibility.Hidden, viewModel.ResultsVisible);

            viewModel.AdvanceStep();
            viewModel.Results = results;

            Assert.Equal(ImportNewAssetsStepEnum.Import, viewModel.Step);
            Assert.False(viewModel.CanConfigure);
            Assert.True(viewModel.CanViewResults);
            Assert.Equal(Visibility.Visible, viewModel.InputVisible);
            Assert.Equal(Visibility.Hidden, viewModel.ResultsVisible);

            viewModel.AdvanceStep();
            
            Assert.Equal(ImportNewAssetsStepEnum.ViewResults, viewModel.Step);
            Assert.False(viewModel.CanConfigure);
            Assert.False(viewModel.CanViewResults);
            Assert.Equal(Visibility.Hidden, viewModel.InputVisible);
            Assert.Equal(Visibility.Visible, viewModel.ResultsVisible);

            viewModel.AdvanceStep();

            Assert.Equal(ImportNewAssetsStepEnum.ViewResults, viewModel.Step);
            Assert.False(viewModel.CanConfigure);
            Assert.False(viewModel.CanViewResults);
            Assert.Equal(Visibility.Hidden, viewModel.InputVisible);
            Assert.Equal(Visibility.Visible, viewModel.ResultsVisible);
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

            Mock<IApplication> mock = new Mock<IApplication>();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            ImportNewAssetsViewModel viewModel = new ImportNewAssetsViewModel(mock.Object)
            {
                Imports = imports
            };

            viewModel.MoveUpDefinition(viewModel.Imports[1]);

            Assert.Equal(3, viewModel.Imports.Count);
            Assert.Equal(@"C:\MyGame2\Screenshots", viewModel.Imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame2", viewModel.Imports[0].DestinationDirectory);
            Assert.Equal(@"C:\MyGame1\Screenshots", viewModel.Imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame1", viewModel.Imports[1].DestinationDirectory);
            Assert.Equal(@"C:\MyGame3\Screenshots", viewModel.Imports[2].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame3", viewModel.Imports[2].DestinationDirectory);
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

            Mock<IApplication> mock = new Mock<IApplication>();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            ImportNewAssetsViewModel viewModel = new ImportNewAssetsViewModel(mock.Object)
            {
                Imports = imports
            };

            viewModel.MoveDownDefinition(viewModel.Imports[1]);

            Assert.Equal(3, viewModel.Imports.Count);
            Assert.Equal(@"C:\MyGame1\Screenshots", viewModel.Imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame1", viewModel.Imports[0].DestinationDirectory);
            Assert.Equal(@"C:\MyGame3\Screenshots", viewModel.Imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame3", viewModel.Imports[1].DestinationDirectory);
            Assert.Equal(@"C:\MyGame2\Screenshots", viewModel.Imports[2].SourceDirectory);
            Assert.Equal(@"C:\Images\MyGame2", viewModel.Imports[2].DestinationDirectory);
        }

        [Fact]
        public void NotifyImportImageTest()
        {
            Mock<IApplication> mock = new Mock<IApplication>();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            ImportNewAssetsViewModel viewModel = new ImportNewAssetsViewModel(mock.Object);

            viewModel.NotifyImageImported(new StatusChangeCallbackEventArgs
            {
                NewStatus = @"2 images imported from C:\MyGame1\Screenshots to C:\Images\MyGame1"
            });
            
            Assert.Single(viewModel.StatusMessages);
            Assert.Equal(@"2 images imported from C:\MyGame1\Screenshots to C:\Images\MyGame1", viewModel.StatusMessages[0]);

            viewModel.NotifyImageImported(new StatusChangeCallbackEventArgs
            {
                NewStatus = @"2 images imported from C:\MyGame2\Screenshots to C:\Images\MyGame2"
            });

            Assert.Equal(2, viewModel.StatusMessages.Count);
            Assert.Equal(@"2 images imported from C:\MyGame2\Screenshots to C:\Images\MyGame2", viewModel.StatusMessages[1]);

            viewModel.NotifyImageImported(new StatusChangeCallbackEventArgs
            {
                NewStatus = @"2 images imported from C:\MyGame3\Screenshots to C:\Images\MyGame3"
            });

            Assert.Equal(3, viewModel.StatusMessages.Count);
            Assert.Equal(@"2 images imported from C:\MyGame3\Screenshots to C:\Images\MyGame3", viewModel.StatusMessages[2]);
        }
    }
}
