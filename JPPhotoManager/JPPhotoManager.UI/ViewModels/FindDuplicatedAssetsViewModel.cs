using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Windows;

namespace JPPhotoManager.UI.ViewModels
{
    public class FindDuplicatedAssetsViewModel : BaseViewModel<IApplication>
    {
        private List<List<Asset>> _duplicatedAssets;
        private List<DuplicatedSetViewModel> _collection;
        private int _duplicatedAssetSetsPosition;
        private int _duplicatedAssetPosition;
		
		public FindDuplicatedAssetsViewModel(IApplication assetApp) : base(assetApp)
		{
		}

        public List<DuplicatedSetViewModel> DuplicatedAssetSetsCollection
        {
            get { return this._collection; }
            private set
            {
                this._collection = value;
                this.NotifyPropertyChanged(nameof(DuplicatedAssetSetsCollection));
                this.DuplicatedAssetSetsPosition = 0;
            }
        }

        public int DuplicatedAssetSetsPosition
        {
            get { return this._duplicatedAssetSetsPosition; }
            set
            {
                this._duplicatedAssetSetsPosition = value;
                this.NotifyPropertyChanged(nameof(DuplicatedAssetSetsPosition), nameof(CurrentDuplicatedAssetSet));
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
                    duplicatedSetViewModel.Add(
                        new DuplicatedAssetViewModel(this.Application)
                        {
                            Asset = asset,
                            Visible = Visibility.Visible,
                            ParentViewModel = duplicatedSetViewModel
                        });
                }

                collection.Add(duplicatedSetViewModel);
            }

            this.DuplicatedAssetSetsCollection = collection;
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

        public DuplicatedSetViewModel CurrentDuplicatedAssetSet
        {
            get
            {
                DuplicatedSetViewModel result = null;

                if (this.DuplicatedAssetSetsCollection != null && this.DuplicatedAssetSetsCollection.Count > 0 && this.DuplicatedAssetSetsPosition >= 0)
                {
                    result = this.DuplicatedAssetSetsCollection[this.DuplicatedAssetSetsPosition];
                }

                return result;
            }
        }

        public DuplicatedAssetViewModel CurrentDuplicatedAsset
        {
            get
            {
                DuplicatedAssetViewModel result = null;

                if (this.CurrentDuplicatedAssetSet != null && this.CurrentDuplicatedAssetSet.Count > 0 && this.DuplicatedAssetPosition >= 0)
                {
                    result = this.CurrentDuplicatedAssetSet[this.DuplicatedAssetPosition];
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
            assetViewModel.Visible = Visibility.Collapsed;
            this.NavigateToNextVisibleSet(this.DuplicatedAssetSetsPosition);
            // TODO: THE COUNTER DISPLAYED AT THE TOP OF THE SCREEN SHOULD BE UPDATED AS WELL BASED ON THE TOTAL OF DUPLICATED SETS WITH AT LEAST 2 VISIBLE ASSETS.
        }

        private void NavigateToNextVisibleSet(int currentIndex)
        {
            var nextVisibleSet = this.DuplicatedAssetSetsCollection
                .Where(s => s.Visible == Visibility.Visible
                    && this.DuplicatedAssetSetsCollection.IndexOf(s) > currentIndex)
                .FirstOrDefault();

            if (nextVisibleSet != null)
            {
                int nextIndex = this.DuplicatedAssetSetsCollection.IndexOf(nextVisibleSet);
                this.DuplicatedAssetSetsPosition = nextIndex;
            }
            else
            {
                this.NavigateToPreviousVisibleSet(currentIndex);
            }
        }

        private void NavigateToPreviousVisibleSet(int currentIndex)
        {
            var previousVisibleSet = this.DuplicatedAssetSetsCollection
                .Where(s => s.Visible == Visibility.Visible
                    && this.DuplicatedAssetSetsCollection.IndexOf(s) < currentIndex)
                .LastOrDefault();

            if (previousVisibleSet != null)
            {
                int nextIndex = this.DuplicatedAssetSetsCollection.IndexOf(previousVisibleSet);
                this.DuplicatedAssetSetsPosition = nextIndex;
            }
            else
            {
                // TODO: SHOULD DISPLAY A MESSAGE.
            }
        }
    }

    public class DuplicatedAssetViewModel : BaseViewModel<IApplication>
    {
        private Asset asset;
        private Visibility visible;

        public DuplicatedAssetViewModel(IApplication assetApp) : base(assetApp)
        {
        }

        public Asset Asset
        {
            get { return this.asset; }
            set
            {
                this.asset = value;
                this.NotifyPropertyChanged(nameof(Asset));
            }
        }

        public Visibility Visible
        {
            get { return this.visible; }
            set
            {
                this.visible = value;
                this.NotifyPropertyChanged(nameof(Visible));

                if (this.ParentViewModel != null)
                    this.ParentViewModel.NotifyAssetChanged(this);
            }
        }

        public DuplicatedSetViewModel ParentViewModel { get; internal set; }
    }

    public class DuplicatedSetViewModel : List<DuplicatedAssetViewModel>, INotifyPropertyChanged
    {
        public event PropertyChangedEventHandler PropertyChanged;

        protected void NotifyPropertyChanged(params string[] propertyNames)
        {
            foreach (string propertyName in propertyNames)
            {
                this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
            }
        }

        private int GetVisibleDuplicates()
        {
            return this.Count(vm => vm.Visible == Visibility.Visible);
        }

        public string FileName
        {
            get { return this[0].Asset.FileName; }
        }

        public int DuplicatesCount
        {
            get { return this.GetVisibleDuplicates(); }
        }

        public Visibility Visible
        {
            get
            {
                return this.GetVisibleDuplicates() > 1 ? Visibility.Visible : Visibility.Collapsed;
            }
        }

        public void NotifyAssetChanged(DuplicatedAssetViewModel asset)
        {
            this.NotifyPropertyChanged(nameof(DuplicatesCount), nameof(Visible));
        }
    }
}
