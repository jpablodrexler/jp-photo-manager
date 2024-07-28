using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI.ViewModels;
using log4net;
using Microsoft.Toolkit.Uwp.Notifications;
using System;
using System.Diagnostics;
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
        private static readonly ILog _log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        private readonly IApplication _application;
        private Task _catalogTask;

        public MainWindow(ApplicationViewModel viewModel, IApplication application)
        {
            try
            {
                InitializeComponent();

                _application = application;
                var aboutInformation = application.GetAboutInformation(GetType().Assembly);
                viewModel.Product = aboutInformation.Product;
                viewModel.Version = aboutInformation.Version;
                DataContext = viewModel;
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        public ApplicationViewModel ViewModel
        {
            get { return (ApplicationViewModel)DataContext; }
        }

        private async void Window_Loaded(object sender, RoutedEventArgs e)
        {
            try
            {
                ViewModel?.ChangeAppMode(AppModeEnum.Thumbnails);
                thumbnailsUserControl.GoToFolder(_application, ViewModel?.CurrentFolder);
                folderTreeView.SelectedPath = ViewModel?.CurrentFolder;
                await DoBackgroundWork();
            }
            catch (Exception ex)
            {
                _log.Error(ex);
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
                            ViewModel?.GoToPreviousAsset();
                            ShowImage();
                            break;

                        case Key.PageDown:
                        case Key.Right:
                            ViewModel?.GoToNextAsset();
                            ShowImage();
                            break;

                        case Key.F1:
                            ViewModel?.ChangeAppMode();
                            ShowImage();
                            break;

                        case Key.Add:
                            if (ViewModel.AppMode == AppModeEnum.Viewer)
                            {
                                viewerUserControl.ZoomIn();
                            }
                            break;

                        case Key.Subtract:
                            if (ViewModel.AppMode == AppModeEnum.Viewer)
                            {
                                viewerUserControl.ZoomOut();
                            }
                            break;
                    }
                }
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void ShowImage()
        {
            if (ViewModel.AppMode == AppModeEnum.Viewer)
            {
                viewerUserControl.ShowImage();
            }
            else
            {
                thumbnailsUserControl.ShowImage();
            }
        }

        private void ThumbnailsUserControl_ThumbnailSelected(object sender, ThumbnailSelectedEventArgs e)
        {
            try
            {
                ViewModel?.GoToAsset(e.Asset, AppModeEnum.Viewer);
                ShowImage();
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void ViewerUserControl_ThumbnailSelected(object sender, ThumbnailSelectedEventArgs e)
        {
            try
            {
                ViewModel?.GoToAsset(e.Asset, AppModeEnum.Thumbnails);
                ShowImage();
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void FolderTreeView_FolderSelected(object sender, System.EventArgs e)
        {
            try
            {
                thumbnailsUserControl.GoToFolder(_application, folderTreeView.SelectedPath);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void SetAsWallpaperCenter_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                _application.SetAsWallpaper(ViewModel?.CurrentAsset, WallpaperStyle.Center);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void SetAsWallpaperFill_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                _application.SetAsWallpaper(ViewModel?.CurrentAsset, WallpaperStyle.Fill);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void SetAsWallpaperFit_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                _application.SetAsWallpaper(ViewModel?.CurrentAsset, WallpaperStyle.Fit);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void SetAsWallpaperSpan_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                _application.SetAsWallpaper(ViewModel?.CurrentAsset, WallpaperStyle.Span);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void SetAsWallpaperStretch_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                _application.SetAsWallpaper(ViewModel?.CurrentAsset, WallpaperStyle.Stretch);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void SetAsWallpaperTile_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                _application.SetAsWallpaper(ViewModel?.CurrentAsset, WallpaperStyle.Tile);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void FindDuplicates_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                var duplicates = _application.GetDuplicatedAssets();
                
                if (duplicates.Count > 0)
                {
                    FindDuplicatedAssetsViewModel viewModel = new(_application);
                    viewModel.SetDuplicates(duplicates);
                    DuplicatedAssetsWindow duplicatedAssetsWindow = new(viewModel);
                    duplicatedAssetsWindow.ShowDialog();
                }
                else
                {
                    MessageBox.Show("No duplicates have been found.", "Information", MessageBoxButton.OK, MessageBoxImage.Information);
                }
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void SyncAssets_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                SyncAssetsViewModel viewModel = new(_application);
                SyncAssetsWindow syncAssetsWindow = new(viewModel);
                syncAssetsWindow.ShowDialog();
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void About_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                var about = _application.GetAboutInformation(GetType().Assembly);
                AboutWindow duplicatedAssetsWindow = new(about);
                duplicatedAssetsWindow.ShowDialog();
            }
            catch (Exception ex)
            {
                _log.Error(ex);
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
                var assets = ViewModel.SelectedAssets;

                if (assets != null && assets.Length > 0)
                {
                    FolderNavigationWindow folderNavigationWindow = new(
                        new FolderNavigationViewModel(
                            _application,
                            assets.First().Folder,
                            ViewModel.LastSelectedFolder,
                            _application.GetRecentTargetPaths()));

                    folderNavigationWindow.Closed += (sender, e) =>
                    {
                        if (folderNavigationWindow.ViewModel.HasConfirmed)
                        {
                            bool result = true;

                            result = _application.MoveAssets(assets,
                                folderNavigationWindow.ViewModel.SelectedFolder,
                                preserveOriginalFiles);

                            if (result)
                            {
                                ViewModel.LastSelectedFolder = folderNavigationWindow.ViewModel.SelectedFolder;
                                ViewModel.IsRefreshingFolders = true;
                                folderTreeView.Initialize();
                                ViewModel.IsRefreshingFolders = false;

                                if (!preserveOriginalFiles)
                                {
                                    ViewModel.RemoveAssets(assets);

                                    if (ViewModel.AppMode == AppModeEnum.Viewer)
                                    {
                                        viewerUserControl.ShowImage();
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
                _log.Error(ex);
            }
        }

        private void DeleteAssets()
        {
            try
            {
                var assets = ViewModel.SelectedAssets;

                if (assets != null)
                {
                    _application.DeleteAssets(assets, deleteFiles: true);
                    ViewModel.RemoveAssets(assets);
                    ShowImage();
                }
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void DeleteAssets_Click(object sender, RoutedEventArgs e)
        {
            DeleteAssets();
        }

        private void Exit_Click(object sender, RoutedEventArgs e)
        {
            Close();
        }

        private void SortAssetsByFileName_Click(object sender, RoutedEventArgs e)
        {
            ViewModel.SortAssetsByCriteria(SortCriteriaEnum.FileName);
        }

        private void SortAssetsByFileSize_Click(object sender, RoutedEventArgs e)
        {
            ViewModel.SortAssetsByCriteria(SortCriteriaEnum.FileSize);
        }

        private void SortAssetsByFileCreationDateTime_Click(object sender, RoutedEventArgs e)
        {
            ViewModel.SortAssetsByCriteria(SortCriteriaEnum.FileCreationDateTime);
        }

        private void SortAssetsByFileModificationDateTime_Click(object sender, RoutedEventArgs e)
        {
            ViewModel.SortAssetsByCriteria(SortCriteriaEnum.FileModificationDateTime);
        }

        private void SortAssetsByThumbnailCreationDateTime_Click(object sender, RoutedEventArgs e)
        {
            ViewModel.SortAssetsByCriteria(SortCriteriaEnum.ThumbnailCreationDateTime);
        }

        private void ZoomIn_Click(object sender, RoutedEventArgs e)
        {
            if (ViewModel.AppMode == AppModeEnum.Viewer)
            {
                viewerUserControl.ZoomIn();
            }
        }

        private void ZoomOut_Click(object sender, RoutedEventArgs e)
        {
            if (ViewModel.AppMode == AppModeEnum.Viewer)
            {
                viewerUserControl.ZoomOut();
            }
        }

        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            e.Cancel = _catalogTask != null && !_catalogTask.IsCompleted;
        }

        private async Task DoBackgroundWork()
        {
            await CheckNewRelease();

            ViewModel.StatusMessage = "Cataloging thumbnails for " + ViewModel.CurrentFolder;
            int minutes = ViewModel.GetCatalogCooldownMinutes();

            while (true)
            {
                _catalogTask = ViewModel.CatalogAssets(
                    async (e) =>
                    {
                        // The InvokeAsync method is used to avoid freezing the application when the task is cancelled.
                        await Dispatcher.InvokeAsync(() => ViewModel.NotifyCatalogChange(e));
                    });

                await _catalogTask.ConfigureAwait(true);
                await Task.Delay(1000 * 60 * minutes, CancellationToken.None).ConfigureAwait(true);
            }
        }

        private async Task CheckNewRelease()
        {
            var latestRelease = await _application.CheckNewReleaseAsyc().ConfigureAwait(true);

            if (latestRelease != null && latestRelease.Success && latestRelease.IsNewRelease)
            {
                string newReleaseTitle = $"New Release {latestRelease.Name} available!";
                string newReleaseMessage = $"There is a new release of the application: {latestRelease.Name}, " +
                    $"published on {latestRelease.PublishedOn.Value.LocalDateTime.ToShortDateString()}! " +
                    $"To download it please click here.";

                new ToastContentBuilder()
                    .AddArgument("action", "viewConversation")
                    .AddArgument("conversationId", 10000)
                    .AddArgument("releaseDownloadUrl", latestRelease.DownloadUrl)
                    .AddText(newReleaseTitle)
                    .AddText(newReleaseMessage)
                    .Show();

                // Listen to notification activation
                ToastNotificationManagerCompat.OnActivated += toastArgs =>
                {
                    // Obtain the arguments from the notification
                    ToastArguments args = ToastArguments.Parse(toastArgs.Argument);
                    string releaseDownloadUrl = args["releaseDownloadUrl"];

                    if (!string.IsNullOrWhiteSpace(releaseDownloadUrl))
                    {
                        Process process = new();
                        process.StartInfo.FileName = releaseDownloadUrl;
                        process.StartInfo.UseShellExecute = true;
                        process.Start();
                    }
                };
            }
        }
    }
}
