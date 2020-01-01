using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Text;

namespace JPPhotoManager.UI.ViewModels
{
    public static class ViewModelExtensions
    {
        public static void MoveUp<T>(this ObservableCollection<T> collection, T item)
        {
            int currentIndex = collection.IndexOf(item);

            if (currentIndex > 0)
            {
                int newIndex = currentIndex - 1;
                collection.Remove(item);
                collection.Insert(newIndex, item);
            }
        }

        public static void MoveDown<T>(this ObservableCollection<T> collection, T item)
        {
            int currentIndex = collection.IndexOf(item);

            if (currentIndex < (collection.Count - 1))
            {
                int newIndex = currentIndex + 1;
                collection.Remove(item);
                collection.Insert(newIndex, item);
            }
        }
    }
}
