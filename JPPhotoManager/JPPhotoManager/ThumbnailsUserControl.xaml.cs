using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.ViewModels;
using log4net;
using System;
using System.Collections.Generic;
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

        public async void GoToFolderAsync(IJPPhotoManagerApplication assetApp, string selectedImagePath)
        {
            try
            {
                this.ViewModel.CurrentFolder = selectedImagePath;
                await GetImagesAsync(assetApp, this.ViewModel.CurrentFolder);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private async Task GetImagesAsync(IJPPhotoManagerApplication assetApp, string folder)
        {
            try
            {
                var task = await Task.Run(() =>
                {
                    Asset[] result = null;

                    try
                    {
                        result = assetApp.GetAssets(folder);
                    }
                    catch (Exception ex)
                    {
                        log.Error(ex);
                    }

                    return result;
                }).ConfigureAwait(false);

                this.Dispatcher.Invoke(() =>
                {
                    // The assets that have no image data are filtered out.
                    // If a folder is being catalogued for the first time and
                    // the GetImages method is called, since the thumbnails file is not
                    // created yet, the assets catalogued so far are returned without
                    // its thumbnails.
                    task = task.Where(a => a.ImageData != null).ToArray();
                    this.ViewModel.Files = new ObservableCollection<Asset>(task);

                    if (this.thumbnailsListView.Items.Count > 0)
                    {
                        this.ViewModel.ViewerPosition = 0;
                        this.thumbnailsListView.ScrollIntoView(this.thumbnailsListView.Items[0]);
                    }
                });
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
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
