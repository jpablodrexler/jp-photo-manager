using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System.Collections.ObjectModel;
using System.Threading.Tasks;

namespace JPPhotoManager.UI.ViewModels
{
    public class ImportNewAssetsViewModel : BaseProcessViewModel<ImportNewAssetsConfiguration, ImportNewAssetsResult>
    {
        private ObservableCollection<ImportNewAssetsDirectoriesDefinition>? imports;
        
        public ImportNewAssetsViewModel(IApplication assetApp) : base(assetApp)
        {
        }

        public override string Description => "This process allows to import new assets to the catalog. " +
            "You can configure one or multiple import operations by entering a source path and a destination path. " +
            "You can specify if the import operation should also include sub-folders.";

        public ObservableCollection<ImportNewAssetsDirectoriesDefinition>? Imports
        {
            get { return imports; }
            set
            {
                imports = value;
                NotifyPropertyChanged(nameof(Imports));
            }
        }

        public void DeleteDefinition(ImportNewAssetsDirectoriesDefinition definition)
        {
            if (Imports != null)
            {
                Imports.Remove(definition);
                NotifyPropertyChanged(nameof(Imports));
            }
        }

        public void MoveUpDefinition(ImportNewAssetsDirectoriesDefinition definition)
        {
            if (Imports != null)
            {
                Imports.MoveUp(definition);
            }
        }

        public void MoveDownDefinition(ImportNewAssetsDirectoriesDefinition definition)
        {
            if (Imports != null)
            {
                Imports.MoveDown(definition);
            }
        }

        public override ImportNewAssetsConfiguration GetProcessConfiguration() => Application.GetImportNewAssetsConfiguration();

        public override void SetProcessConfiguration(ImportNewAssetsConfiguration configuration) => Application.SetImportNewAssetsConfiguration(configuration);

        public override async Task RunProcessAsync(ProcessStatusChangedCallback callback)
        {
            var results = await Application.ImportNewAssetsAsync(callback);
            Results = new ObservableCollection<ImportNewAssetsResult>(results);
        }
    }
}
