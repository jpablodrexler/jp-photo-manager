using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System.Collections.ObjectModel;
using System.Threading.Tasks;

namespace JPPhotoManager.UI.ViewModels
{
    public class SyncAssetsViewModel : BaseProcessViewModel<SyncAssetsConfiguration, SyncAssetsResult>
    {
        private ObservableCollection<SyncAssetsDirectoriesDefinition>? definitions;
        
        public SyncAssetsViewModel(IApplication assetApp) : base(assetApp)
        {
        }

        public override string Description => "This process allows to sync new assets to the catalog. " +
            "You can configure one or multiple sync operations by entering a source path and a destination path. " +
            "You can specify if the sync operation should also include sub-folders. " +
            "There is also the option to delete from the destination path the assets not present in the source path.";

        public ObservableCollection<SyncAssetsDirectoriesDefinition>? Definitions
        {
            get { return definitions; }
            set
            {
                definitions = value;
                NotifyPropertyChanged(nameof(Definitions));
            }
        }

        public void DeleteDefinition(SyncAssetsDirectoriesDefinition definition)
        {
            if (Definitions != null)
            {
                Definitions.Remove(definition);
                NotifyPropertyChanged(nameof(Definitions));
            }
        }

        public void MoveUpDefinition(SyncAssetsDirectoriesDefinition definition)
        {
            if (Definitions != null)
            {
                Definitions.MoveUp(definition);
            }
        }

        public void MoveDownDefinition(SyncAssetsDirectoriesDefinition definition)
        {
            if (Definitions != null)
            {
                Definitions.MoveDown(definition);
            }
        }

        public override SyncAssetsConfiguration GetProcessConfiguration() => Application.GetSyncAssetsConfiguration();

        public override void SetProcessConfiguration(SyncAssetsConfiguration configuration) => Application.SetSyncAssetsConfiguration(configuration);

        public override async Task RunProcessAsync(ProcessStatusChangedCallback callback)
        {
            var results = await Application.SyncAssetsAsync(callback);
            Results = new ObservableCollection<SyncAssetsResult>(results);
        }
    }
}
