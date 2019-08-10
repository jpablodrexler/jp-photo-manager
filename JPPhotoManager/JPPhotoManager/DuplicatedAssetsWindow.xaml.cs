using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.ViewModels;
using log4net;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;

namespace JPPhotoManager
{
    /// <summary>
    /// Interaction logic for DuplicatedAssetsWindow.xaml
    /// </summary>
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
                Asset asset = (Asset)((TextBlock)e.Source).DataContext;

                if (MessageBox.Show($"Are you sure you want to delete {asset.FullPath}?", "Confirm", MessageBoxButton.OKCancel) == MessageBoxResult.OK)
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
    }
}
