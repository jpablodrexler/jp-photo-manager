using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI.ViewModels;
using log4net;
using System;
using System.Linq;
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
                assetApp.CatalogAssets(
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
                            MoveAssets(preserveOriginalFiles: true);
                            break;

                        case Key.M:
                            MoveAssets(preserveOriginalFiles: false);
                            break;
                    }
                }
                else
                {
                    switch (e.Key)
                    {
                        case Key.Delete:
                            DeleteAssets();
                            break;

                        case Key.PageUp:
                        case Key.Left:
                            this.ViewModel?.GoToPreviousAsset();
                            ShowImage();
                            break;

                        case Key.PageDown:
                        case Key.Right:
                            this.ViewModel?.GoToNextAsset();
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
                    FindDuplicatedAssetsViewModel viewModel = new FindDuplicatedAssetsViewModel(this.ViewModel.Application);
                    viewModel.SetDuplicates(duplicates);
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

        private void CopyAssets_Click(object sender, RoutedEventArgs e)
        {
            MoveAssets(preserveOriginalFiles: true);
        }

        private void MoveAssets_Click(object sender, RoutedEventArgs e)
        {
            MoveAssets(preserveOriginalFiles: false);
        }

        private void MoveAssets(bool preserveOriginalFiles)
        {
            try
            {
                var assets = this.ViewModel.SelectedAssets;

                if (assets != null && assets.Length > 0)
                {
                    FolderNavigationWindow folderNavigationWindow = new(
                        new FolderNavigationViewModel(
                            this.ViewModel.Application,
                            assets.First().Folder,
                            this.ViewModel.LastSelectedFolder,
                            this.ViewModel.Application.GetRecentTargetPaths()));
                    
                    folderNavigationWindow.Closed += (sender, e) =>
                    {
                        if (folderNavigationWindow.ViewModel.HasConfirmed)
                        {
                            bool result = true;

                            result = this.ViewModel.Application.MoveAssets(assets,
                                folderNavigationWindow.ViewModel.SelectedFolder,
                                preserveOriginalFiles);

                            if (result)
                            {
                                this.ViewModel.LastSelectedFolder = folderNavigationWindow.ViewModel.SelectedFolder;
                                this.ViewModel.IsRefreshingFolders = true;
                                this.folderTreeView.Initialize();
                                this.ViewModel.IsRefreshingFolders = false;

                                if (!preserveOriginalFiles)
                                {
                                    this.ViewModel.RemoveAssets(assets);

                                    if (this.ViewModel.AppMode == AppModeEnum.Viewer)
                                    {
                                        this.viewerUserControl.ShowImage();
                                    }
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

        private void DeleteAssets()
        {
            try
            {
                var assets = this.ViewModel.SelectedAssets;

                if (assets != null)
                {
                    this.ViewModel.Application.DeleteAssets(assets, deleteFiles: true);
                    this.ViewModel.RemoveAssets(assets);
                    ShowImage();
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void DeleteAssets_Click(object sender, RoutedEventArgs e)
        {
            DeleteAssets();
        }

        private void SortAssetsByFileName_Click(object sender, RoutedEventArgs e)
        {
            this.ViewModel.SortAssetsByCriteria(SortCriteriaEnum.FileName);
        }

        private void SortAssetsByFileSize_Click(object sender, RoutedEventArgs e)
        {
            this.ViewModel.SortAssetsByCriteria(SortCriteriaEnum.FileSize);
        }

        private void SortAssetsByFileCreationDateTime_Click(object sender, RoutedEventArgs e)
        {
            this.ViewModel.SortAssetsByCriteria(SortCriteriaEnum.FileCreationDateTime);
        }

        private void SortAssetsByFileModificationDateTime_Click(object sender, RoutedEventArgs e)
        {
            this.ViewModel.SortAssetsByCriteria(SortCriteriaEnum.FileModificationDateTime);
        }

        private void SortAssetsByThumbnailCreationDateTime_Click(object sender, RoutedEventArgs e)
        {
            this.ViewModel.SortAssetsByCriteria(SortCriteriaEnum.ThumbnailCreationDateTime);
        }

        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            e.Cancel = catalogTask != null && !catalogTask.IsCompleted;
        }
    }
}
