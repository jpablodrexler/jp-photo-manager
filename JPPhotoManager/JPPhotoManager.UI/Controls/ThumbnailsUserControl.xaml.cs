using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI.ViewModels;
using JPPhotoManager.UI.Windows;
using log4net;
using System;
using System.Linq;
using System.Reflection;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace JPPhotoManager.UI.Controls
{
    /// <summary>
    /// Interaction logic for ThumbnailsUserControl.xaml
    /// </summary>
    [ExcludeFromCodeCoverage]
    public partial class ThumbnailsUserControl : UserControl
    {
        private static readonly ILog _log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        public event ThumbnailSelectedEventHandler ThumbnailSelected;

        public ThumbnailsUserControl()
        {
            try
            {
                InitializeComponent();
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private ApplicationViewModel ViewModel
        {
            get { return (ApplicationViewModel)DataContext; }
        }

        public async void GoToFolder(IApplication assetApp, string selectedImagePath)
        {
            try
            {
                if (!ViewModel.IsRefreshingFolders)
                {
                    if (ViewModel.CurrentFolder != selectedImagePath)
                    {
                        ViewModel.CurrentFolder = selectedImagePath;
                    }

                    await LoadAssetsWithPagination(assetApp);
                }
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private async Task LoadAssetsWithPagination(IApplication assetApp)
        {
            PaginatedData<Asset> assets;
            int pageNumber = 0;

            try
            {
                ViewModel.IsLoading = true;

                do
                {
                    assets = await GetAssets(assetApp, ViewModel.CurrentFolder, pageNumber).ConfigureAwait(true);
                    ViewModel.SetPaginatedAssets(assets);

                    

                    if (pageNumber == 0 && thumbnailsListView.Items.Count > 0)
                    {
                        ViewModel.ViewerPosition = 0;
                        thumbnailsListView.ScrollIntoView(thumbnailsListView.Items[0]);
                    }

                    pageNumber++;
                }
                while (assets != null && assets.Items?.Length > 0);
            }
            finally
            {
                ViewModel.IsLoading = false;
            }
        }

        private Task<PaginatedData<Asset>> GetAssets(IApplication assetApp, string folder, int pageIndex)
        {
            return Task.Run(() => assetApp.GetAssets(folder, pageIndex));
        }

        private void ContentControl_MouseDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                Asset asset = (Asset)((FrameworkElement)sender).DataContext;
                ViewModel?.GoToAsset(asset);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void ContentControl_MouseDoubleClick(object sender, MouseButtonEventArgs e)
        {
            try
            {
                Asset asset = (Asset)((FrameworkElement)sender).DataContext;
                ThumbnailSelected?.Invoke(this, new ThumbnailSelectedEventArgs() { Asset = asset });
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        public void ShowImage()
        {
            if (thumbnailsListView.Items.Count > 0 && thumbnailsListView.SelectedItem != null)
            {
                thumbnailsListView.ScrollIntoView(thumbnailsListView.SelectedItem);
            }
        }

        private void thumbnailsListView_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            ViewModel.SelectedAssets = thumbnailsListView.SelectedItems.Cast<Asset>().ToArray();
        }
    }
}
