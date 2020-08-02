using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI.ViewModels;
using log4net;
using System;
using System.Reflection;
using System.Windows;

namespace JPPhotoManager.UI.Windows
{
    /// <summary>
    /// Interaction logic for FolderNavigationWindow.xaml
    /// </summary>
    [ExcludeFromCodeCoverage]
    public partial class FolderNavigationWindow : Window
    {
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        public FolderNavigationWindow(FolderNavigationViewModel viewModel)
        {
            try
            {
                InitializeComponent();

                this.DataContext = viewModel;
                this.folderTreeView.SelectedPath = viewModel.LastSelectedFolder != null ? viewModel.LastSelectedFolder.Path : viewModel.SourceFolder.Path;
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
            this.ViewModel.SelectedFolder = new Domain.Folder { Path = selectedPathTextBox.Text };
        }

        private void Confirm_MouseLeftButtonDown(object sender, RoutedEventArgs e)
        {
            // TODO: SHOULD VALIDATE IF THE PATH IS VALID.
            this.ViewModel.SelectedFolder = new Domain.Folder { Path = selectedPathTextBox.Text };
            this.ViewModel.HasConfirmed = true;
            this.Close();
        }

        private void Cancel_MouseLeftButtonDown(object sender, RoutedEventArgs e)
        {
            this.ViewModel.HasConfirmed = false;
            this.Close();
        }
    }
}
