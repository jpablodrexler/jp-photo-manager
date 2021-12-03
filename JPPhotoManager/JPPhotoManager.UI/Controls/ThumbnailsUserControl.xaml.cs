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
            get { return (ApplicationViewModel)this.DataContext; }
        }

        public async void GoToFolder(string selectedImagePath)
        {
            try
            {
                if (!this.ViewModel.IsRefreshingFolders)
                {
                    this.ViewModel.CurrentFolder = selectedImagePath;
                    Asset[] assets = await GetAssets(this.ViewModel.CurrentFolder).ConfigureAwait(true);
                    this.ViewModel.SetAssets(assets);

                    if (this.thumbnailsListView.Items.Count > 0)
                    {
                        this.ViewModel.ViewerPosition = 0;
                        this.thumbnailsListView.ScrollIntoView(this.thumbnailsListView.Items[0]);
                    }
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private Task<Asset[]> GetAssets(string folder)
        {
            return Task.Run(() => this.ViewModel.GetAssets(folder));
        }

        private void ContentControl_MouseDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                Asset asset = (Asset)((FrameworkElement)sender).DataContext;
                this.ViewModel?.GoToAsset(asset);
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

        private void thumbnailsListView_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            this.ViewModel.SelectedAssets = this.thumbnailsListView.SelectedItems.Cast<Asset>().ToArray();
        }
    }
}
