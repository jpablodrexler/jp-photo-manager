using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System.Collections.Generic;

namespace JPPhotoManager.UI.ViewModels
{
    public class DuplicatedAssetsViewModel : BaseViewModel<IApplication>
    {
        private List<DuplicatedAssetCollection> _duplicatedAssetCollectionSets;
        private int _duplicatedAssetCollectionSetsPosition;
        private int _duplicatedAssetPosition;
		
		public DuplicatedAssetsViewModel(IApplication assetApp) : base(assetApp)
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

                Asset asset = CurrentDuplicatedAsset;

                if (asset != null && asset.ImageData == null)
                {
                    this.Application.LoadThumbnailAndFileInformation(asset);
                }

                if (asset != null && asset.ImageData == null)
                {
                    Refresh();
                }
                else
                {
                    this.NotifyPropertyChanged(nameof(DuplicatedAssetPosition), nameof(CurrentDuplicatedAsset));
                }
            }
        }

        public DuplicatedAssetCollection CurrentDuplicatedAssetCollection
        {
            get
            {
                DuplicatedAssetCollection result = null;

                if (this.DuplicatedAssetCollectionSets != null && this.DuplicatedAssetCollectionSets.Count > 0 && this.DuplicatedAssetCollectionSetsPosition >= 0)
                {
                    result = this.DuplicatedAssetCollectionSets[this.DuplicatedAssetCollectionSetsPosition];
                }

                return result;
            }
        }

        public Asset CurrentDuplicatedAsset
        {
            get
            {
                Asset result = null;

                if (this.CurrentDuplicatedAssetCollection != null && this.CurrentDuplicatedAssetCollection.Count > 0 && this.DuplicatedAssetPosition >= 0)
                {
                    result = this.CurrentDuplicatedAssetCollection[this.DuplicatedAssetPosition];
                }

                return result;
            }
        }

        public void Refresh()
        {
            var duplicates = this.Application.GetDuplicatedAssets();
            DuplicatedAssetCollectionSets = duplicates;
        }
    }
}
