using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI.ViewModels;
using JPPhotoManager.UI.Windows;
using log4net;
using System;
using System.Reflection;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace JPPhotoManager.UI.Controls
{
    /// <summary>
    /// Interaction logic for ViewerUserControl.xaml
    /// </summary>
    [ExcludeFromCodeCoverage]
    public partial class ViewerUserControl : UserControl
    {
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        public event ThumbnailSelectedEventHandler ThumbnailSelected;
        
        public ViewerUserControl()
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

        private void nextButton_MouseLeftButtonDown(object sender, RoutedEventArgs e)
        {
            try
            {
                this.ViewModel?.GoToNextAsset();
                this.ShowImage();
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void previousButton_MouseLeftButtonDown(object sender, RoutedEventArgs e)
        {
            try
            {
                this.ViewModel?.GoToPreviousAsset();
                this.ShowImage();
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
                this.ThumbnailSelected?.Invoke(this, new ThumbnailSelectedEventArgs() { Asset = this.ViewModel.CurrentAsset });
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        public void ShowImage()
        {
            if (this.ViewModel.ViewerPosition >= 0)
            {
                var source = this.ViewModel.LoadCurrentAssetBitmapImage();

                if (source != null)
                {
                    this.image.Source = source;
                    this.backgroundImage.Source = source;
                }
            }
            else
            {
                this.image.Source = null;
                this.backgroundImage.Source = null;
            }
        }
    }
}
