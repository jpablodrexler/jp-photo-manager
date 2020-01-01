using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Text;
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
            this.statusMessages = new ObservableCollection<string>();
        }

        public ObservableCollection<ImportNewAssetsDirectoriesDefinition> Imports
        {
            get { return this.imports; }
            set
            {
                this.imports = value;
                this.NotifyPropertyChanged(nameof(Imports));
            }
        }

        public ObservableCollection<ImportNewAssetsResult> Results
        {
            get { return this.results; }
            set
            {
                this.results = value;
                this.NotifyPropertyChanged(nameof(Results));
                this.NotifyPropertyChanged(nameof(CanViewResults));
            }
        }

        public ObservableCollection<string> StatusMessages
        {
            get { return this.statusMessages; }
        }

        public ImportNewAssetsStepEnum Step
        {
            get { return this.step; }
            private set
            {
                this.step = value;
                this.NotifyPropertyChanged(nameof(Step), nameof(InputVisible), nameof(ResultsVisible), nameof(CanConfigure));
            }
        }

        public Visibility InputVisible
        {
            get { return this.Step == ImportNewAssetsStepEnum.Configure || this.Step == ImportNewAssetsStepEnum.Import ? Visibility.Visible : Visibility.Hidden; }
        }

        public Visibility ResultsVisible
        {
            get { return this.Step == ImportNewAssetsStepEnum.ViewResults ? Visibility.Visible : Visibility.Hidden; }
        }

        public bool CanConfigure
        {
            get { return this.Step == ImportNewAssetsStepEnum.Configure; }
        }

        public bool CanViewResults
        {
            get { return this.Step == ImportNewAssetsStepEnum.Import && this.Results != null && this.Results.Count > 0; }
        }

        public void AdvanceStep()
        {
            switch (this.Step)
            {
                case ImportNewAssetsStepEnum.Configure:
                    this.Step = ImportNewAssetsStepEnum.Import;
                    break;

                case ImportNewAssetsStepEnum.Import:
                    this.Step = ImportNewAssetsStepEnum.ViewResults;
                    break;
            }
        }

        public void DeleteDefinition(ImportNewAssetsDirectoriesDefinition definition)
        {
            if (this.Imports != null)
            {
                this.Imports.Remove(definition);
                this.NotifyPropertyChanged(nameof(Imports));
            }
        }

        public void NotifyImageImported(StatusChangeCallbackEventArgs e)
        {
            this.StatusMessages.Add(e.NewStatus);
        }

        internal void MoveUpDefinition(ImportNewAssetsDirectoriesDefinition definition)
        {
            if (this.Imports != null)
            {
                this.Imports.MoveUp(definition);
            }
        }

        internal void MoveDownDefinition(ImportNewAssetsDirectoriesDefinition definition)
        {
            if (this.Imports != null)
            {
                this.Imports.MoveDown(definition);
            }
        }
    }
}
