using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System.Collections.Generic;
using System.Collections.ObjectModel;

namespace JPPhotoManager.UI.ViewModels
{
    public class FolderNavigationViewModel : BaseViewModel<IApplication>
    {
        private string targetPath;

        public FolderNavigationViewModel(IApplication assetApp, Folder sourceFolder, Folder lastSelectedFolder, List<string> recentTargetPaths): base(assetApp)
        {
            this.SourceFolder = sourceFolder;
            this.LastSelectedFolder = lastSelectedFolder;
            this.RecentTargetPaths = new ObservableCollection<string>(recentTargetPaths);
        }

        public Folder SourceFolder { get; private set; }

        public Folder SelectedFolder
        {
            get { return new Folder { Path = this.TargetPath }; }
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

        public Folder LastSelectedFolder { get; private set; }
        public bool HasConfirmed { get; set; }
        public ObservableCollection<string> RecentTargetPaths { get; private set; }

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
