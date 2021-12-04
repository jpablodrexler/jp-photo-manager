using System.ComponentModel;

namespace JPPhotoManager.UI.ViewModels
{
    public abstract class BaseViewModel<T> : INotifyPropertyChanged
    {
        public event PropertyChangedEventHandler PropertyChanged;

        // TODO: INJECT THIS OBJECT DIRECTLY TO THE WPF COMPONENTS INSTEAD OF HAVING THEM USE THIS PROPERTY.
        public T Application { get; private set; }

        public BaseViewModel(T application)
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
