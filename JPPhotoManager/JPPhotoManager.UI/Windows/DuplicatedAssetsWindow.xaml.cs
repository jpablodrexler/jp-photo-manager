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

        public DuplicatedAssetsWindow(DuplicatedAssetsViewModel viewModel)
        {
            try
            {
                InitializeComponent();

                this.DataContext = viewModel;
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        public DuplicatedAssetsViewModel ViewModel
        {
            get { return (DuplicatedAssetsViewModel)this.DataContext; }
        }

        private void DeleteLabel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                Asset asset = (Asset)((FrameworkElement)e.Source).DataContext;

                if (MessageBox.Show($"Are you sure you want to delete {asset.FullPath}?", "Confirm", MessageBoxButton.OKCancel, MessageBoxImage.Question) == MessageBoxResult.OK)
                {
                    this.ViewModel.Application.DeleteAsset(asset, deleteFile: true);
                    var duplicates = this.ViewModel.Application.GetDuplicatedAssets();
                    this.ViewModel.DuplicatedAssetCollectionSets = duplicates;
                }

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
                this.ViewModel.Refresh();
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }
    }
}
