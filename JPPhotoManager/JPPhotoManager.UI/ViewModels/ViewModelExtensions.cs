using System.Collections.ObjectModel;

namespace JPPhotoManager.UI.ViewModels
{
    public static class ViewModelExtensions
    {
        public static int MoveUp<T>(this ObservableCollection<T> collection, T item)
        {
            int currentIndex = collection.IndexOf(item);

            if (currentIndex > 0)
            {
                int newIndex = currentIndex - 1;
                collection.Remove(item);
                collection.Insert(newIndex, item);
                currentIndex = newIndex;
            }

            return currentIndex;
        }

        public static int MoveDown<T>(this ObservableCollection<T> collection, T item)
        {
            int currentIndex = collection.IndexOf(item);

            if (currentIndex < (collection.Count - 1))
            {
                int newIndex = currentIndex + 1;
                collection.Remove(item);
                collection.Insert(newIndex, item);
                currentIndex = newIndex;
            }

            return currentIndex;
        }
    }
}
