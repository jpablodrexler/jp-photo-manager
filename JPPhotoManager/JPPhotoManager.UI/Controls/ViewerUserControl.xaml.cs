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
            get { return (ApplicationViewModel)DataContext; }
        }

        private void nextButton_MouseLeftButtonDown(object sender, RoutedEventArgs e)
        {
            try
            {
                ViewModel?.GoToNextAsset();
                ShowImage();
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
                ViewModel?.GoToPreviousAsset();
                ShowImage();
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
                ThumbnailSelected?.Invoke(this, new ThumbnailSelectedEventArgs() { Asset = ViewModel.CurrentAsset });
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        public void ShowImage()
        {
            if (ViewModel.ViewerPosition >= 0)
            {
                var source = ViewModel.Application.LoadBitmapImage(ViewModel.CurrentAsset.FullPath, ViewModel.CurrentAsset.ImageRotation);

                if (source != null)
                {
                    image.Source = source;
                    backgroundImage.Source = source;
                }
            }
            else
            {
                image.Source = null;
                backgroundImage.Source = null;
            }
        }
    }
}
