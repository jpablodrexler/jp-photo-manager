using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.Windows;

namespace JPPhotoManager.UI.ViewModels
{
    public class FindDuplicatedAssetsViewModel : BaseViewModel<IApplication>
    {
        // TODO: IMPROVE NAMING ON CLASSES AND VARIABLES.
        private List<List<Asset>> _duplicatedAssets;
        private List<DuplicatedSetViewModel> _collection;
        private int _duplicatedAssetCollectionSetsPosition;
        private int _duplicatedAssetPosition;
		
		public FindDuplicatedAssetsViewModel(IApplication assetApp) : base(assetApp)
		{
		}

        public List<DuplicatedSetViewModel> ObservableDuplicatedAssetCollectionSets
        {
            get { return this._collection; }
            private set
            {
                this._collection = value;
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

        public void SetDuplicates(List<List<Asset>> duplicatedAssets)
        {
            if (duplicatedAssets == null)
                throw new ArgumentNullException(nameof(duplicatedAssets));

            this._duplicatedAssets = duplicatedAssets;
            List<DuplicatedSetViewModel> collection = new List<DuplicatedSetViewModel>();

            foreach (var duplicatedSet in duplicatedAssets)
            {
                DuplicatedSetViewModel duplicatedSetViewModel = new DuplicatedSetViewModel();

                foreach (var asset in duplicatedSet)
                {
                    duplicatedSetViewModel.Add(new DuplicatedAssetViewModel { Asset = asset, Visible = Visibility.Visible });
                }

                collection.Add(duplicatedSetViewModel);
            }

            this.ObservableDuplicatedAssetCollectionSets = collection;
        }

        public int DuplicatedAssetPosition
        {
            get { return this._duplicatedAssetPosition; }
            set
            {
                this._duplicatedAssetPosition = value;

                DuplicatedAssetViewModel assetViewModel = CurrentDuplicatedAsset;

                if (assetViewModel != null && assetViewModel.Asset != null && assetViewModel.Asset.ImageData == null)
                {
                    this.Application.LoadThumbnail(assetViewModel.Asset);
                }

                if (assetViewModel != null && assetViewModel.Asset != null && assetViewModel.Asset.ImageData == null)
                {
                    Refresh();
                }
                else
                {
                    this.NotifyPropertyChanged(nameof(DuplicatedAssetPosition), nameof(CurrentDuplicatedAsset));
                }
            }
        }

        public DuplicatedSetViewModel CurrentDuplicatedAssetCollection
        {
            get
            {
                DuplicatedSetViewModel result = null;

                if (this.ObservableDuplicatedAssetCollectionSets != null && this.ObservableDuplicatedAssetCollectionSets.Count > 0 && this.DuplicatedAssetCollectionSetsPosition >= 0)
                {
                    result = this.ObservableDuplicatedAssetCollectionSets[this.DuplicatedAssetCollectionSetsPosition];
                }

                return result;
            }
        }

        public DuplicatedAssetViewModel CurrentDuplicatedAsset
        {
            get
            {
                DuplicatedAssetViewModel result = null;

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

        public void DeleteAsset(DuplicatedAssetViewModel assetViewModel)
        {
            this.Application.DeleteAsset(assetViewModel.Asset, deleteFile: true);
            assetViewModel.Visible = Visibility.Hidden;
            //this.Refresh(); // TODO: SHOULD REFRESH THE OBSERVABLE COLLECTION INSTEAD.

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

    public class DuplicatedAssetViewModel
    {
        public Asset Asset { get; set; }
        public Visibility Visible { get; set; }
    }

    public class DuplicatedSetViewModel : List<DuplicatedAssetViewModel>
    {
        public string Description
        {
            get
            {
                return $"{this[0].Asset.FileName} ({this.Count} duplicates)";
            }
        }

        public bool HasDuplicates
        {
            get
            {
                return this.Count > 1;
            }
        }
    }
}
