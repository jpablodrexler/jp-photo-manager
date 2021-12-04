using JPPhotoManager.Application;
using JPPhotoManager.Domain;
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
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        public event ThumbnailSelectedEventHandler ThumbnailSelected;

        public ThumbnailsUserControl()
        {
            try
            {
                InitializeComponent();
            }
            catch (Exception ex)
            {
                log.Error(ex);
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
                    ViewModel.CurrentFolder = selectedImagePath;
                    Asset[] assets = await GetAssets(assetApp, ViewModel.CurrentFolder).ConfigureAwait(true);
                    ViewModel.SetAssets(assets);

                    if (thumbnailsListView.Items.Count > 0)
                    {
                        ViewModel.ViewerPosition = 0;
                        thumbnailsListView.ScrollIntoView(thumbnailsListView.Items[0]);
                    }
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private Task<Asset[]> GetAssets(IApplication assetApp, string folder)
        {
            return Task.Run(() => assetApp.GetAssets(folder));
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
                log.Error(ex);
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
                log.Error(ex);
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
