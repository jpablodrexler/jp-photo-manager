using JPPhotoManager.Application;
using JPPhotoManager.Domain.Entities;
using System;

namespace JPPhotoManager.UI.ViewModels
{
    public class FolderViewModel : BaseViewModel
    {
        private ApplicationViewModel _applicationViewModel;
        private bool _isExpanded;

        public FolderViewModel(ApplicationViewModel applicationViewModel, IApplication application) : base(application)
        {
            _applicationViewModel = applicationViewModel;

            if (_applicationViewModel != null)
                _applicationViewModel.PropertyChanged += _applicationViewModel_PropertyChanged;
        }

        private void _applicationViewModel_PropertyChanged(object sender, System.ComponentModel.PropertyChangedEventArgs e)
        {
            if (e.PropertyName == nameof(ApplicationViewModel.CurrentFolder))
            {
                NotifyPropertyChanged(nameof(IsExpanded));
                NotifyPropertyChanged(nameof(ImageSource));
            }
        }

        public bool IsExpanded
        {
            get { return _isExpanded || string.Compare(Folder.Path, _applicationViewModel?.CurrentFolder, StringComparison.OrdinalIgnoreCase) == 0; }
            set
            {
                _isExpanded = value;
                NotifyPropertyChanged(nameof(IsExpanded));
                NotifyPropertyChanged(nameof(ImageSource));
            }
        }

        public string ImageSource => IsExpanded ? "/Images/FolderOpenIcon.png" : "/Images/FolderClosedIcon.png";

        public Folder Folder { get; set; }

        ~FolderViewModel()
        {
            if (_applicationViewModel != null)
                _applicationViewModel.PropertyChanged -= _applicationViewModel_PropertyChanged;
        }
    }
}
