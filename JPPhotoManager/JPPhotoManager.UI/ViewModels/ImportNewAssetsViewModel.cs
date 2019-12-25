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
        private int _importsPosition;
        private ObservableCollection<ImportNewAssetsResult> results;

        public ImportNewAssetsViewModel(IApplication assetApp) : base(assetApp)
        {

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

        public int ImportsPosition
        {
            get { return this._importsPosition; }
            set
            {
                this._importsPosition = value;
                this.NotifyPropertyChanged(nameof(ImportsPosition));
            }
        }

        public ObservableCollection<ImportNewAssetsResult> Results
        {
            get { return this.results; }
            set
            {
                this.results = value;
                this.NotifyPropertyChanged(nameof(Results));
                this.NotifyPropertyChanged(nameof(ResultsVisibility));
            }
        }

        public void RemoveDefinition(ImportNewAssetsDirectoriesDefinition definition)
        {
            if (this.Imports != null)
            {
                int position = this.ImportsPosition;
                this.Imports.Remove(definition);

                if (position == this.Imports.Count)
                {
                    position--;
                }

                this.ImportsPosition = position;

                this.NotifyPropertyChanged(nameof(Imports));
            }
        }

        public Visibility ResultsVisibility
        {
            get
            {
                return this.Results != null && this.Results.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
            }
        }
    }
}
