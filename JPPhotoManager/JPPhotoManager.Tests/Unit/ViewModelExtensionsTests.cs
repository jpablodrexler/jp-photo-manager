using FluentAssertions;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using System.Collections.ObjectModel;
using Xunit;

namespace JPPhotoManager.Tests.Unit
{
    public class ViewModelExtensionsTests
    {
        [Fact]
        public void MoveUpSecondDefinitionTest()
        {
            var imports = new ObservableCollection<ImportNewAssetsDirectoriesDefinition>
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
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                }
            };

            imports.MoveUp(imports[1]);

            imports.Should().HaveCount(3);
            imports[0].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            imports[0].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
            imports[1].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            imports[1].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            imports[2].SourceDirectory.Should().Be(@"\\MyServer\Images");
            imports[2].DestinationDirectory.Should().Be(@"C:\Images");
        }

        [Fact]
        public void MoveUpThirdDefinitionTest()
        {
            var imports = new ObservableCollection<ImportNewAssetsDirectoriesDefinition>
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
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                }
            };

            imports.MoveUp(imports[2]);

            imports.Should().HaveCount(3);
            imports[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            imports[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            imports[1].SourceDirectory.Should().Be(@"\\MyServer\Images");
            imports[1].DestinationDirectory.Should().Be(@"C:\Images");
            imports[2].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            imports[2].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
        }

        [Fact]
        public void MoveUpFirstDefinitionTest()
        {
            var imports = new ObservableCollection<ImportNewAssetsDirectoriesDefinition>
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
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                }
            };

            imports.MoveUp(imports[0]);

            imports.Should().HaveCount(3);
            imports[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            imports[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            imports[1].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            imports[1].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
            imports[2].SourceDirectory.Should().Be(@"\\MyServer\Images");
            imports[2].DestinationDirectory.Should().Be(@"C:\Images");
        }

        [Fact]
        public void MoveDownSecondDefinitionTest()
        {
            var imports = new ObservableCollection<ImportNewAssetsDirectoriesDefinition>
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
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                }
            };

            imports.MoveDown(imports[1]);

            imports.Should().HaveCount(3);
            imports[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            imports[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            imports[1].SourceDirectory.Should().Be(@"\\MyServer\Images");
            imports[1].DestinationDirectory.Should().Be(@"C:\Images");
            imports[2].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            imports[2].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
        }

        [Fact]
        public void MoveDownFirstDefinitionTest()
        {
            var imports = new ObservableCollection<ImportNewAssetsDirectoriesDefinition>
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
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                }
            };

            imports.MoveDown(imports[0]);

            imports.Should().HaveCount(3);
            imports[0].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            imports[0].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
            imports[1].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            imports[1].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            imports[2].SourceDirectory.Should().Be(@"\\MyServer\Images");
            imports[2].DestinationDirectory.Should().Be(@"C:\Images");
        }

        [Fact]
        public void MoveDownLastDefinitionTest()
        {
            var imports = new ObservableCollection<ImportNewAssetsDirectoriesDefinition>
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
                },
                new ImportNewAssetsDirectoriesDefinition
                {
                    SourceDirectory = @"\\MyServer\Images",
                    DestinationDirectory = @"C:\Images"
                }
            };

            imports.MoveDown(imports[2]);

            imports.Should().HaveCount(3);
            imports[0].SourceDirectory.Should().Be(@"C:\MyFirstGame\Screenshots");
            imports[0].DestinationDirectory.Should().Be(@"C:\Images\MyFirstGame");
            imports[1].SourceDirectory.Should().Be(@"C:\MySecondGame\Screenshots");
            imports[1].DestinationDirectory.Should().Be(@"C:\Images\MySecondGame");
            imports[2].SourceDirectory.Should().Be(@"\\MyServer\Images");
            imports[2].DestinationDirectory.Should().Be(@"C:\Images");
        }
    }
}
