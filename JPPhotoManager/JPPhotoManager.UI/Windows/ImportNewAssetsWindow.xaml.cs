using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using log4net;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Reflection;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace JPPhotoManager.UI.Windows
{
    /// <summary>
    /// Interaction logic for ImportNewAssetsWindow.xaml
    /// </summary>
    public partial class ImportNewAssetsWindow : Window
    {
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        public ImportNewAssetsWindow(ImportNewAssetsViewModel viewModel)
        {
            try
            {
                InitializeComponent();

                this.DataContext = viewModel;
                this.Initialize();
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        public ImportNewAssetsViewModel ViewModel
        {
            get { return (ImportNewAssetsViewModel)this.DataContext; }
        }

        private void Initialize()
        {
            var configuration = this.ViewModel.Application.GetImportNewAssetsConfiguration();

            if (configuration == null)
            {
                configuration = new ImportNewAssetsConfiguration();
            }

            this.ViewModel.Imports = new ObservableCollection<ImportNewAssetsDirectoriesDefinition>(configuration.Imports);
        }

        private void DeleteLabel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                this.DeleteDefinition(((TextBlock)e.Source).DataContext);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void MoveUpLabel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                this.MoveUpDefinition(((TextBlock)e.Source).DataContext);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void MoveDownLabel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            try
            {
                this.MoveDownDefinition(((TextBlock)e.Source).DataContext);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void SaveButton_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                this.Cursor = Cursors.Wait;
                this.Save(this.ViewModel.Application, this.ViewModel.Imports);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
            finally
            {
                this.Cursor = Cursors.Arrow;
            }
        }

        private async void ImportButton_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                this.Cursor = Cursors.Wait;
                this.ViewModel.AdvanceStep();
                this.ViewModel.Results = await this.Import(this.ViewModel.Application, this.ViewModel.Imports).ConfigureAwait(true);
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
            finally
            {
                this.Cursor = Cursors.Arrow;
            }
        }

        private void ViewResultsButton_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                this.Cursor = Cursors.Wait;
                this.ViewModel.AdvanceStep();
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
            finally
            {
                this.Cursor = Cursors.Arrow;
            }
        }

        private void DeleteDefinition(object selected)
        {
            // Evaluates if it is an existing item or the NewItemPlaceholder.
            if (selected is ImportNewAssetsDirectoriesDefinition definition)
            {
                this.ViewModel.DeleteDefinition(definition);
            }
        }

        private void MoveUpDefinition(object selected)
        {
            // Evaluates if it is an existing item or the NewItemPlaceholder.
            if (selected is ImportNewAssetsDirectoriesDefinition definition)
            {
                this.ViewModel.MoveUpDefinition(definition);
            }
        }

        private void MoveDownDefinition(object selected)
        {
            // Evaluates if it is an existing item or the NewItemPlaceholder.
            if (selected is ImportNewAssetsDirectoriesDefinition definition)
            {
                this.ViewModel.MoveDownDefinition(definition);
            }
        }

        private void Save(IApplication assetApp, ObservableCollection<ImportNewAssetsDirectoriesDefinition> imports)
        {
            ImportNewAssetsConfiguration configuration = new ImportNewAssetsConfiguration();
            configuration.Imports.AddRange(imports);
            assetApp.SetImportNewAssetsConfiguration(configuration);
        }

        private Task<ObservableCollection<ImportNewAssetsResult>> Import(IApplication assetApp, ObservableCollection<ImportNewAssetsDirectoriesDefinition> imports)
        {
            return Task.Run(() =>
            {
                this.Save(assetApp, imports);
                var results = assetApp.ImportNewImages(e => Dispatcher.Invoke(() => ViewModel.NotifyImageImported(e)));
                return new ObservableCollection<ImportNewAssetsResult>(results);
            });
        }
    }
}
