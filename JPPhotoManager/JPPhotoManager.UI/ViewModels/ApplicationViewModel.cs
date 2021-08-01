using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading;
using System.Windows;

namespace JPPhotoManager.UI.ViewModels
{
    public class ApplicationViewModel : BaseViewModel<IApplication>
    {
        private AppModeEnum appMode;
        private int viewerPosition;
        private string currentFolder;
        private Asset[] cataloguedAssets;
        private ObservableCollection<Asset> observableAssets;
        private string appTitle;
        private string statusMessage;
        private SortCriteriaEnum sortCriteria;
        private SortCriteriaEnum previousSortCriteria;
        public bool SortAscending { get; private set; } = true;

        public string Product { get; set; }
        public string Version { get; set; }
        public bool IsRefreshingFolders { get; set; }

        public ApplicationViewModel(IApplication assetApp, SortCriteriaEnum initialSortCriteria = SortCriteriaEnum.FileName) : base(assetApp)
        {
            this.CurrentFolder = this.Application.GetInitialFolder();
            this.SortCriteria = initialSortCriteria;
        }

        public AppModeEnum AppMode
        {
            get { return this.appMode; }
            private set
            {
                this.appMode = value;
                this.NotifyPropertyChanged(nameof(AppMode), nameof(ThumbnailsVisible), nameof(ViewerVisible));
                this.UpdateAppTitle();
            }
        }

        public SortCriteriaEnum SortCriteria
        {
            get { return this.sortCriteria; }
            private set
            {
                this.sortCriteria = value;
                this.NotifyPropertyChanged(nameof(SortCriteria));
            }
        }

        public void ChangeAppMode()
        {
            if (this.AppMode == AppModeEnum.Viewer)
            {
                this.AppMode = AppModeEnum.Thumbnails;
            }
            else if (this.AppMode == AppModeEnum.Thumbnails)
            {
                this.AppMode = AppModeEnum.Viewer;
            }
        }

        public void ChangeAppMode(AppModeEnum appMode)
        {
            this.AppMode = appMode;
        }

        public Visibility ThumbnailsVisible
        {
            get { return this.AppMode == AppModeEnum.Thumbnails ? Visibility.Visible : Visibility.Hidden; }
        }

        public Visibility ViewerVisible
        {
            get { return this.AppMode == AppModeEnum.Viewer ? Visibility.Visible : Visibility.Hidden; }
        }

        public int ViewerPosition
        {
            get { return this.viewerPosition; }
            set
            {
                this.viewerPosition = value;
                this.NotifyPropertyChanged(nameof(ViewerPosition), nameof(CurrentAsset));
                this.UpdateAppTitle();
            }
        }

        public string CurrentFolder
        {
            get { return this.currentFolder; }
            set
            {
                this.currentFolder = value;
                this.NotifyPropertyChanged(nameof(CurrentFolder));
                this.UpdateAppTitle();
            }
        }

        public ObservableCollection<Asset> ObservableAssets
        {
            get { return this.observableAssets; }
            private set
            {
                this.observableAssets = value;
                this.NotifyPropertyChanged(nameof(ObservableAssets));
                this.UpdateAppTitle();
            }
        }

        public void SetAssets(Asset[] assets)
        {
            // The assets that have no image data are filtered out.
            // If a folder is being catalogued for the first time and
            // the GetImages method is called, since the thumbnails file is not
            // created yet, the assets catalogued so far are returned without
            // its thumbnails.
            this.cataloguedAssets = assets?.Where(a => a.ImageData != null).ToArray();
            this.SortAssets();
        }

        private void SortAssets()
        {
            switch (this.SortCriteria)
            {
                case SortCriteriaEnum.FileName:
                    this.cataloguedAssets = this.SortAscending ?
                        this.cataloguedAssets?.OrderBy(a => a.FileName).ToArray() :
                        this.cataloguedAssets?.OrderByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.ThumbnailCreationDateTime:
                    this.cataloguedAssets = this.SortAscending ?
                        this.cataloguedAssets?.OrderBy(a => a.ThumbnailCreationDateTime).ThenBy(a => a.FileName).ToArray() :
                        this.cataloguedAssets?.OrderByDescending(a => a.ThumbnailCreationDateTime).ThenByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.FileCreationDateTime:
                    this.cataloguedAssets = this.SortAscending ?
                        this.cataloguedAssets?.OrderBy(a => a.FileCreationDateTime).ThenBy(a => a.FileName).ToArray() :
                        this.cataloguedAssets?.OrderByDescending(a => a.FileCreationDateTime).ThenByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.FileModificationDateTime:
                    this.cataloguedAssets = this.SortAscending ?
                        this.cataloguedAssets?.OrderBy(a => a.FileModificationDateTime).ThenBy(a => a.FileName).ToArray() :
                        this.cataloguedAssets?.OrderByDescending(a => a.FileModificationDateTime).ThenByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.FileSize:
                    this.cataloguedAssets = this.SortAscending ?
                        this.cataloguedAssets?.OrderBy(a => a.FileSize).ThenBy(a => a.FileName).ToArray() :
                        this.cataloguedAssets?.OrderByDescending(a => a.FileSize).ThenByDescending(a => a.FileName).ToArray();
                    break;
            }

            this.ObservableAssets = this.cataloguedAssets != null ? new ObservableCollection<Asset>(this.cataloguedAssets) : null;
        }

        public string AppTitle
        {
            get { return this.appTitle; }
            set
            {
                this.appTitle = value;
                this.NotifyPropertyChanged(nameof(AppTitle));
            }
        }

        public string StatusMessage
        {
            get { return this.statusMessage; }
            set
            {
                this.statusMessage = value;
                this.NotifyPropertyChanged(nameof(StatusMessage));
            }
        }

