using JPPhotoManager.Application;
using JPPhotoManager.Domain;

namespace JPPhotoManager.UI.ViewModels
{
    public class FolderViewModel : BaseViewModel
    {
        private bool _isExpanded;

        public FolderViewModel(IApplication application) : base(application)
        {
        }

        public bool IsExpanded
        {
            get { return _isExpanded; }
            set
            {
                _isExpanded = value;
                NotifyPropertyChanged(nameof(IsExpanded));
                NotifyPropertyChanged(nameof(ImageSource));
            }
        }

        public string ImageSource => IsExpanded ? "/Images/FolderOpenIcon.png" : "/Images/FolderClosedIcon.png";

        public Folder Folder { get; set; }
    }
}
