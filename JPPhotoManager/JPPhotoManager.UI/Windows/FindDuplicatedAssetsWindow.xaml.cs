using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI.ViewModels;
using log4net;
using System;
using System.Reflection;
using System.Windows;
using System.Windows.Input;

namespace JPPhotoManager.UI.Windows
{
    /// <summary>
    /// Interaction logic for DuplicatedAssetsWindow.xaml
    /// </summary>
    [ExcludeFromCodeCoverage]
    public partial class DuplicatedAssetsWindow : Window
    {
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        public DuplicatedAssetsWindow(FindDuplicatedAssetsViewModel viewModel)
        {
            try
            {
                InitializeComponent();

                DataContext = viewModel;
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        public FindDuplicatedAssetsViewModel ViewModel
        {
            get { return (FindDuplicatedAssetsViewModel)DataContext; }
        }

        private void DeleteLabel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                DuplicatedAssetViewModel viewModel = (DuplicatedAssetViewModel)((FrameworkElement)e.Source).DataContext;
                Asset asset = viewModel.Asset;
                ViewModel.DeleteAsset(viewModel);

                // TODO: IN THE LIST BOXES, IF THE FILENAME INCLUDES _ IT IS NOT BEING SHOWN.
                Console.WriteLine("Delete " + asset.FullPath);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void RefreshLabel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                ViewModel.Refresh();
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }
    }
}
