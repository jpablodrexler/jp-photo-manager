using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.ViewModels;
using log4net;
using System;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;

namespace JPPhotoManager
{
    public class ThumbnailSelectedEventArgs : EventArgs
    {
        public Asset Asset { get; set; }
    }

    public delegate void ThumbnailSelectedEventHandler(object sender, ThumbnailSelectedEventArgs e);

    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        
        public MainWindow(ApplicationViewModel viewModel)
        {
            try
            {
                InitializeComponent();

                var aboutInformation = viewModel.Application.GetAboutInformation(this.GetType().Assembly);
                viewModel.Product = aboutInformation.Product;
                viewModel.Version = aboutInformation.Version;
                this.DataContext = viewModel;
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        public ApplicationViewModel ViewModel
        {
            get { return (ApplicationViewModel)this.DataContext; }
        }

        private async Task CatalogImagesAsync(IJPPhotoManagerApplication assetApp)
        {
            await Task.Run(() =>
            {
                try
                {
                    assetApp.CatalogImages((e) => Dispatcher.Invoke(() => ViewModel.NotifyCatalogChange(e)));
                }
                catch (TaskCanceledException)
                {
                    // If the application shuts down while the catalog process is running, this exception is thrown.
                }
                catch (Exception ex)
                {
                    log.Error(ex);
                }
            }).ConfigureAwait(true);

            this.Dispatcher.Invoke(() =>
            {
                this.ViewModel.StatusMessage = "Cataloging thumbnails for " + this.ViewModel.CurrentFolder;
            });
        }

        private async void Window_Loaded(object sender, RoutedEventArgs e)
        {
            try
            {
                this.ViewModel?.ChangeAppMode(AppModeEnum.Thumbnails);
                this.thumbnailsUserControl.GoToFolderAsync(this.ViewModel.Application, this.ViewModel?.CurrentFolder);
                this.folderTreeView.SelectedPath = this.ViewModel?.CurrentFolder;
                //this.folderTreeView.GoToFolder(this.ViewModel?.CurrentFolder);
                await CatalogImagesAsync(this.ViewModel.Application);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void Window_KeyDown(object sender, KeyEventArgs e)
        {
            try
            {
                switch (e.Key)
                {
                    case Key.PageUp:
                    case Key.Left:
                        this.ViewModel?.GoToPreviousImage();
                        this.viewerUserControl.ShowImage();
                        break;

                    case Key.PageDown:
                    case Key.Right:
                        this.ViewModel?.GoToNextImage();
                        this.viewerUserControl.ShowImage();
                        break;

                    case Key.F1:
                        this.ViewModel?.ChangeAppMode();
                        this.viewerUserControl.ShowImage();
                        break;
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void ThumbnailsUserControl_ThumbnailSelected(object sender, ThumbnailSelectedEventArgs e)
        {
            try
            {
                this.ViewModel?.GoToImage(e.Asset, AppModeEnum.Viewer);

                if (this.ViewModel.AppMode == AppModeEnum.Viewer)
                {
                    this.viewerUserControl.ShowImage();
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void ViewerUserControl_ThumbnailSelected(object sender, ThumbnailSelectedEventArgs e)
        {
            try
            {
                this.ViewModel?.GoToImage(e.Asset, AppModeEnum.Thumbnails);

                if (this.ViewModel.AppMode == AppModeEnum.Viewer)
                {
                    this.viewerUserControl.ShowImage();
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void FolderTreeView_FolderSelected(object sender, System.EventArgs e)
        {
            try
            {
                this.thumbnailsUserControl.GoToFolderAsync(this.ViewModel.Application, this.folderTreeView.SelectedPath);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void SetAsWallpaperCenter_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                this.ViewModel.Application.SetAsWallpaper(this.ViewModel?.CurrentAsset, WallpaperStyle.Center);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void SetAsWallpaperFill_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                this.ViewModel.Application.SetAsWallpaper(this.ViewModel?.CurrentAsset, WallpaperStyle.Fill);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void SetAsWallpaperFit_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                this.ViewModel.Application.SetAsWallpaper(this.ViewModel?.CurrentAsset, WallpaperStyle.Fit);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void SetAsWallpaperSpan_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                this.ViewModel.Application.SetAsWallpaper(this.ViewModel?.CurrentAsset, WallpaperStyle.Span);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void SetAsWallpaperStretch_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                this.ViewModel.Application.SetAsWallpaper(this.ViewModel?.CurrentAsset, WallpaperStyle.Stretch);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void SetAsWallpaperTile_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                this.ViewModel.Application.SetAsWallpaper(this.ViewModel?.CurrentAsset, WallpaperStyle.Tile);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void FindDuplicates_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                var duplicates = this.ViewModel.Application.GetDuplicatedAssets();

                if (duplicates.Count > 0)
                {
                    DuplicatedAssetsViewModel viewModel = new DuplicatedAssetsViewModel(this.ViewModel.Application) { DuplicatedAssetCollectionSets = duplicates };
                    DuplicatedAssetsWindow duplicatedAssetsWindow = new DuplicatedAssetsWindow(viewModel);
                    duplicatedAssetsWindow.ShowDialog();
                }
                else
                {
                    MessageBox.Show("No duplicates have been found.");
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void About_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                var about = this.ViewModel.Application.GetAboutInformation(this.GetType().Assembly);
                AboutWindow duplicatedAssetsWindow = new AboutWindow(about);
                duplicatedAssetsWindow.ShowDialog();
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void CopyFile_Click(object sender, RoutedEventArgs e)
        {
            MoveAsset(preserveOriginalFile: true);
        }

        private void MoveFile_Click(object sender, RoutedEventArgs e)
        {
            MoveAsset(preserveOriginalFile: false);
        }

        private void MoveAsset(bool preserveOriginalFile)
        {
            try
            {
                var asset = this.ViewModel.CurrentAsset;

                if (asset != null)
                {
                    FolderNavigationViewModel viewModel = new FolderNavigationViewModel(this.ViewModel.Application, asset.Folder);
                    FolderNavigationWindow folderNavigationWindow = new FolderNavigationWindow(viewModel);

                    if (folderNavigationWindow.ShowDialog().Value)
                    {
                        Folder destinationFolder = viewModel.SelectedFolder;
                        // TODO: ADD TESTS FOR NULL PARAMETERS
                        bool result = this.ViewModel.Application.MoveAsset(asset, asset.Folder, destinationFolder, preserveOriginalFile);

                        if (!preserveOriginalFile && result)
                        {
                            int position = this.ViewModel.ViewerPosition;
                            position++;

                            this.ViewModel.RemoveAsset(asset);
                            
                            if (position < this.ViewModel.Files.Count)
                            {
                                this.ViewModel.ViewerPosition = position;
                            }
                            
                            if (this.ViewModel.AppMode == AppModeEnum.Viewer)
                            {
                                this.viewerUserControl.ShowImage();
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }
    }
}