        public Asset CurrentAsset
        {
            get { return this.ObservableAssets?.Count > 0 && this.ViewerPosition >= 0 ? this.ObservableAssets?[this.ViewerPosition] : null; }
        }

        public Folder LastSelectedFolder { get; set; }

        private void AddAsset(Asset asset)
        {
            if (this.ObservableAssets != null)
            {
                this.ObservableAssets.Add(asset);
                this.NotifyPropertyChanged(nameof(ObservableAssets));
            }
        }

        private void UpdateAsset(Asset asset)
        {
            if (this.ObservableAssets != null)
            {
                var updatedAsset = this.ObservableAssets.FirstOrDefault(
                    a => string.Compare(a.FileName, asset.FileName, StringComparison.OrdinalIgnoreCase) == 0);
                
                if (updatedAsset != null)
                {
                    RemoveAsset(updatedAsset);
                    AddAsset(asset);
                    this.NotifyPropertyChanged(nameof(ObservableAssets));
                }
            }
        }

        public void RemoveAsset(Asset asset)
        {
            if (this.ObservableAssets != null)
            {
                int position = this.ViewerPosition;
                this.ObservableAssets.Remove(asset);

                if (position == this.ObservableAssets.Count)
                {
                    position--;
                }

                this.ViewerPosition = position;

                this.NotifyPropertyChanged(nameof(ObservableAssets));
            }
        }

        private void UpdateAppTitle()
        {
            string title = null;
            string sortCriteria = GetSortCriteriaDescription();

            if (this.AppMode == AppModeEnum.Thumbnails)
            {
                title = string.Format(
                    Thread.CurrentThread.CurrentCulture,
                    "{0} {1} - {2} - image {3} de {4} - sorted by {5}",
                    this.Product,
                    this.Version,
                    this.CurrentFolder,
                    this.ViewerPosition + 1,
                    this.ObservableAssets?.Count,
                    sortCriteria);
            }
            else if (this.AppMode == AppModeEnum.Viewer)
            {
                title = string.Format(
                    Thread.CurrentThread.CurrentCulture,
                    "{0} {1} - {2} - {3} - image {4} de {5} - sorted by {6}",
                    this.Product,
                    this.Version,
                    this.CurrentFolder,
                    this.CurrentAsset?.FileName,
                    this.ViewerPosition + 1,
                    this.ObservableAssets?.Count,
                    sortCriteria);
            }

            this.AppTitle = title;
        }

        public void GoToAsset(Asset asset)
        {
            this.GoToAsset(asset, this.AppMode);
        }

        public void GoToAsset(Asset asset, AppModeEnum newAppMode)
        {
            Asset targetAsset = this.ObservableAssets.FirstOrDefault(f => f.FileName == asset.FileName);

            if (targetAsset != null && this.Application.FileExists(targetAsset.FullPath))
            {
                int position = this.ObservableAssets.IndexOf(targetAsset);
                this.ChangeAppMode(newAppMode);
                this.ViewerPosition = position;
            }
        }

        public void GoToPreviousAsset()
        {
            if (this.ViewerPosition > 0)
            {
                this.ViewerPosition--;
            }
        }

        public void GoToNextAsset()
        {
            if (this.ViewerPosition < (this.ObservableAssets.Count - 1))
            {
                this.ViewerPosition++;
            }
        }

        public void NotifyCatalogChange(CatalogChangeCallbackEventArgs e)
        {
            this.StatusMessage = e?.Message;

            if (e?.Asset?.Folder?.Path == this.CurrentFolder)
            {
                switch (e.Reason)
                {
                    case ReasonEnum.Created:
                        // If the files list is empty or belongs to other directory
                        if ((this.ObservableAssets.Count == 0 || this.ObservableAssets[0].Folder.Path != this.CurrentFolder) && e.CataloguedAssets != null)
                        {
                            this.cataloguedAssets = e.CataloguedAssets.Where(a => a.ImageData != null).ToArray();
                            this.SortAssets();
                        }
                        else
                        {
                            this.AddAsset(e.Asset);
                        }
                        
                        break;

                    case ReasonEnum.Updated:
                        // If the files list is empty or belongs to other directory
                        if ((this.ObservableAssets.Count == 0 || this.ObservableAssets[0].Folder.Path != this.CurrentFolder) && e.CataloguedAssets != null)
                        {
                            this.cataloguedAssets = e.CataloguedAssets.Where(a => a.ImageData != null).ToArray();
                            this.SortAssets();
                        }
                        else
                        {
                            this.UpdateAsset(e.Asset);
                        }

                        break;

                    case ReasonEnum.Deleted:
                        this.RemoveAsset(e.Asset);
                        break;
                }
            }
        }

        public void SortAssetsByCriteria(SortCriteriaEnum sortCriteria)
        {
            this.previousSortCriteria = this.SortCriteria;
            this.SortCriteria = sortCriteria;
            this.SortAscending = this.SortCriteria != this.previousSortCriteria || !this.SortAscending;
            this.SortAssets();
        }

        private string GetSortCriteriaDescription()
        {
            string result = "";

            switch (this.SortCriteria)
            {
                case SortCriteriaEnum.FileName:
                    result = "file name";
                    break;

                case SortCriteriaEnum.FileSize:
                    result = "file size";
                    break;

                case SortCriteriaEnum.FileCreationDateTime:
                    result = "file creation";
                    break;

                case SortCriteriaEnum.FileModificationDateTime:
                    result = "file modification";
                    break;

                case SortCriteriaEnum.ThumbnailCreationDateTime:
                    result = "thumbnail creation";
                    break;
            }

            result += this.SortAscending ? " ascending" : " descending";

            return result;
        }
    }
}
