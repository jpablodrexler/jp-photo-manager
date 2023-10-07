using JPPhotoManager.Application;
using JPPhotoManager.Domain.Entities;
using System.Collections.Generic;
using System.Collections.ObjectModel;

namespace JPPhotoManager.UI.ViewModels
{
    public class FolderNavigationViewModel : ApplicationViewModel
    {
        private string _targetPath;

        public FolderNavigationViewModel(IApplication assetApp, Folder sourceFolder, Folder lastSelectedFolder, List<string> recentTargetPaths) : base(assetApp)
        {
            SourceFolder = sourceFolder;
            LastSelectedFolder = lastSelectedFolder;
            RecentTargetPaths = new ObservableCollection<string>(recentTargetPaths);
        }

        public Folder SourceFolder { get; private set; }

        public Folder SelectedFolder
        {
            get
            {
                return !string.IsNullOrEmpty(TargetPath) ? new Folder { Path = TargetPath } : null;
            }
        }

        public bool CanConfirm
        {
            get
            {
                return SourceFolder != null
                    && SelectedFolder != null
                    && SourceFolder.Path != SelectedFolder.Path;
            }
        }

        public bool HasConfirmed { get; set; }
        public ObservableCollection<string> RecentTargetPaths { get; private set; }

        public string TargetPath
        {
            get { return _targetPath; }
            set
            {
                _targetPath = !string.IsNullOrEmpty(value) && value.EndsWith("\\") ? value[0..^1] : value;
                NotifyPropertyChanged(nameof(TargetPath), nameof(SelectedFolder), nameof(CanConfirm));
            }
        }
    }
}
