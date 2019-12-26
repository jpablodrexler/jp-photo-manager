using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace JPPhotoManager.UI.ViewModels
{
    public class FolderNavigationViewModel : BaseViewModel<IApplication>
    {
        private Folder selectedFolder;

        public FolderNavigationViewModel(IApplication assetApp, Folder sourceFolder): base(assetApp)
        {
            this.SourceFolder = sourceFolder;
        }

        public Folder SourceFolder { get; private set; }

        public Folder SelectedFolder
        {
            get { return this.selectedFolder; }
            set
            {
                this.selectedFolder = value;
                this.NotifyPropertyChanged(nameof(SelectedFolder), nameof(CanConfirm));
            }
        }

        public bool CanConfirm
        {
            get
            {
                return this.SourceFolder?.Path != this.SelectedFolder?.Path;
            }
        }
    }
}
