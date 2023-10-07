using FluentAssertions;
using JPPhotoManager.Domain.Entities;
using JPPhotoManager.UI.ViewModels;
using System.Collections.ObjectModel;
using Xunit;

namespace JPPhotoManager.Tests.Unit.UI.ViewModels
{
    public class ViewModelExtensionsTests
    {
        [Fact]
        public void MoveUpSecondDefinitionTest()
        {
            var definitions = new ObservableCollection<SyncAssetsDirectoriesDefinition>
            {
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyFirstGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MyFirstGame"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MySecondGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MySecondGame"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                }
            };

            definitions.MoveUp(definitions[1]);

            definitions.Should().HaveCount(3);
            definitions[0].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
            definitions[1].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            definitions[2].SourceDirectory.Should().Be(@"\\MyServer\Images");
            definitions[2].DestinationDirectory.Should().Be(@"C:\Images");
        }

        [Fact]
        public void MoveUpThirdDefinitionTest()
        {
            var definitions = new ObservableCollection<SyncAssetsDirectoriesDefinition>
            {
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyFirstGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MyFirstGame"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MySecondGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MySecondGame"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                }
            };

            definitions.MoveUp(definitions[2]);

            definitions.Should().HaveCount(3);
            definitions[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            definitions[1].SourceDirectory.Should().Be(@"\\MyServer\Images");
            definitions[1].DestinationDirectory.Should().Be(@"C:\Images");
            definitions[2].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            definitions[2].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
        }

        [Fact]
        public void MoveUpFirstDefinitionTest()
        {
            var definitions = new ObservableCollection<SyncAssetsDirectoriesDefinition>
            {
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyFirstGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MyFirstGame"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MySecondGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MySecondGame"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                }
            };

            definitions.MoveUp(definitions[0]);

            definitions.Should().HaveCount(3);
            definitions[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            definitions[1].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
            definitions[2].SourceDirectory.Should().Be(@"\\MyServer\Images");
            definitions[2].DestinationDirectory.Should().Be(@"C:\Images");
        }

        [Fact]
        public void MoveDownSecondDefinitionTest()
        {
            var definitions = new ObservableCollection<SyncAssetsDirectoriesDefinition>
            {
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyFirstGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MyFirstGame"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MySecondGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MySecondGame"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                }
            };

            definitions.MoveDown(definitions[1]);

            definitions.Should().HaveCount(3);
            definitions[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            definitions[1].SourceDirectory.Should().Be(@"\\MyServer\Images");
            definitions[1].DestinationDirectory.Should().Be(@"C:\Images");
            definitions[2].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            definitions[2].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
        }

        [Fact]
        public void MoveDownFirstDefinitionTest()
        {
            var definitions = new ObservableCollection<SyncAssetsDirectoriesDefinition>
            {
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyFirstGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MyFirstGame"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MySecondGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MySecondGame"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                }
            };

            definitions.MoveDown(definitions[0]);

            definitions.Should().HaveCount(3);
            definitions[0].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
            definitions[1].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            definitions[2].SourceDirectory.Should().Be(@"\\MyServer\Images");
            definitions[2].DestinationDirectory.Should().Be(@"C:\Images");
        }

        [Fact]
        public void MoveDownLastDefinitionTest()
        {
            var definitions = new ObservableCollection<SyncAssetsDirectoriesDefinition>
            {
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MyFirstGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MyFirstGame"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"C:\MySecondGame\Screenshots",
                    DestinationDirectory = @"C:\Images\MySecondGame"
                },
                new SyncAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                }
            };

            definitions.MoveDown(definitions[2]);

            definitions.Should().HaveCount(3);
            definitions[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            definitions[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            definitions[1].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            definitions[1].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
            definitions[2].SourceDirectory.Should().Be(@"\\MyServer\Images");
            definitions[2].DestinationDirectory.Should().Be(@"C:\Images");
        }
    }
}
