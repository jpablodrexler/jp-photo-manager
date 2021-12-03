using System.ComponentModel;

namespace JPPhotoManager.UI.ViewModels
{
    public abstract class BaseViewModel<T> : INotifyPropertyChanged
    {
        public event PropertyChangedEventHandler PropertyChanged;

        protected T Application { get; private set; }

        public BaseViewModel(T application)
        {
            this.Application = application;
        }

        protected void NotifyPropertyChanged(params string[] propertyNames)
        {
            foreach (string propertyName in propertyNames)
            {
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
            }
        }
    }
}
