using JPPhotoManager.Application;
using System.ComponentModel;

namespace JPPhotoManager.UI.ViewModels
{
    public abstract class BaseViewModel : INotifyPropertyChanged
    {
        public event PropertyChangedEventHandler PropertyChanged;

        /// <summary>
        /// Gets or sets the application object.
        /// This property is declared as protected so the views
        /// always use the view model as a facade to the application object.
        /// </summary>
        protected IApplication Application { get; private set; }

        public BaseViewModel(IApplication application)
        {
            Application = application;
        }

        protected void NotifyPropertyChanged(params string[] propertyNames)
        {
            foreach (string propertyName in propertyNames)
            {
                PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
            }
        }
    }
}
