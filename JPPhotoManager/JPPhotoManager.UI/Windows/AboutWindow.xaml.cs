using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using log4net;
using System;
using System.Reflection;
using System.Windows;

namespace JPPhotoManager.UI.Windows
{
    /// <summary>
    /// Interaction logic for AboutWindow.xaml
    /// </summary>
    [ExcludeFromCodeCoverage]
    public partial class AboutWindow : Window
    {
        private static readonly ILog _log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);

        public AboutWindow(AboutInformation aboutInformation)
        {
            try
            {
                InitializeComponent();

                DataContext = aboutInformation;
                Title = $"About {aboutInformation.Product} {aboutInformation.Version}";
            }
            catch (Exception ex)
            {
                _log.Error(ex);
            }
        }

        private void ConfirmButton_MouseLeftButtonDown(object sender, RoutedEventArgs e)
        {
            Close();
        }
    }
}
