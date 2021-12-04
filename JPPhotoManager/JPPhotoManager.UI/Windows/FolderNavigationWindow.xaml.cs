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

                DataContext = viewModel;
                folderTreeView.SelectedPath = viewModel.LastSelectedFolder != null ? viewModel.LastSelectedFolder.Path : viewModel.SourceFolder.Path;
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        public FolderNavigationViewModel ViewModel
        {
            get { return (FolderNavigationViewModel)DataContext; }
        }

        private void FolderTreeView_FolderSelected(object sender, EventArgs e)
        {
            ViewModel.TargetPath = folderTreeView.SelectedPath;
        }

        private void Confirm_MouseLeftButtonDown(object sender, RoutedEventArgs e)
        {
            Confirm();
        }

        private void Cancel_MouseLeftButtonDown(object sender, RoutedEventArgs e)
        {
            Cancel();
        }

        private void Window_KeyDown(object sender, KeyEventArgs e)
        {
            switch (e.Key)
            {
                case Key.Enter:
                    Confirm();
                    break;

                case Key.Escape:
                    Cancel();
                    break;
            }
        }

        private void Confirm()
        {
            // TODO: SHOULD VALIDATE IF THE PATH IS VALID.
            //this.ViewModel.SelectedFolder = new Domain.Folder { Path = this.ViewModel.TargetPath };
            ViewModel.HasConfirmed = true;
            Close();
        }

        private void Cancel()
        {
            ViewModel.HasConfirmed = false;
            Close();
        }
    }
}
