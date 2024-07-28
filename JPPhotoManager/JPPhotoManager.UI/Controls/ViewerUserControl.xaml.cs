using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI.ViewModels;
using JPPhotoManager.UI.Windows;
using log4net;
using System;
using System.Reflection;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media.Imaging;

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
        private const double SCALE_STEP = 0.05;

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
                var source = ViewModel.LoadBitmapImage();

                if (source != null)
                {
                    double currentScale = GetDefaultScale(source);

                    scaleTransform.ScaleX = currentScale;
                    scaleTransform.ScaleY = currentScale;
                    ViewModel.ViewerZoom = currentScale;
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

        private double GetDefaultScale(BitmapImage source)
        {
            double imageHeight = source.Height;
            double imageWidth = source.Width;

            double availableHeight = scrollViewer.ActualHeight;
            double availableWidth = scrollViewer.ActualWidth;

            double scaleX = availableWidth / imageWidth;
            double scaleY = availableHeight / imageHeight;

            var currentScale = Math.Min(scaleX, scaleY);

            return currentScale;
        }

        public void ZoomIn()
        {
            var currentScale = scaleTransform.ScaleX;
            currentScale += SCALE_STEP;
            scaleTransform.ScaleX = currentScale;
            scaleTransform.ScaleY = currentScale;
            ViewModel.ViewerZoom = currentScale;
        }

        public void ZoomOut()
        {
            if (CanZoom())
            {
                var currentScale = scaleTransform.ScaleX;
                currentScale -= SCALE_STEP;

                if (currentScale < SCALE_STEP)
                {
                    currentScale = SCALE_STEP;
                }

                scaleTransform.ScaleX = currentScale;
                scaleTransform.ScaleY = currentScale;
                ViewModel.ViewerZoom = currentScale;
            }
        }

        private bool CanZoom()
        {
            return scrollViewer.ComputedHorizontalScrollBarVisibility == Visibility.Visible ||
                scrollViewer.ComputedVerticalScrollBarVisibility == Visibility.Visible;
        }
    }
}
