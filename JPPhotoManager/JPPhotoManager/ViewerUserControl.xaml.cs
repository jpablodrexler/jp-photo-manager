using JPPhotoManager.Domain;
using JPPhotoManager.ViewModels;
using log4net;
using System;
using System.Linq;
using System.Reflection;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace JPPhotoManager
{
    /// <summary>
    /// Interaction logic for ViewerUserControl.xaml
    /// </summary>
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

        private void nextButton_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                this.ViewModel?.GoToNextImage();
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void previousButton_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                this.ViewModel?.GoToPreviousImage();
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
                var source = this.ViewModel.Application.LoadBitmapImage(this.ViewModel.CurrentAsset.FullPath);

                if (source != null)
                {
                    this.image.Source = source;
                }
            }
            else
            {
                this.image.Source = null;
            }
        }
    }
}
