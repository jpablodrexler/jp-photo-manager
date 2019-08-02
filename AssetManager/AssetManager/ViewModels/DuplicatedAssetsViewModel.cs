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
    public class DuplicatedAssetsViewModel : BaseViewModel<IAssetManagerApplication>
    {
        private List<DuplicatedAssetCollection> _duplicatedAssetCollectionSets;
        private int _duplicatedAssetCollectionSetsPosition;
        private int _duplicatedAssetPosition;

        public DuplicatedAssetsViewModel(IAssetManagerApplication assetApp) : base(assetApp)
        {
        }

        public List<DuplicatedAssetCollection> DuplicatedAssetCollectionSets
        {
            get { return this._duplicatedAssetCollectionSets; }
            set
            {
                this._duplicatedAssetCollectionSets = value;
                this.NotifyPropertyChanged(nameof(DuplicatedAssetCollectionSets));
                this.DuplicatedAssetCollectionSetsPosition = 0;
            }
        }

        public int DuplicatedAssetCollectionSetsPosition
        {
            get { return this._duplicatedAssetCollectionSetsPosition; }
            set
            {
                this._duplicatedAssetCollectionSetsPosition = value;
                this.NotifyPropertyChanged(nameof(DuplicatedAssetCollectionSetsPosition), nameof(CurrentDuplicatedAssetCollection));
                this.DuplicatedAssetPosition = 0;
            }
        }

        public int DuplicatedAssetPosition
        {
            get { return this._duplicatedAssetPosition; }
            set
            {
                this._duplicatedAssetPosition = value;
                this.NotifyPropertyChanged(nameof(DuplicatedAssetPosition), nameof(CurrentDuplicatedAsset));
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
