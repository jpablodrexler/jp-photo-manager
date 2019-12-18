using JPPhotoManager.Domain;
using JPPhotoManager.UI.ViewModels;
using log4net;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Reflection;
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
            this.Delete(((TextBlock)e.Source).DataContext);
        }

        private void SaveButton_Click(object sender, RoutedEventArgs e)
        {
            this.Save();
        }

        private void ImportButton_Click(object sender, RoutedEventArgs e)
        {
            this.Import();
        }

        private void Delete(object selected)
        {
            try
            {
                // Evaluates if it is an existing item or the NewItemPlaceholder.
                if (selected is ImportNewAssetsDirectoriesDefinition definition)
                {
                    this.ViewModel.RemoveDefinition(definition);
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void Save()
        {
            try
            {
                this.Cursor = Cursors.Wait;
                ImportNewAssetsConfiguration configuration = new ImportNewAssetsConfiguration();
                configuration.Imports = new List<ImportNewAssetsDirectoriesDefinition>(this.ViewModel.Imports);
                this.ViewModel.Application.SetImportNewAssetsConfiguration(configuration);
                this.Initialize();
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

        private void Import()
        {
            try
            {
                // TODO: Clear the results grid, fire the import process in an async way and display the results when completed.
                this.Cursor = Cursors.Wait;
                this.Save();
                var results = this.ViewModel.Application.ImportNewImages();
                this.ViewModel.Results = new ObservableCollection<ImportNewAssetsResult>(results);
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
    }
}
