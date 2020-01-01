using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Text;
using Xunit;

namespace JPPhotoManager.Tests
{
    public class ViewModelExtensionsTest
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

            Assert.Equal(3, imports.Count);
            Assert.Equal(@"C:\MySecondGame\Screenshots", imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MySecondGame", imports[0].DestinationDirectory);
            Assert.Equal(@"C:\MyFirstGame\Screenshots", imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MyFirstGame", imports[1].DestinationDirectory);
            Assert.Equal(@"\\MyServer\Images", imports[2].SourceDirectory);
            Assert.Equal(@"C:\Images", imports[2].DestinationDirectory);
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

            Assert.Equal(3, imports.Count);
            Assert.Equal(@"C:\MyFirstGame\Screenshots", imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyFirstGame", imports[0].DestinationDirectory);
            Assert.Equal(@"\\MyServer\Images", imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images", imports[1].DestinationDirectory);
            Assert.Equal(@"C:\MySecondGame\Screenshots", imports[2].SourceDirectory);
            Assert.Equal(@"C:\Images\MySecondGame", imports[2].DestinationDirectory);
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

            Assert.Equal(3, imports.Count);
            Assert.Equal(@"C:\MyFirstGame\Screenshots", imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyFirstGame", imports[0].DestinationDirectory);
            Assert.Equal(@"C:\MySecondGame\Screenshots", imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MySecondGame", imports[1].DestinationDirectory);
            Assert.Equal(@"\\MyServer\Images", imports[2].SourceDirectory);
            Assert.Equal(@"C:\Images", imports[2].DestinationDirectory);
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

            Assert.Equal(3, imports.Count);
            Assert.Equal(@"C:\MyFirstGame\Screenshots", imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyFirstGame", imports[0].DestinationDirectory);
            Assert.Equal(@"\\MyServer\Images", imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images", imports[1].DestinationDirectory);
            Assert.Equal(@"C:\MySecondGame\Screenshots", imports[2].SourceDirectory);
            Assert.Equal(@"C:\Images\MySecondGame", imports[2].DestinationDirectory);
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

            Assert.Equal(3, imports.Count);
            Assert.Equal(@"C:\MySecondGame\Screenshots", imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MySecondGame", imports[0].DestinationDirectory);
            Assert.Equal(@"C:\MyFirstGame\Screenshots", imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MyFirstGame", imports[1].DestinationDirectory);
            Assert.Equal(@"\\MyServer\Images", imports[2].SourceDirectory);
            Assert.Equal(@"C:\Images", imports[2].DestinationDirectory);
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

            Assert.Equal(3, imports.Count);
            Assert.Equal(@"C:\MyFirstGame\Screenshots", imports[0].SourceDirectory);
            Assert.Equal(@"C:\Images\MyFirstGame", imports[0].DestinationDirectory);
            Assert.Equal(@"C:\MySecondGame\Screenshots", imports[1].SourceDirectory);
            Assert.Equal(@"C:\Images\MySecondGame", imports[1].DestinationDirectory);
            Assert.Equal(@"\\MyServer\Images", imports[2].SourceDirectory);
            Assert.Equal(@"C:\Images", imports[2].DestinationDirectory);
        }
    }
}
