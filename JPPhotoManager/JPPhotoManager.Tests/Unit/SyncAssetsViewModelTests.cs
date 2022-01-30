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
    public class SyncAssetsViewModelTests
    {
        [Fact]
        public void ViewModelTest()
        {
            var definitions = new ObservableCollection<SyncAssetsDirectoriesDefinition>
            {
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame1\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame1"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame2\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame2"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame3\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame3"
                }
            };

            Mock<IApplication> mock = new();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            SyncAssetsViewModel viewModel = new(mock.Object)
            {
                Definitions = definitions
            };

            viewModel.Definitions.Should().HaveCount(3);
            viewModel.Definitions[0].SourceDirectory.Should().Be(@"C:\MyGame1\Screenshots");
            viewModel.Definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame1");
            viewModel.Definitions[1].SourceDirectory.Should().Be(@"C:\MyGame2\Screenshots");
            viewModel.Definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MyGame2");
            viewModel.Definitions[2].SourceDirectory.Should().Be(@"C:\MyGame3\Screenshots");
            viewModel.Definitions[2].DestinationDirectory.Should().Be(@"C:\Images\MyGame3");
        }

        [Fact]
        public void DeleteDefinitionTest()
        {
            var definitions = new ObservableCollection<SyncAssetsDirectoriesDefinition>
            {
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame1\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame1"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame2\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame2"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame3\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame3"
                }
            };

            Mock<IApplication> mock = new();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            SyncAssetsViewModel viewModel = new(mock.Object)
            {
                Definitions = definitions
            };

            viewModel.DeleteDefinition(viewModel.Definitions[1]);

            viewModel.Definitions.Should().HaveCount(2);
            viewModel.Definitions[0].SourceDirectory.Should().Be(@"C:\MyGame1\Screenshots");
            viewModel.Definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame1");
            viewModel.Definitions[1].SourceDirectory.Should().Be(@"C:\MyGame3\Screenshots");
            viewModel.Definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MyGame3");
        }

        [Fact]
        public void AdvanceStepTest()
        {
            var results = new ObservableCollection<SyncAssetsResult>
            {
                new SyncAssetsResult
                {
                    SourceDirectory = @"C:\MyGame1\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame1",
                    SyncedImages = 2,
                    Message = "2 images synced"
                }
            };

            Mock<IApplication> mock = new();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");
            SyncAssetsViewModel viewModel = new(mock.Object);

            viewModel.Step.Should().Be(ProcessStepEnum.ViewDescription);
            viewModel.CanConfigure.Should().BeFalse();
            viewModel.CanViewResults.Should().BeFalse();
            viewModel.ResultsVisible.Should().Be(Visibility.Hidden);

            viewModel.AdvanceStep();

            viewModel.Step.Should().Be(ProcessStepEnum.Configure);
            viewModel.CanConfigure.Should().BeTrue();
            viewModel.CanViewResults.Should().BeFalse();
            viewModel.ResultsVisible.Should().Be(Visibility.Hidden);

            viewModel.AdvanceStep();
            viewModel.Results = results;

            viewModel.Step.Should().Be(ProcessStepEnum.Run);
            viewModel.CanConfigure.Should().BeFalse();
            viewModel.CanViewResults.Should().BeTrue();
            viewModel.ResultsVisible.Should().Be(Visibility.Hidden);

            viewModel.AdvanceStep();

            viewModel.Step.Should().Be(ProcessStepEnum.ViewResults);
            viewModel.CanConfigure.Should().BeFalse();
            viewModel.CanViewResults.Should().BeFalse();
            viewModel.ResultsVisible.Should().Be(Visibility.Visible);

            viewModel.AdvanceStep();

            viewModel.Step.Should().Be(ProcessStepEnum.ViewResults);
            viewModel.CanConfigure.Should().BeFalse();
            viewModel.CanViewResults.Should().BeFalse();
            viewModel.ResultsVisible.Should().Be(Visibility.Visible);
        }

        [Fact]
        public void MoveUpDefinitionTest()
        {
            var definitions = new ObservableCollection<SyncAssetsDirectoriesDefinition>
            {
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame1\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame1"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame2\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame2"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame3\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame3"
                }
            };

            Mock<IApplication> mock = new();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            SyncAssetsViewModel viewModel = new(mock.Object)
            {
                Definitions = definitions
            };

            viewModel.MoveUpDefinition(viewModel.Definitions[1]);

            viewModel.Definitions.Should().HaveCount(3);
            viewModel.Definitions[0].SourceDirectory.Should().Be(@"C:\MyGame2\Screenshots");
            viewModel.Definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame2");
            viewModel.Definitions[1].SourceDirectory.Should().Be(@"C:\MyGame1\Screenshots");
            viewModel.Definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MyGame1");
            viewModel.Definitions[2].SourceDirectory.Should().Be(@"C:\MyGame3\Screenshots");
            viewModel.Definitions[2].DestinationDirectory.Should().Be(@"C:\Images\MyGame3");
        }

        [Fact]
        public void MoveDownDefinitionTest()
        {
            var definitions = new ObservableCollection<SyncAssetsDirectoriesDefinition>
            {
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame1\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame1"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame2\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame2"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyGame3\Screenshots",
                    DestinationDirectory = @"C:\Images\MyGame3"
                }
            };

            Mock<IApplication> mock = new();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            SyncAssetsViewModel viewModel = new(mock.Object)
            {
                Definitions = definitions
            };

            viewModel.MoveDownDefinition(viewModel.Definitions[1]);

            viewModel.Definitions.Should().HaveCount(3);
            viewModel.Definitions[0].SourceDirectory.Should().Be(@"C:\MyGame1\Screenshots");
            viewModel.Definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyGame1");
            viewModel.Definitions[1].SourceDirectory.Should().Be(@"C:\MyGame3\Screenshots");
            viewModel.Definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MyGame3");
            viewModel.Definitions[2].SourceDirectory.Should().Be(@"C:\MyGame2\Screenshots");
            viewModel.Definitions[2].DestinationDirectory.Should().Be(@"C:\Images\MyGame2");
        }

        [Fact]
        public void NotifySyncImageTest()
        {
            Mock<IApplication> mock = new();
            mock.Setup(app => app.GetInitialFolder()).Returns(@"C:\");

            SyncAssetsViewModel viewModel = new(mock.Object);

            viewModel.NotifyProcessStatusChanged(new ProcessStatusChangedCallbackEventArgs
            {
                NewStatus = @"2 images synced from C:\MyGame1\Screenshots to C:\Images\MyGame1"
            });

            viewModel.ProcessStatusMessages.Should().ContainSingle();
            viewModel.ProcessStatusMessages[0].Should().Be(@"2 images synced from C:\MyGame1\Screenshots to C:\Images\MyGame1");

            viewModel.NotifyProcessStatusChanged(new ProcessStatusChangedCallbackEventArgs
            {
                NewStatus = @"2 images synced from C:\MyGame2\Screenshots to C:\Images\MyGame2"
            });

            viewModel.ProcessStatusMessages.Should().HaveCount(2);
            viewModel.ProcessStatusMessages[1].Should().Be(@"2 images synced from C:\MyGame2\Screenshots to C:\Images\MyGame2");

            viewModel.NotifyProcessStatusChanged(new ProcessStatusChangedCallbackEventArgs
            {
                NewStatus = @"2 images synced from C:\MyGame3\Screenshots to C:\Images\MyGame3"
            });

            viewModel.ProcessStatusMessages.Should().HaveCount(3);
            viewModel.ProcessStatusMessages[2].Should().Be(@"2 images synced from C:\MyGame3\Screenshots to C:\Images\MyGame3");
        }
    }
}
