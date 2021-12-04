using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading;
using System.Windows;

namespace JPPhotoManager.UI.ViewModels
{
    public class FolderAddedEventArgs
    {
        public Folder Folder { get; set; }
    }

    public class FolderRemovedEventArgs
    {
        public Folder Folder { get; set; }
    }

    public delegate void FolderAddedEventHandler(object sender, FolderAddedEventArgs e);
    public delegate void FolderRemovedEventHandler(object sender, FolderRemovedEventArgs e);

    public class ApplicationViewModel : BaseViewModel<IApplication>
    {
        private AppModeEnum appMode;
        private int viewerPosition;
        private string currentFolder;
        private Asset[] cataloguedAssets;
        private ObservableCollection<Asset> observableAssets;
        private Asset[] selectedAssets;
        private string appTitle;
        private string statusMessage;
        private SortCriteriaEnum sortCriteria;
        private SortCriteriaEnum previousSortCriteria;
        public bool SortAscending { get; private set; } = true;

        public string Product { get; set; }
        public string Version { get; set; }
        public bool IsRefreshingFolders { get; set; }

        public event FolderAddedEventHandler FolderAdded;
        public event FolderRemovedEventHandler FolderRemoved;

        public ApplicationViewModel(IApplication assetApp, SortCriteriaEnum initialSortCriteria = SortCriteriaEnum.FileName) : base(assetApp)
        {
            CurrentFolder = Application.GetInitialFolder();
            SortCriteria = initialSortCriteria;
        }

        public AppModeEnum AppMode
        {
            get { return appMode; }
            private set
            {
                appMode = value;
                NotifyPropertyChanged(nameof(AppMode), nameof(ThumbnailsVisible), nameof(ViewerVisible));
                UpdateAppTitle();
            }
        }

        public SortCriteriaEnum SortCriteria
        {
            get { return sortCriteria; }
            private set
            {
                sortCriteria = value;
                NotifyPropertyChanged(nameof(SortCriteria));
            }
        }

        public void ChangeAppMode()
        {
            if (AppMode == AppModeEnum.Viewer)
            {
                AppMode = AppModeEnum.Thumbnails;
            }
            else if (AppMode == AppModeEnum.Thumbnails)
            {
                AppMode = AppModeEnum.Viewer;
            }
        }

        public void ChangeAppMode(AppModeEnum appMode)
        {
            AppMode = appMode;
        }

        public Visibility ThumbnailsVisible
        {
            get { return AppMode == AppModeEnum.Thumbnails ? Visibility.Visible : Visibility.Hidden; }
        }

        public Visibility ViewerVisible
        {
            get { return AppMode == AppModeEnum.Viewer ? Visibility.Visible : Visibility.Hidden; }
        }

        public int ViewerPosition
        {
            get { return viewerPosition; }
            set
            {
                viewerPosition = value;
                NotifyPropertyChanged(
                    nameof(ViewerPosition),
                    nameof(CanGoToPreviousAsset),
                    nameof(CanGoToNextAsset),
                    nameof(CurrentAsset));
                UpdateAppTitle();
            }
        }

        public Asset[] SelectedAssets
        {
            get { return selectedAssets; }
            set
            {
                selectedAssets = value;
                NotifyPropertyChanged(nameof(SelectedAssets));
            }
        }

        public string CurrentFolder
        {
            get { return currentFolder; }
            set
            {
                currentFolder = value;
                NotifyPropertyChanged(nameof(CurrentFolder));
                UpdateAppTitle();
            }
        }

        public ObservableCollection<Asset> ObservableAssets
        {
            get { return observableAssets; }
            private set
            {
                observableAssets = value;
                NotifyPropertyChanged(nameof(ObservableAssets));
                UpdateAppTitle();
            }
        }

        public void SetAssets(Asset[] assets)
        {
            // The assets that have no image data are filtered out.
            // If a folder is being catalogued for the first time and
            // the GetImages method is called, since the thumbnails file is not
            // created yet, the assets catalogued so far are returned without
            // its thumbnails.
            cataloguedAssets = assets?.Where(a => a.ImageData != null).ToArray();
            SortAssets();
        }

