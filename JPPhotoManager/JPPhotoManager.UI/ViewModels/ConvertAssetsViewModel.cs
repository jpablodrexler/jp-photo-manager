using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Entities;
using System.Collections.ObjectModel;
using System.Threading.Tasks;

namespace JPPhotoManager.UI.ViewModels
{
    public class ConvertAssetsViewModel : BaseProcessViewModel<ConvertAssetsConfiguration, ConvertAssetsResult>
    {
        private ObservableCollection<ConvertAssetsDirectoriesDefinition>? _definitions;

        public ConvertAssetsViewModel(IApplication assetApp) : base(assetApp)
        {
        }

        public override string Description => "This process allows to convert assets in the catalog from PNG to JPG. " +
            "You can configure one or multiple convert operations by entering a source path and a destination path. " +
            "You can specify if the convert operation should also include sub-folders. " +
            "There is also the option to delete the source assets.";

        public ObservableCollection<ConvertAssetsDirectoriesDefinition>? Definitions
        {
            get { return _definitions; }
            set
            {
                _definitions = value;
                NotifyPropertyChanged(nameof(Definitions));
            }
        }

        public void DeleteDefinition(ConvertAssetsDirectoriesDefinition definition)
        {
            if (Definitions != null)
            {
                Definitions.Remove(definition);
                NotifyPropertyChanged(nameof(Definitions));
            }
        }

        public void MoveUpDefinition(ConvertAssetsDirectoriesDefinition definition)
        {
            if (Definitions != null)
            {
                definition.Order = Definitions.MoveUp(definition);
            }
        }

        public void MoveDownDefinition(ConvertAssetsDirectoriesDefinition definition)
        {
            if (Definitions != null)
            {
                definition.Order = Definitions.MoveDown(definition);
            }
        }

        public override ConvertAssetsConfiguration GetProcessConfiguration() => Application.GetConvertAssetsConfiguration();

        public override void SetProcessConfiguration(ConvertAssetsConfiguration configuration) => Application.SetConvertAssetsConfiguration(configuration);

        public override async Task RunProcessAsync(ProcessStatusChangedCallback callback)
        {
            var results = await Application.ConvertAssetsAsync(callback);
            Results = new ObservableCollection<ConvertAssetsResult>(results);
        }
    }
}
