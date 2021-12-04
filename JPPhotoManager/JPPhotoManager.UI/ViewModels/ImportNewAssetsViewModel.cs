using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System.Collections.ObjectModel;
using System.Windows;

namespace JPPhotoManager.UI.ViewModels
{
    public class ImportNewAssetsViewModel : BaseViewModel<IApplication>
    {
        private ObservableCollection<ImportNewAssetsDirectoriesDefinition> imports;
        private ObservableCollection<ImportNewAssetsResult> results;
        private ObservableCollection<string> statusMessages;
        private ImportNewAssetsStepEnum step = ImportNewAssetsStepEnum.Configure;

        public ImportNewAssetsViewModel(IApplication assetApp) : base(assetApp)
        {
            statusMessages = new ObservableCollection<string>();
        }

        public ObservableCollection<ImportNewAssetsDirectoriesDefinition> Imports
        {
            get { return imports; }
            set
            {
                imports = value;
                NotifyPropertyChanged(nameof(Imports));
            }
        }

        public ObservableCollection<ImportNewAssetsResult> Results
        {
            get { return results; }
            set
            {
                results = value;
                NotifyPropertyChanged(nameof(Results));
                NotifyPropertyChanged(nameof(CanViewResults));
            }
        }

        public ObservableCollection<string> StatusMessages
        {
            get { return statusMessages; }
        }

        public ImportNewAssetsStepEnum Step
        {
            get { return step; }
            private set
            {
                step = value;
                NotifyPropertyChanged(nameof(Step), nameof(InputVisible), nameof(ResultsVisible), nameof(CanConfigure));
            }
        }

        public Visibility InputVisible
        {
            get { return Step == ImportNewAssetsStepEnum.Configure || Step == ImportNewAssetsStepEnum.Import ? Visibility.Visible : Visibility.Hidden; }
        }

        public Visibility ResultsVisible
        {
            get { return Step == ImportNewAssetsStepEnum.ViewResults ? Visibility.Visible : Visibility.Hidden; }
        }

        public bool CanConfigure
        {
            get { return Step == ImportNewAssetsStepEnum.Configure; }
        }

        public bool CanViewResults
        {
            get { return Step == ImportNewAssetsStepEnum.Import && Results != null && Results.Count > 0; }
        }

        public void AdvanceStep()
        {
            switch (Step)
            {
                case ImportNewAssetsStepEnum.Configure:
                    Step = ImportNewAssetsStepEnum.Import;
                    break;

                case ImportNewAssetsStepEnum.Import:
                    Step = ImportNewAssetsStepEnum.ViewResults;
                    break;
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

        public void NotifyImageImported(StatusChangeCallbackEventArgs e)
        {
            StatusMessages.Add(e.NewStatus);
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
    }
}
