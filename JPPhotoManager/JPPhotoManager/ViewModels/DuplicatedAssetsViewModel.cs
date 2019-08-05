using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace JPPhotoManager.ViewModels
{
    public class DuplicatedAssetsViewModel : INotifyPropertyChanged
    {
        public event PropertyChangedEventHandler PropertyChanged;

        private List<DuplicatedAssetCollection> _duplicatedAssetCollectionSets;
        private int _duplicatedAssetCollectionSetsPosition;
        private int _duplicatedAssetPosition;

        public IJPPhotoManagerApplication AssetApp { get; private set; }

        public DuplicatedAssetsViewModel(IJPPhotoManagerApplication assetApp)
        {
            this.AssetApp = assetApp;
        }

        public List<DuplicatedAssetCollection> DuplicatedAssetCollectionSets
        {
            get { return this._duplicatedAssetCollectionSets; }
            set
            {
                this._duplicatedAssetCollectionSets = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("DuplicatedAssetCollectionSets"));
                this.DuplicatedAssetCollectionSetsPosition = 0;
            }
        }

        public int DuplicatedAssetCollectionSetsPosition
        {
            get { return this._duplicatedAssetCollectionSetsPosition; }
            set
            {
                this._duplicatedAssetCollectionSetsPosition = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("DuplicatedAssetCollectionSetsPosition"));
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("CurrentDuplicatedAssetCollection"));
                this.DuplicatedAssetPosition = 0;
            }
        }

        public int DuplicatedAssetPosition
        {
            get { return this._duplicatedAssetPosition; }
            set
            {
                this._duplicatedAssetPosition = value;
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("DuplicatedAssetPosition"));
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("CurrentDuplicatedAsset"));
            }
        }

        public DuplicatedAssetCollection CurrentDuplicatedAssetCollection
        {
            get { return this.DuplicatedAssetCollectionSets?.Count > 0 && this.DuplicatedAssetCollectionSetsPosition >= 0 ? this.DuplicatedAssetCollectionSets?[this.DuplicatedAssetCollectionSetsPosition] : null; }
        }

        public Asset CurrentDuplicatedAsset
        {
            get { return this.CurrentDuplicatedAssetCollection?.Count > 0 && this.DuplicatedAssetPosition >= 0 ? this.CurrentDuplicatedAssetCollection?[this.DuplicatedAssetPosition] : null; }
        }
    }
}
