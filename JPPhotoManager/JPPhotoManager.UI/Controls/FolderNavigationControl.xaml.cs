using JPPhotoManager.Domain;
using JPPhotoManager.Infrastructure;
using JPPhotoManager.UI.ViewModels;
using log4net;
using System;
using System.Linq;
using System.Reflection;
using System.Windows;
using System.Windows.Controls;

namespace JPPhotoManager.UI.Controls
{
    /// <summary>
    /// Interaction logic for FolderNavigationControl.xaml
    /// </summary>
    [ExcludeFromCodeCoverage]
    public partial class FolderNavigationControl : UserControl
    {
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        private object placeholderNode = null;
        public event EventHandler FolderSelected;
        public string SelectedPath { get; set; }
        private bool isInitializing = true;

        public FolderNavigationControl()
        {
            InitializeComponent();
        }

        public ApplicationViewModel ViewModel
        {
            get { return (ApplicationViewModel)DataContext; }
        }

        private void UserControl_Loaded(object sender, RoutedEventArgs e)
        {
            Initialize();
            ViewModel.FolderAdded += ViewModel_FolderAdded;
            ViewModel.FolderRemoved += ViewModel_FolderRemoved;
        }

        private void ViewModel_FolderAdded(object sender, FolderAddedEventArgs e)
        {
            ViewModel.IsRefreshingFolders = true;
            Initialize();
            ViewModel.IsRefreshingFolders = false;
        }

        private void ViewModel_FolderRemoved(object sender, FolderRemovedEventArgs e)
        {
            ViewModel.IsRefreshingFolders = true;
            Initialize();
            ViewModel.IsRefreshingFolders = false;
        }

        // TODO: When a new folder is catalogued, this control should be notified so it can display it.
        public void Initialize()
        {
            try
            {
                foldersTreeView.Items.Clear();
                FolderViewModel[] rootFolders = ViewModel.GetRootCatalogFolders();

                foreach (FolderViewModel folder in rootFolders)
                {
                    TreeViewItem item = new()
                    {
                        Header = folder,
                        Tag = folder
                    };

                    item.Items.Add(placeholderNode);
                    item.Expanded += new RoutedEventHandler(Item_Expanded);
                    foldersTreeView.Items.Add(item);
                }

                GoToFolder(SelectedPath);
                isInitializing = false;
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void AddSubItems(TreeViewItem item, bool includeHidden)
        {
            try
            {
                item.Items.Clear();

                FolderViewModel[] folders = ViewModel.GetSubFolders(((FolderViewModel)item.Tag).Folder, includeHidden);
                folders = folders.OrderBy(f => f.Folder.Name).ToArray();

                foreach (FolderViewModel folder in folders)
                {
                    TreeViewItem subitem = new()
                    {
                        Header = folder,
                        Tag = folder
                    };

                    subitem.Collapsed += new RoutedEventHandler(Item_Collapsed);
                    subitem.Expanded += new RoutedEventHandler(Item_Expanded);
                    item.Items.Add(subitem);
                    AddSubItems(subitem, includeHidden);
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void Item_Collapsed(object sender, RoutedEventArgs e)
        {
            if (isInitializing)
                return;

            TreeViewItem item = (TreeViewItem)sender;
            FolderViewModel folder = (FolderViewModel)item.Tag;
            folder.IsExpanded = false; // TODO: THE SETTER DOESNT SEEM TO WORK.
        }

        private void Item_Expanded(object sender, RoutedEventArgs e)
        {
            if (isInitializing)
                return;

            TreeViewItem item = (TreeViewItem)sender;
            FolderViewModel folder = (FolderViewModel)item.Tag;
            folder.IsExpanded = true; // TODO: THE SETTER DOESNT SEEM TO WORK.

            if (LacksSubItems(item))
            {
                // TODO: SHOULD ASK THE USER IF HE WANTS TO SEE HIDDEN FOLDERS.
                AddSubItems(item, false);
            }
        }

        private bool LacksSubItems(TreeViewItem item)
        {
            return item.Items.Count == 1 && item.Items[0] == placeholderNode;
        }

        private void FoldersTreeView_SelectedItemChanged(object sender, RoutedPropertyChangedEventArgs<object> e)
        {
            try
            {
                TreeView tree = (TreeView)sender;
                TreeViewItem selectedTreeViewItem = ((TreeViewItem)tree.SelectedItem);

                if (selectedTreeViewItem == null)
                    return;

                if (selectedTreeViewItem.Tag is FolderViewModel folder)
                {
                    SelectedPath = folder.Folder.Path;
                    FolderSelected?.Invoke(this, new EventArgs());
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        public void GoToFolder(string folderFullPath)
        {
            foreach (var item in foldersTreeView.Items)
            {
                TreeViewItem treeViewItem = (TreeViewItem)item;
                // TODO: SHOULD ASK THE USER IF HE WANTS TO SEE HIDDEN FOLDERS.
                GoToFolder(treeViewItem, folderFullPath, false);
            }
        }

        private void GoToFolder(TreeViewItem item, string folderFullPath, bool includeHidden)
        {
            if (item.Tag is Folder folder)
            {
                if (folderFullPath.StartsWith(folder.Path, StringComparison.InvariantCultureIgnoreCase))
                {
                    if (LacksSubItems(item))
                    {
                        AddSubItems(item, includeHidden);
                    }

                    item.IsExpanded = true;

                    if (folder.Path == folderFullPath)
                    {
                        item.IsSelected = true;
                        item.BringIntoView();
                    }
                    else
                    {
                        foreach (var subItem in item.Items)
                        {
                            TreeViewItem treeViewItem = (TreeViewItem)subItem;
                            GoToFolder(treeViewItem, folderFullPath, includeHidden);
                        }
                    }
                }
            }
        }
    }
}