        private void SortAssets()
        {
            switch (SortCriteria)
            {
                case SortCriteriaEnum.FileName:
                    cataloguedAssets = SortAscending ?
                        cataloguedAssets?.OrderBy(a => a.FileName).ToArray() :
                        cataloguedAssets?.OrderByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.ThumbnailCreationDateTime:
                    cataloguedAssets = SortAscending ?
                        cataloguedAssets?.OrderBy(a => a.ThumbnailCreationDateTime).ThenBy(a => a.FileName).ToArray() :
                        cataloguedAssets?.OrderByDescending(a => a.ThumbnailCreationDateTime).ThenByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.FileCreationDateTime:
                    cataloguedAssets = SortAscending ?
                        cataloguedAssets?.OrderBy(a => a.FileCreationDateTime).ThenBy(a => a.FileName).ToArray() :
                        cataloguedAssets?.OrderByDescending(a => a.FileCreationDateTime).ThenByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.FileModificationDateTime:
                    cataloguedAssets = SortAscending ?
                        cataloguedAssets?.OrderBy(a => a.FileModificationDateTime).ThenBy(a => a.FileName).ToArray() :
                        cataloguedAssets?.OrderByDescending(a => a.FileModificationDateTime).ThenByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.FileSize:
                    cataloguedAssets = SortAscending ?
                        cataloguedAssets?.OrderBy(a => a.FileSize).ThenBy(a => a.FileName).ToArray() :
                        cataloguedAssets?.OrderByDescending(a => a.FileSize).ThenByDescending(a => a.FileName).ToArray();
                    break;
            }

            ObservableAssets = cataloguedAssets != null ? new ObservableCollection<Asset>(cataloguedAssets) : null;
        }

        public string AppTitle
        {
            get { return appTitle; }
            set
            {
                appTitle = value;
                NotifyPropertyChanged(nameof(AppTitle));
            }
        }

        public string StatusMessage
        {
            get { return statusMessage; }
            set
            {
                statusMessage = value;
                NotifyPropertyChanged(nameof(StatusMessage));
            }
        }

        public Asset CurrentAsset
        {
            get { return ObservableAssets?.Count > 0 && ViewerPosition >= 0 ? ObservableAssets?[ViewerPosition] : null; }
        }

        public Folder LastSelectedFolder { get; set; }

        private void AddAsset(Asset asset)
        {
            if (ObservableAssets != null)
            {
                ObservableAssets.Add(asset);
                NotifyPropertyChanged(nameof(ObservableAssets));
            }
        }

        private void UpdateAsset(Asset asset)
        {
            if (ObservableAssets != null)
            {
                var updatedAsset = ObservableAssets.FirstOrDefault(
                    a => string.Compare(a.FileName, asset.FileName, StringComparison.OrdinalIgnoreCase) == 0);
                
                if (updatedAsset != null)
                {
                    RemoveAssets(new Asset[] { updatedAsset });
                    AddAsset(asset);
                    NotifyPropertyChanged(nameof(ObservableAssets));
                }
            }
        }

        public void RemoveAssets(Asset[] assets)
        {
            if (ObservableAssets != null && assets != null)
            {
                foreach (var asset in assets)
                {
                    int position = ViewerPosition;
                    ObservableAssets.Remove(asset);

                    if (position == ObservableAssets.Count)
                    {
                        position--;
                    }

                    ViewerPosition = position;
                }
                
                NotifyPropertyChanged(nameof(ObservableAssets));
            }
        }

        private void AddFolder(Folder folder)
        {
            if (FolderAdded != null)
            {
                FolderAdded(this, new FolderAddedEventArgs { Folder = folder });
            }
        }

        private void RemoveFolder(Folder folder)
        {
            if (FolderRemoved != null)
            {
                FolderRemoved(this, new FolderRemovedEventArgs { Folder = folder });
            }
        }

