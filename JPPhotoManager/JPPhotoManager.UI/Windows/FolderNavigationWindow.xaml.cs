using JPPhotoManager.UI.ViewModels;
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

namespace JPPhotoManager.UI.Windows
{
    /// <summary>
    /// Interaction logic for FolderNavigationWindow.xaml
    /// </summary>
    public partial class FolderNavigationWindow : Window
    {
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        public FolderNavigationWindow(FolderNavigationViewModel viewModel)
        {
            try
            {
                InitializeComponent();

                this.DataContext = viewModel;
                this.folderTreeView.SelectedPath = viewModel.SourceFolder.Path;
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        public FolderNavigationViewModel ViewModel
        {
            get { return (FolderNavigationViewModel)this.DataContext; }
        }

        private void FolderTreeView_FolderSelected(object sender, System.EventArgs e)
        {
            selectedPathTextBox.Text = this.folderTreeView.SelectedPath;
        }

        private void Confirm_Click(object sender, RoutedEventArgs e)
        {
            // TODO: SHOULD VALIDATE IF THE PATH IS VALID.
            this.ViewModel.SelectedFolder = new Domain.Folder { Path = selectedPathTextBox.Text };
            this.DialogResult = true;
            this.Close();
        }

        private void Cancel_Click(object sender, RoutedEventArgs e)
        {
            this.DialogResult = false;
            this.Close();
        }
    }
}
