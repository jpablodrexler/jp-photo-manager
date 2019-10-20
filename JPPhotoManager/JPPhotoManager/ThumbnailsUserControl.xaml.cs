using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.ViewModels;
using log4net;
using System;
using System.Collections.ObjectModel;
using System.Linq;
using System.Reflection;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace JPPhotoManager
{
    /// <summary>
    /// Interaction logic for ThumbnailsUserControl.xaml
    /// </summary>
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
            get { return (ApplicationViewModel)this.DataContext; }
        }

        public async void GoToFolder(IJPPhotoManagerApplication assetApp, string selectedImagePath)
        {
            try
            {
                this.ViewModel.CurrentFolder = selectedImagePath;
                Asset[] assets = await GetAssets(assetApp, ViewModel.CurrentFolder).ConfigureAwait(true);

                // The assets that have no image data are filtered out.
                // If a folder is being catalogued for the first time and
                // the GetImages method is called, since the thumbnails file is not
                // created yet, the assets catalogued so far are returned without
                // its thumbnails.
                assets = assets.Where(a => a.ImageData != null).ToArray();
                this.ViewModel.Files = new ObservableCollection<Asset>(assets);

                if (this.thumbnailsListView.Items.Count > 0)
                {
                    this.ViewModel.ViewerPosition = 0;
                    this.thumbnailsListView.ScrollIntoView(this.thumbnailsListView.Items[0]);
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private Task<Asset[]> GetAssets(IJPPhotoManagerApplication assetApp, string folder)
        {
            return Task.Run(() => assetApp.GetAssets(folder));
        }

        private void ContentControl_MouseDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                Asset asset = (Asset)((FrameworkElement)sender).DataContext;
                this.ViewModel?.GoToImage(asset);
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
                this.ThumbnailSelected?.Invoke(this, new ThumbnailSelectedEventArgs() { Asset = asset });
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        public void ShowImage()
        {
            if (this.thumbnailsListView.Items.Count > 0 && this.thumbnailsListView.SelectedItem != null)
            {
                this.thumbnailsListView.ScrollIntoView(this.thumbnailsListView.SelectedItem);
            }
        }
    }
}
