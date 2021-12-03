using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System.Collections.Generic;
using System.Collections.ObjectModel;

namespace JPPhotoManager.UI.ViewModels
{
    public class FolderNavigationViewModel : ApplicationViewModel
    {
        private string targetPath;

        public FolderNavigationViewModel(IApplication assetApp): base(assetApp)
        {
        }

        public Folder SourceFolder { get; set; }

        public Folder SelectedFolder
        {
            get
            {
                return !string.IsNullOrEmpty(this.TargetPath) ? new Folder { Path = this.TargetPath } : null;
            }
        }

        public bool CanConfirm
        {
            get
            {
                return this.SourceFolder != null
                    && this.SelectedFolder != null
                    && this.SourceFolder.Path != this.SelectedFolder.Path;
            }
        }

        public Folder LastSelectedFolder { get; set; }
        public bool HasConfirmed { get; set; }
        public ObservableCollection<string> RecentTargetPaths { get; set; }

        public string TargetPath
        {
            get { return this.targetPath; }
            set
            {
                this.targetPath = !string.IsNullOrEmpty(value) && value.EndsWith("\\") ? value.Substring(0, value.Length - 1) : value;
                this.NotifyPropertyChanged(nameof(TargetPath), nameof(SelectedFolder), nameof(CanConfirm));
            }
        }
    }
}
