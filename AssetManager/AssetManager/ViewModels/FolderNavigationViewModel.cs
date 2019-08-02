using AssetManager.Application;
using AssetManager.Domain;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AssetManager.ViewModels
{
    public class FolderNavigationViewModel : BaseViewModel<IAssetManagerApplication>
    {
        private Folder selectedFolder;

        public FolderNavigationViewModel(IAssetManagerApplication assetApp, Folder sourceFolder): base(assetApp)
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
