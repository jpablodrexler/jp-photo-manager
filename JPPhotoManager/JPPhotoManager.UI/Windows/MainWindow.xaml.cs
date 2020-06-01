using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI.ViewModels;
using log4net;
using System;
using System.Reflection;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;

namespace JPPhotoManager.UI.Windows
{
    public class ThumbnailSelectedEventArgs : EventArgs
    {
        public Asset Asset { get; set; }
    }

    public delegate void ThumbnailSelectedEventHandler(object sender, ThumbnailSelectedEventArgs e);

    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    [ExcludeFromCodeCoverage]
    public partial class MainWindow : Window
    {
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        Task catalogTask;

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

        private Task CatalogImages(IApplication assetApp)
        {
            return Task.Run(() =>
            {
                assetApp.CatalogImages(
                    async (e) =>
                    {
                        // The InvokeAsync method is used to avoid freezing the application when the task is cancelled.
                        await Dispatcher.InvokeAsync(() => ViewModel.NotifyCatalogChange(e));
                    });
            }, CancellationToken.None);
        }

        private async void Window_Loaded(object sender, RoutedEventArgs e)
        {
            try
            {
                this.ViewModel?.ChangeAppMode(AppModeEnum.Thumbnails);
                this.thumbnailsUserControl.GoToFolder(this.ViewModel.Application, this.ViewModel?.CurrentFolder);
                this.folderTreeView.SelectedPath = this.ViewModel?.CurrentFolder;

                ViewModel.StatusMessage = "Cataloging thumbnails for " + ViewModel.CurrentFolder;
                int minutes = this.ViewModel.Application.GetCatalogCooldownMinutes();
                
                while (true)
                {
                    catalogTask = this.CatalogImages(this.ViewModel.Application);
                    await catalogTask.ConfigureAwait(true);
                    await Task.Delay(1000 * 60 * minutes, CancellationToken.None).ConfigureAwait(true);
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
            finally
            {
                ViewModel.StatusMessage = "";
            }
        }

        private void Window_KeyDown(object sender, KeyEventArgs e)
        {
            try
            {
                if (e.KeyboardDevice.IsKeyDown(Key.LeftCtrl) || e.KeyboardDevice.IsKeyDown(Key.RightCtrl))
                {
                    switch (e.Key)
                    {
                        case Key.C:
                            MoveAsset(preserveOriginalFile: true);
                            break;

                        case Key.M:
                            MoveAsset(preserveOriginalFile: false);
                            break;
                    }
                }
                else
                {
                    switch (e.Key)
                    {
                        case Key.PageUp:
                        case Key.Left:
                            this.ViewModel?.GoToPreviousImage();
                            ShowImage();
                            break;

                        case Key.PageDown:
                        case Key.Right:
                            this.ViewModel?.GoToNextImage();
                            ShowImage();
                            break;

                        case Key.F1:
                            this.ViewModel?.ChangeAppMode();
                            ShowImage();
                            break;
                    }
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void ShowImage()
        {
            if (this.ViewModel.AppMode == AppModeEnum.Viewer)
            {
                this.viewerUserControl.ShowImage();
            }
            else
            {
                this.thumbnailsUserControl.ShowImage();
            }
        }

        private void ThumbnailsUserControl_ThumbnailSelected(object sender, ThumbnailSelectedEventArgs e)
        {
            try
            {
                this.ViewModel?.GoToAsset(e.Asset, AppModeEnum.Viewer);
                ShowImage();
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
                this.ViewModel?.GoToAsset(e.Asset, AppModeEnum.Thumbnails);
                ShowImage();
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
                this.thumbnailsUserControl.GoToFolder(this.ViewModel.Application, this.folderTreeView.SelectedPath);
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
                    MessageBox.Show("No duplicates have been found.", "Information", MessageBoxButton.OK, MessageBoxImage.Information);
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void ImportNewAssets_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                ImportNewAssetsViewModel viewModel = new ImportNewAssetsViewModel(this.ViewModel.Application);
                ImportNewAssetsWindow importNewAssetsWindow = new ImportNewAssetsWindow(viewModel);
                importNewAssetsWindow.ShowDialog();
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

        private void CopyAsset_Click(object sender, RoutedEventArgs e)
        {
            MoveAsset(preserveOriginalFile: true);
        }

        private void MoveAsset_Click(object sender, RoutedEventArgs e)
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
                    FolderNavigationViewModel viewModel = new FolderNavigationViewModel(this.ViewModel.Application, asset.Folder, this.ViewModel.LastSelectedFolder);
                    FolderNavigationWindow folderNavigationWindow = new FolderNavigationWindow(viewModel);
                    
                    folderNavigationWindow.Closed += (sender, e) =>
                    {
                        if (viewModel.HasConfirmed)
                        {
                            Folder destinationFolder = viewModel.SelectedFolder;
                            bool result = this.ViewModel.Application.MoveAsset(asset, destinationFolder, preserveOriginalFile);
                            this.ViewModel.LastSelectedFolder = viewModel.SelectedFolder;

                            if (!preserveOriginalFile && result)
                            {
                                this.ViewModel.RemoveAsset(asset);

                                if (this.ViewModel.AppMode == AppModeEnum.Viewer)
                                {
                                    this.viewerUserControl.ShowImage();
                                }
                            }
                        }
                    };

                    folderNavigationWindow.Show();
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void DeleteAsset()
        {
            try
            {
                var asset = this.ViewModel.CurrentAsset;

                if (asset != null)
                {
                    this.ViewModel.Application.DeleteAsset(asset, deleteFile: true);
                    this.ViewModel.RemoveAsset(asset);
                    ShowImage();
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void DeleteAsset_Click(object sender, RoutedEventArgs e)
        {
            DeleteAsset();
        }

        private void SortAssetsByFileName_Click(object sender, RoutedEventArgs e)
        {
            this.ViewModel.SortAssetsByFileName();
        }

        private void SortAssetsByFileCreationDateTime_Click(object sender, RoutedEventArgs e)
        {
            this.ViewModel.SortAssetsByFileCreationDateTime();
        }

        private void SortAssetsByFileModificationDateTime_Click(object sender, RoutedEventArgs e)
        {
            this.ViewModel.SortAssetsByFileModificationDateTime();
        }

        private void SortAssetsByThumbnailCreationDateTime_Click(object sender, RoutedEventArgs e)
        {
            this.ViewModel.SortAssetsByThumbnailCreationDateTime();
        }

        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            e.Cancel = catalogTask != null && !catalogTask.IsCompleted;
        }
    }
}
