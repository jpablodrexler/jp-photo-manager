using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.IO;
using System.Linq;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace JPPhotoManager.ViewModels
{
    public class ApplicationViewModel : INotifyPropertyChanged
    {
        public event PropertyChangedEventHandler PropertyChanged;

        private AppModeEnum _appMode;
        private int _viewerPosition;
        private string _currentFolder;
        private ObservableCollection<Asset> _files;
        private ImageSource _currentImageSource;
        private string _appTitle;
        private string _statusMessage;

        public string Product { get; set; }
        public string Version { get; set; }
        public IJPPhotoManagerApplication AssetApp { get; private set; }

        public ApplicationViewModel(IJPPhotoManagerApplication assetApp)
        {
            this.AssetApp = assetApp;
            var folder = this.AssetApp.GetInitialFolder();
            this.CurrentFolder = folder;
        }

        public AppModeEnum AppMode
        {
            get { return this._appMode; }
            private set
            {
                this._appMode = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("AppMode"));
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("ThumbnailsVisible"));
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("ViewerVisible"));
                this.UpdateAppTitle();
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
            get { return this._viewerPosition; }
            set
            {
                this._viewerPosition = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("ViewerPosition"));
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("CurrentAsset"));
                this.UpdateAppTitle();
            }
        }

        public string CurrentFolder
        {
            get { return this._currentFolder; }
            set
            {
                this._currentFolder = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("CurrentFolder"));
                this.UpdateAppTitle();
            }
        }

        public ObservableCollection<Asset> Files
        {
            get { return this._files; }
            set
            {
                this._files = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("Files"));
                this.UpdateAppTitle();
            }
        }

        public ImageSource CurrentImageSource
        {
            get { return this._currentImageSource; }
            set
            {
                this._currentImageSource = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("CurrentImageSource"));
            }
        }

        public string AppTitle
        {
            get { return this._appTitle; }
            set
            {
                this._appTitle = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("AppTitle"));
            }
        }

        public string StatusMessage
        {
            get { return this._statusMessage; }
            set
            {
                this._statusMessage = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("StatusMessage"));
            }
        }

        public Asset CurrentAsset
        {
            get { return this.Files?.Count > 0 ? this.Files?[this.ViewerPosition] : null; }
        }

        private void AddAsset(Asset asset)
        {
            if (this.Files != null)
            {
                this.Files.Add(asset);
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("Files"));
            }
        }

        private void RemoveAsset(Asset asset)
        {
            if (this.Files != null)
            {
                this.Files.Remove(asset);
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("Files"));
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
                title = string.Format("{0} {1} - {2} - imagen {3} de {4}", this.Product, this.Version, this.CurrentAsset?.FileName, this.ViewerPosition + 1, this.Files?.Count);
            }

            this.AppTitle = title;
        }

        public void GoToImage(Asset asset)
        {
            this.GoToImage(asset, this.AppMode);
        }

        public void GoToImage(Asset asset, AppModeEnum newAppMode)
        {
            Asset targetAsset = this.Files.FirstOrDefault(f => f.FileName == asset.FileName);

            if (targetAsset != null && File.Exists(targetAsset.FullPath))
            {
                int position = this.Files.IndexOf(targetAsset);
                this.ChangeAppMode(newAppMode);
                this.ViewerPosition = position;

                if (this.AppMode == AppModeEnum.Viewer)
                {
                    this.ShowImage();
                }
            }
        }

        public void GoToPreviousImage()
        {
            if (this.ViewerPosition > 0)
            {
                this.ViewerPosition--;
                this.ShowImage();
            }
        }

        public void GoToNextImage()
        {
            if (this.ViewerPosition < (this.Files.Count - 1))
            {
                this.ViewerPosition++;
                this.ShowImage();
            }
        }

        private void ShowImage()
        {
            if (File.Exists(this.CurrentAsset.FullPath))
            {
                ImageSource source = new BitmapImage(new Uri(this.CurrentAsset.FullPath));
                this.CurrentImageSource = source;
            }
        }

        public void NotifyCatalogChange(CatalogChangeCallbackEventArgs e)
        {
            this.StatusMessage = e.Message;

            if (e?.Asset?.Folder?.Path == this.CurrentFolder)
            {
                switch (e.Reason)
                {
                    case ReasonEnum.Created:
                        // If the files list is empty or belongs to other directory
                        if ((this.Files.Count == 0 || this.Files[0].Folder.Path != this.CurrentFolder) && e.CataloguedAssets != null)
                        {
                            this.Files = new ObservableCollection<Asset>(e.CataloguedAssets);
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
    }
}
