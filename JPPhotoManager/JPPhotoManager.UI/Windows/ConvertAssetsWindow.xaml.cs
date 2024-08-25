using JPPhotoManager.Domain.Entities;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI.ViewModels;
using log4net;
using System;
using System.Collections.ObjectModel;
using System.Reflection;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;

namespace JPPhotoManager.UI.Windows
{
    /// <summary>
    /// Interaction logic for ConvertAssetsWindow.xaml
    /// </summary>
    [ExcludeFromCodeCoverage]
    public partial class ConvertAssetsWindow : Window
    {
        private static readonly ILog _log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        public ConvertAssetsWindow(ConvertAssetsViewModel viewModel)
        {
            try
            {
                InitializeComponent();

                DataContext = viewModel;
                Initialize();
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        public ConvertAssetsViewModel ViewModel
        {
            get { return (ConvertAssetsViewModel)DataContext; }
        }

        private void Initialize()
        {
            var configuration = ViewModel.GetProcessConfiguration();

            if (configuration == null)
            {
                configuration = new ConvertAssetsConfiguration();
            }

            ViewModel.Definitions = new ObservableCollection<ConvertAssetsDirectoriesDefinition>(configuration.Definitions);
        }

        private void ContinueButton_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                Cursor = Cursors.Wait;
                ViewModel.AdvanceStep();
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
            finally
            {
                Cursor = Cursors.Arrow;
            }
        }

        private void DeleteLabel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                DeleteDefinition(((FrameworkElement)e.Source).DataContext);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void MoveUpLabel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                MoveUpDefinition(((FrameworkElement)e.Source).DataContext);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void MoveDownLabel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                MoveDownDefinition(((FrameworkElement)e.Source).DataContext);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void SaveButton_MouseLeftButtonDown(object sender, RoutedEventArgs e)
        {
            try
            {
                Cursor = Cursors.Wait;
                Save(ViewModel.Definitions);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
            finally
            {
                Cursor = Cursors.Arrow;
            }
        }

        private async void RunButton_MouseLeftButtonDown(object sender, RoutedEventArgs e)
        {
            try
            {
                Cursor = Cursors.Wait;
                ViewModel.AdvanceStep();
                await RunProcess().ConfigureAwait(true);
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
            finally
            {
                Cursor = Cursors.Arrow;
            }
        }

        private void CloseButton_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                this.Close();
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void DeleteDefinition(object selected)
        {
            // Evaluates if it is an existing item or the NewItemPlaceholder.
            if (selected is ConvertAssetsDirectoriesDefinition definition)
            {
                ViewModel.DeleteDefinition(definition);
            }
        }

        private void MoveUpDefinition(object selected)
        {
            // Evaluates if it is an existing item or the NewItemPlaceholder.
            if (selected is ConvertAssetsDirectoriesDefinition definition)
            {
                ViewModel.MoveUpDefinition(definition);
            }
        }

        private void MoveDownDefinition(object selected)
        {
            // Evaluates if it is an existing item or the NewItemPlaceholder.
            if (selected is ConvertAssetsDirectoriesDefinition definition)
            {
                ViewModel.MoveDownDefinition(definition);
            }
        }

        private void Save(ObservableCollection<ConvertAssetsDirectoriesDefinition> definitions)
        {
            ConvertAssetsConfiguration configuration = new();
            configuration.Definitions.AddRange(definitions);
            ViewModel.SetProcessConfiguration(configuration);
        }

        private async Task RunProcess()
        {
            Save(ViewModel.Definitions);
            await ViewModel.RunProcessAsync(e => Dispatcher.Invoke(() => ViewModel.NotifyProcessStatusChanged(e)));
        }
    }
}
