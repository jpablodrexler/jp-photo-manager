using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Windows;

namespace JPPhotoManager.UI.ViewModels
{
    public class FindDuplicatedAssetsViewModel : BaseViewModel
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
            get { return _collection; }
            private set
            {
                _collection = value;
                NotifyPropertyChanged(nameof(DuplicatedAssetSetsCollection));
                DuplicatedAssetSetsPosition = 0;
            }
        }

        public int DuplicatedAssetSetsPosition
        {
            get { return _duplicatedAssetSetsPosition; }
            set
            {
                _duplicatedAssetSetsPosition = value;
                NotifyPropertyChanged(nameof(DuplicatedAssetSetsPosition), nameof(CurrentDuplicatedAssetSet));
                DuplicatedAssetPosition = 0;
            }
        }

        public void SetDuplicates(List<List<Asset>> duplicatedAssets)
        {
            if (duplicatedAssets == null)
                throw new ArgumentNullException(nameof(duplicatedAssets));

            _duplicatedAssets = duplicatedAssets;
            List<DuplicatedSetViewModel> collection = new();

            foreach (var duplicatedSet in duplicatedAssets)
            {
                DuplicatedSetViewModel duplicatedSetViewModel = new();

                foreach (var asset in duplicatedSet)
                {
                    duplicatedSetViewModel.Add(
                        new DuplicatedAssetViewModel(Application)
                        {
                            Asset = asset,
                            Visible = Visibility.Visible,
                            ParentViewModel = duplicatedSetViewModel
                        });
                }

                collection.Add(duplicatedSetViewModel);
            }

            DuplicatedAssetSetsCollection = collection;
        }

        public int DuplicatedAssetPosition
        {
            get { return _duplicatedAssetPosition; }
            set
            {
                _duplicatedAssetPosition = value;

                DuplicatedAssetViewModel assetViewModel = CurrentDuplicatedAsset;

                if (assetViewModel != null && assetViewModel.Asset != null && assetViewModel.Asset.ImageData == null)
                {
                    Application.LoadThumbnail(assetViewModel.Asset);
                }

                if (assetViewModel != null && assetViewModel.Asset != null && assetViewModel.Asset.ImageData == null)
                {
                    Refresh();
                }
                else
                {
                    NotifyPropertyChanged(nameof(DuplicatedAssetPosition), nameof(CurrentDuplicatedAsset));
                }
            }
        }

        public DuplicatedSetViewModel CurrentDuplicatedAssetSet
        {
            get
            {
                DuplicatedSetViewModel result = null;

                if (DuplicatedAssetSetsCollection != null && DuplicatedAssetSetsCollection.Count > 0 && DuplicatedAssetSetsPosition >= 0)
                {
                    result = DuplicatedAssetSetsCollection[DuplicatedAssetSetsPosition];
                }

                return result;
            }
        }

        public DuplicatedAssetViewModel CurrentDuplicatedAsset
        {
            get
            {
                DuplicatedAssetViewModel result = null;

                if (CurrentDuplicatedAssetSet != null && CurrentDuplicatedAssetSet.Count > 0 && DuplicatedAssetPosition >= 0)
                {
                    result = CurrentDuplicatedAssetSet[DuplicatedAssetPosition];
                }

                return result;
            }
        }

        public void Refresh()
        {
            var duplicates = Application.GetDuplicatedAssets();
            SetDuplicates(duplicates);
        }

        public void DeleteAsset(DuplicatedAssetViewModel assetViewModel)
        {
            Application.DeleteAssets(new Asset[] { assetViewModel.Asset }, deleteFiles: true);
            assetViewModel.Visible = Visibility.Collapsed;
            NavigateToNextVisibleSet(DuplicatedAssetSetsPosition);
            // TODO: THE COUNTER DISPLAYED AT THE TOP OF THE SCREEN SHOULD BE UPDATED AS WELL BASED ON THE TOTAL OF DUPLICATED SETS WITH AT LEAST 2 VISIBLE ASSETS.
        }

        private void NavigateToNextVisibleSet(int currentIndex)
        {
            var nextVisibleSet = DuplicatedAssetSetsCollection
                .Where(s => s.Visible == Visibility.Visible
                    && DuplicatedAssetSetsCollection.IndexOf(s) > currentIndex)
                .FirstOrDefault();

            if (nextVisibleSet != null)
            {
                int nextIndex = DuplicatedAssetSetsCollection.IndexOf(nextVisibleSet);
                DuplicatedAssetSetsPosition = nextIndex;
            }
            else
            {
                NavigateToPreviousVisibleSet(currentIndex);
            }
        }

        private void NavigateToPreviousVisibleSet(int currentIndex)
        {
            var previousVisibleSet = DuplicatedAssetSetsCollection
                .Where(s => s.Visible == Visibility.Visible
                    && DuplicatedAssetSetsCollection.IndexOf(s) < currentIndex)
                .LastOrDefault();

            if (previousVisibleSet != null)
            {
                int nextIndex = DuplicatedAssetSetsCollection.IndexOf(previousVisibleSet);
                DuplicatedAssetSetsPosition = nextIndex;
            }
            else
            {
                // TODO: SHOULD DISPLAY A MESSAGE.
            }
        }
    }

    public class DuplicatedAssetViewModel : BaseViewModel
    {
        private Asset asset;
        private Visibility visible;

        public DuplicatedAssetViewModel(IApplication assetApp) : base(assetApp)
        {
        }

        public Asset Asset
        {
            get { return asset; }
            set
            {
                asset = value;
                NotifyPropertyChanged(nameof(Asset));
            }
        }

        public Visibility Visible
        {
            get { return visible; }
            set
            {
                visible = value;
                NotifyPropertyChanged(nameof(Visible));

                if (ParentViewModel != null)
                    ParentViewModel.NotifyAssetChanged(this);
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
                PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
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
            get { return GetVisibleDuplicates(); }
        }

        public Visibility Visible
        {
            get
            {
                return GetVisibleDuplicates() > 1 ? Visibility.Visible : Visibility.Collapsed;
            }
        }

        public void NotifyAssetChanged(DuplicatedAssetViewModel asset)
        {
            NotifyPropertyChanged(nameof(DuplicatesCount), nameof(Visible));
        }
    }
}
