using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Media.Imaging;

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

    public class ApplicationViewModel : BaseViewModel
    {
        private AppModeEnum _appMode;
        private int _viewerPosition;
        private string _currentFolder;
        private Asset[] _cataloguedAssets;
        private ObservableCollection<Asset> _observableAssets;
        private PaginatedData<Asset> _paginatedAssets;
        private Asset[] _selectedAssets;
        private string _appTitle;
        private double _loadingPercent;
        private string _statusMessage;
        private SortCriteriaEnum _sortCriteria;
        private SortCriteriaEnum _previousSortCriteria;
        private bool _isLoading;

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
            get { return _appMode; }
            private set
            {
                _appMode = value;
                NotifyPropertyChanged(nameof(AppMode), nameof(ThumbnailsVisible), nameof(ViewerVisible));
                UpdateAppStatus();
            }
        }

        public SortCriteriaEnum SortCriteria
        {
            get { return _sortCriteria; }
            private set
            {
                _sortCriteria = value;
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

        public Visibility LoadingVisible
        {
            get { return LoadingPercent > 0 && LoadingPercent < 100 ? Visibility.Visible : Visibility.Hidden; }
        }

        public int ViewerPosition
        {
            get { return _viewerPosition; }
            set
            {
                _viewerPosition = value;
                NotifyPropertyChanged(
                    nameof(ViewerPosition),
                    nameof(CanGoToPreviousAsset),
                    nameof(CanGoToNextAsset),
                    nameof(CurrentAsset));
                UpdateAppStatus();
            }
        }

        public Asset[] SelectedAssets
        {
            get { return _selectedAssets; }
            set
            {
                _selectedAssets = value;
                NotifyPropertyChanged(nameof(SelectedAssets));
            }
        }

        public string CurrentFolder
        {
            get { return _currentFolder; }
            set
            {
                _currentFolder = value;
                NotifyPropertyChanged(nameof(CurrentFolder));
                UpdateAppStatus();
            }
        }

        public ObservableCollection<Asset> ObservableAssets
        {
            get { return _observableAssets; }
            private set
            {
                _observableAssets = value;
                NotifyPropertyChanged(nameof(ObservableAssets));
                UpdateAppStatus();
            }
        }

        public void SetAssets(Asset[] assets)
        {
            // The assets that have no image data are filtered out.
            // If a folder is being catalogued for the first time and
            // the GetImages method is called, since the thumbnails file is not
            // created yet, the assets catalogued so far are returned without
            // its thumbnails.
            _cataloguedAssets = assets?.Where(a => a.ImageData != null).ToArray();
            SortAssets();
        }

        private void SortAssets()
        {
            switch (SortCriteria)
            {
                case SortCriteriaEnum.FileName:
                    _cataloguedAssets = SortAscending ?
                        _cataloguedAssets?.OrderBy(a => a.FileName).ToArray() :
                        _cataloguedAssets?.OrderByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.ThumbnailCreationDateTime:
                    _cataloguedAssets = SortAscending ?
                        _cataloguedAssets?.OrderBy(a => a.ThumbnailCreationDateTime).ThenBy(a => a.FileName).ToArray() :
                        _cataloguedAssets?.OrderByDescending(a => a.ThumbnailCreationDateTime).ThenByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.FileCreationDateTime:
                    _cataloguedAssets = SortAscending ?
                        _cataloguedAssets?.OrderBy(a => a.FileCreationDateTime).ThenBy(a => a.FileName).ToArray() :
                        _cataloguedAssets?.OrderByDescending(a => a.FileCreationDateTime).ThenByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.FileModificationDateTime:
                    _cataloguedAssets = SortAscending ?
                        _cataloguedAssets?.OrderBy(a => a.FileModificationDateTime).ThenBy(a => a.FileName).ToArray() :
                        _cataloguedAssets?.OrderByDescending(a => a.FileModificationDateTime).ThenByDescending(a => a.FileName).ToArray();
                    break;

                case SortCriteriaEnum.FileSize:
                    _cataloguedAssets = SortAscending ?
                        _cataloguedAssets?.OrderBy(a => a.FileSize).ThenBy(a => a.FileName).ToArray() :
                        _cataloguedAssets?.OrderByDescending(a => a.FileSize).ThenByDescending(a => a.FileName).ToArray();
                    break;
            }

            ObservableAssets = _cataloguedAssets != null ? new ObservableCollection<Asset>(_cataloguedAssets) : null;
        }

        public bool IsLoading
        {
            get { return _isLoading; }
            set
            {
                _isLoading = value;
                UpdateAppStatus();
            }
        }

        public string AppTitle
        {
            get { return _appTitle; }
            set
            {
                _appTitle = value;
                NotifyPropertyChanged(nameof(AppTitle));
            }
        }

        public double LoadingPercent
        {
            get { return _loadingPercent; }
            set
            {
                _loadingPercent = value;
                NotifyPropertyChanged(nameof(LoadingPercent));
                NotifyPropertyChanged(nameof(LoadingVisible));
            }
        }

        public string StatusMessage
        {
            get { return _statusMessage; }
            set
            {
                _statusMessage = value;
                NotifyPropertyChanged(nameof(StatusMessage));
            }
        }

        public Asset CurrentAsset
        {
            get { return ObservableAssets?.Count > 0 && ViewerPosition >= 0 ? ObservableAssets?[ViewerPosition] : null; }
        }

        public Folder LastSelectedFolder { get; set; }

        public void AddAsset(Asset asset)
        {
            if (ObservableAssets != null && !ObservableAssets.Contains(asset))
            {
                ObservableAssets.Add(asset);
                NotifyPropertyChanged(nameof(ObservableAssets));
                UpdateAppStatus();
            }
        }

        public void AddAssetsPage(Asset[] assets)
        {
            if (ObservableAssets != null)
            {
                foreach (var asset in assets)
                {
                    if (!ObservableAssets.Contains(asset))
                    {
                        ObservableAssets.Add(asset);
                    }
                }

                _cataloguedAssets = ObservableAssets.ToArray();
                NotifyPropertyChanged(nameof(ObservableAssets));
                UpdateAppStatus();
            }
        }

        public void SetPaginatedAssets(PaginatedData<Asset> assets)
        {
            _paginatedAssets = assets;

            if (assets.PageIndex == 0)
                SetAssets(assets.Items);
            else
                AddAssetsPage(assets.Items);
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
                    UpdateAppStatus();
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

        private void AddFolder(Folder folder) => FolderAdded?.Invoke(this, new FolderAddedEventArgs { Folder = folder });

        private void RemoveFolder(Folder folder) => FolderRemoved?.Invoke(this, new FolderRemovedEventArgs { Folder = folder });

        private void UpdateAppStatus()
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

            var percent = GetLoadingPercent();
            LoadingPercent = percent ?? 0;
            AppTitle = title;
        }

        private double? GetLoadingPercent()
        {
            double? count = ObservableAssets?.Count;
            double? totalCount = _paginatedAssets?.TotalCount;
            double? percent = count * 100 / totalCount;

            return percent;
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
                            _cataloguedAssets = e.CataloguedAssets.Where(a => a.ImageData != null).ToArray();
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
                            _cataloguedAssets = e.CataloguedAssets.Where(a => a.ImageData != null).ToArray();
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
            _previousSortCriteria = SortCriteria;
            SortCriteria = sortCriteria;
            SortAscending = SortCriteria != _previousSortCriteria || !SortAscending;
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

        public async Task CatalogAssets(CatalogChangeCallback callback) => await Application.CatalogAssetsAsync(callback);

        public int GetCatalogCooldownMinutes() => Application.GetCatalogCooldownMinutes();

        public FolderViewModel[] GetRootCatalogFolders()
        {
            Folder[] rootFolders = Application.GetRootCatalogFolders();
            return rootFolders.Select(f => new FolderViewModel(this, Application) { Folder = f, IsExpanded = false }).ToArray();
        }

        public FolderViewModel[] GetSubFolders(Folder parentFolder, bool includeHidden)
        {
            Folder[] subFolders = Application.GetSubFolders(parentFolder, includeHidden);
            return subFolders.Select(f => new FolderViewModel(this, Application) { Folder = f, IsExpanded = false }).ToArray();
        }

        public BitmapImage LoadBitmapImage() => Application.LoadBitmapImage(CurrentAsset.FullPath, CurrentAsset.ImageRotation);
    }
}
