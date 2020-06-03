using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System.Collections.ObjectModel;
using System.Linq;
using System.Windows;

namespace JPPhotoManager.UI.ViewModels
{
    public class ApplicationViewModel : BaseViewModel<IApplication>
    {
        private AppModeEnum appMode;
        private int viewerPosition;
        private string currentFolder;
        // TODO: REVIEW NAMING FOR THIS VARIABLE AND RELATED METHODS
        private Asset[] assets;
        // TODO: REVIEW NAMING FOR THIS VARIABLE AND RELATED PROPERTY AND METHOD
        private ObservableCollection<Asset> files;
        private string appTitle;
        private string statusMessage;
        private SortCriteriaEnum sortCriteria;
        private SortCriteriaEnum previousSortCriteria;
        private bool sortAscending = true;

        public string Product { get; set; }
        public string Version { get; set; }

        public ApplicationViewModel(IApplication assetApp) : base(assetApp)
        {
            this.CurrentFolder = this.Application.GetInitialFolder();
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

        public ObservableCollection<Asset> Files
        {
            get { return this.files; }
            private set
            {
                this.files = value;
                this.NotifyPropertyChanged(nameof(Files));
                this.UpdateAppTitle();
            }
        }

        public void SetFiles(Asset[] assets)
        {
            // The assets that have no image data are filtered out.
            // If a folder is being catalogued for the first time and
            // the GetImages method is called, since the thumbnails file is not
            // created yet, the assets catalogued so far are returned without
            // its thumbnails.
            this.assets = assets?.Where(a => a.ImageData != null).ToArray();
            this.SortFiles();
        }

        private void SortFiles()
        {
            // TODO: ADD UNIT TESTS FOR THE NEW CODE.
            switch (this.SortCriteria)
            {
                case SortCriteriaEnum.FileName:
                    this.assets = this.sortAscending ?
                        this.assets?.OrderBy(a => a.FileName).ToArray() :
                        this.assets?.OrderByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.ThumbnailCreationDateTime:
                    this.assets = this.sortAscending ?
                        this.assets?.OrderBy(a => a.ThumbnailCreationDateTime).ToArray() :
                        this.assets?.OrderByDescending(a => a.ThumbnailCreationDateTime).ToArray();
                    break;
            }

            this.Files = this.assets != null ? new ObservableCollection<Asset>(this.assets) : null;
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
            get { return this.Files?.Count > 0 && this.ViewerPosition >= 0 ? this.Files?[this.ViewerPosition] : null; }
        }

        public Folder LastSelectedFolder { get; set; }

        private void AddAsset(Asset asset)
        {
            if (this.Files != null)
            {
                this.Files.Add(asset);
                this.NotifyPropertyChanged(nameof(Files));
            }
        }

        public void RemoveAsset(Asset asset)
        {
            if (this.Files != null)
            {
                int position = this.ViewerPosition;
                this.Files.Remove(asset);

                if (position == this.Files.Count)
                {
                    position--;
                }

                this.ViewerPosition = position;

                this.NotifyPropertyChanged(nameof(Files));
            }
        }

        private void UpdateAppTitle()
        {
            string title = null;

            if (this.AppMode == AppModeEnum.Thumbnails)
            {
                title = string.Format("{0} {1} - {2}", this.Product, this.Version, this.CurrentFolder);
            }
            else if (this.AppMode == AppModeEnum.Viewer)
            {
                title = string.Format("{0} {1} - {2} - image {3} de {4}", this.Product, this.Version, this.CurrentAsset?.FileName, this.ViewerPosition + 1, this.Files?.Count);
            }

            this.AppTitle = title;
        }

        public void GoToAsset(Asset asset)
        {
            this.GoToAsset(asset, this.AppMode);
        }

        public void GoToAsset(Asset asset, AppModeEnum newAppMode)
        {
            Asset targetAsset = this.Files.FirstOrDefault(f => f.FileName == asset.FileName);

            if (targetAsset != null && this.Application.FileExists(targetAsset.FullPath))
            {
                int position = this.Files.IndexOf(targetAsset);
                this.ChangeAppMode(newAppMode);
                this.ViewerPosition = position;
            }
        }

        public void GoToPreviousImage()
        {
            if (this.ViewerPosition > 0)
            {
                this.ViewerPosition--;
            }
        }

        public void GoToNextImage()
        {
            if (this.ViewerPosition < (this.Files.Count - 1))
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
                        if ((this.Files.Count == 0 || this.Files[0].Folder.Path != this.CurrentFolder) && e.CataloguedAssets != null)
                        {
                            this.assets = e.CataloguedAssets.Where(a => a.ImageData != null).ToArray();
                            this.SortFiles();
                        }
                        else
                        {
                            this.AddAsset(e.Asset);
                        }
                        
                        break;

                    case ReasonEnum.Updated:
                        // TODO: IMPLEMENT.
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
            this.sortAscending = this.SortCriteria != this.previousSortCriteria || !this.sortAscending;
            this.SortFiles();
        }
    }
}