        private void UpdateAppTitle()
        {
            string title = null;
            string sortCriteria = GetSortCriteriaDescription();

            if (AppMode == AppModeEnum.Thumbnails)
            {
                title = string.Format(
                    Thread.CurrentThread.CurrentCulture,
                    "{0} {1} - {2} - image {3} de {4} - sorted by {5}",
                    Product,
                    Version,
                    CurrentFolder,
                    ViewerPosition + 1,
                    ObservableAssets?.Count,
                    sortCriteria);
            }
            else if (AppMode == AppModeEnum.Viewer)
            {
                title = string.Format(
                    Thread.CurrentThread.CurrentCulture,
                    "{0} {1} - {2} - {3} - image {4} de {5} - sorted by {6}",
                    Product,
                    Version,
                    CurrentFolder,
                    CurrentAsset?.FileName,
                    ViewerPosition + 1,
                    ObservableAssets?.Count,
                    sortCriteria);
            }

            AppTitle = title;
        }

        public void GoToAsset(Asset asset)
        {
            GoToAsset(asset, AppMode);
        }

        public void GoToAsset(Asset asset, AppModeEnum newAppMode)
        {
            Asset targetAsset = ObservableAssets.FirstOrDefault(f => f.FileName == asset.FileName);

            if (targetAsset != null && Application.FileExists(targetAsset.FullPath))
            {
                int position = ObservableAssets.IndexOf(targetAsset);
                ChangeAppMode(newAppMode);
                ViewerPosition = position;
            }
        }

        public void GoToPreviousAsset()
        {
            if (CanGoToPreviousAsset)
            {
                ViewerPosition--;
            }
        }

        public void GoToNextAsset()
        {
            if (CanGoToNextAsset)
            {
                ViewerPosition++;
            }
        }

        public bool CanGoToPreviousAsset
        {
            get
            {
                return ViewerPosition > 0;
            }
        }

        public bool CanGoToNextAsset
        {
            get
            {
                return ViewerPosition < (ObservableAssets?.Count - 1);
            }
        }

        public void NotifyCatalogChange(CatalogChangeCallbackEventArgs e)
        {
            StatusMessage = e?.Message;

            switch (e?.Reason)
            {
                case ReasonEnum.AssetCreated:
                    if (e?.Asset?.Folder?.Path == CurrentFolder)
                    {
                        // If the files list is empty or belongs to other directory
                        if ((ObservableAssets.Count == 0 || ObservableAssets[0].Folder.Path != CurrentFolder) && e.CataloguedAssets != null)
                        {
                            cataloguedAssets = e.CataloguedAssets.Where(a => a.ImageData != null).ToArray();
                            SortAssets();
                        }
                        else
                        {
                            AddAsset(e.Asset);
                        }
                    }
                        
                    break;

                case ReasonEnum.AssetUpdated:
                    if (e?.Asset?.Folder?.Path == CurrentFolder)
                    {
                        // If the files list is empty or belongs to other directory
                        if ((ObservableAssets.Count == 0 || ObservableAssets[0].Folder.Path != CurrentFolder) && e.CataloguedAssets != null)
                        {
                            cataloguedAssets = e.CataloguedAssets.Where(a => a.ImageData != null).ToArray();
                            SortAssets();
                        }
                        else
                        {
                            UpdateAsset(e.Asset);
                        }
                    }

                    break;

                case ReasonEnum.AssetDeleted:
                    RemoveAssets(new Asset[] { e.Asset });
                    break;

                case ReasonEnum.FolderCreated:
                    AddFolder(e.Folder);
                    break;

                case ReasonEnum.FolderDeleted:
                    RemoveFolder(e.Folder);
                    break;
            }
        }

        public void SortAssetsByCriteria(SortCriteriaEnum sortCriteria)
        {
            previousSortCriteria = SortCriteria;
            SortCriteria = sortCriteria;
            SortAscending = SortCriteria != previousSortCriteria || !SortAscending;
            SortAssets();
        }

        private string GetSortCriteriaDescription()
        {
            string result = "";

            switch (SortCriteria)
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

            result += SortAscending ? " ascending" : " descending";

            return result;
        }
    }
}
