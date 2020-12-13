using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System.Collections.Generic;
using System.Collections.ObjectModel;

namespace JPPhotoManager.UI.ViewModels
{
    public class DuplicatedAssetsViewModel : BaseViewModel<IApplication>
    {
        // TODO: IMPROVE NAMING ON CLASSES AND VARIABLES.
        private List<DuplicatedAssetCollection> _duplicatedAssets;
        private ObservableCollection<DuplicatedAssetCollection> _observableDuplicatedAssetCollectionSets;
        private int _duplicatedAssetCollectionSetsPosition;
        private int _duplicatedAssetPosition;
		
		public DuplicatedAssetsViewModel(IApplication assetApp) : base(assetApp)
		{
		}

        public ObservableCollection<DuplicatedAssetCollection> ObservableDuplicatedAssetCollectionSets
        {
            get { return this._observableDuplicatedAssetCollectionSets; }
            private set
            {
                this._observableDuplicatedAssetCollectionSets = value;
                this.NotifyPropertyChanged(nameof(ObservableDuplicatedAssetCollectionSets));
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

        public void SetDuplicates(List<DuplicatedAssetCollection> duplicatedAssets)
        {
            this._duplicatedAssets = duplicatedAssets;
            this.ObservableDuplicatedAssetCollectionSets = new ObservableCollection<DuplicatedAssetCollection>(duplicatedAssets);
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

                if (this.ObservableDuplicatedAssetCollectionSets != null && this.ObservableDuplicatedAssetCollectionSets.Count > 0 && this.DuplicatedAssetCollectionSetsPosition >= 0)
                {
                    result = this.ObservableDuplicatedAssetCollectionSets[this.DuplicatedAssetCollectionSetsPosition];
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
            SetDuplicates(duplicates);
        }

        public void RemoveDuplicatedAsset(Asset asset)
        {
            this.Application.DeleteAsset(asset, deleteFile: true);
            this.Refresh(); // TODO: SHOULD REFRESH THE OBSERVABLE COLLECTION INSTEAD.

            // TODO: INSTEAD OF REMOVING FROM THE COLLECTION, SHOULD FILTER IF THE DUPLICATED ASSETS ON SET > 1
            //var duplicatedSet = this.DuplicatedAssetCollectionSets[this.DuplicatedAssetCollectionSetsPosition];
            //duplicatedSet.Remove(asset);

            //if (!duplicatedSet.HasDuplicates)
            //{
            //    this.DuplicatedAssetCollectionSets.Remove(duplicatedSet);
            //}
        }

        public void RemoveFromParentFolder()
        {
            this.Application.RemoveDuplicatesFromParentFolder(this._duplicatedAssets);
        }
    }
}
