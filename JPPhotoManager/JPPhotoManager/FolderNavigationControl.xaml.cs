using JPPhotoManager.Domain;
using JPPhotoManager.ViewModels;
using log4net;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace JPPhotoManager
{
    /// <summary>
    /// Interaction logic for FolderNavigationControl.xaml
    /// </summary>
    public partial class FolderNavigationControl : UserControl
    {
        private static readonly ILog log = LogManager.GetLogger(MethodBase.GetCurrentMethod().DeclaringType);
        private object dummyNode = null;
        public event EventHandler FolderSelected;
        public string SelectedPath { get; set; }
        private bool isInitializing = true;

        public FolderNavigationControl()
        {
            InitializeComponent();
        }

        public BaseViewModel<IAssetManagerApplication> ViewModel
        {
            get { return (BaseViewModel<IAssetManagerApplication>)this.DataContext; }
        }

        private void UserControl_Loaded(object sender, RoutedEventArgs e)
        {
            try
            {
                Folder[] drives = this.ViewModel.Application.GetDrives();

                foreach (Folder drive in drives)
                {
                    TreeViewItem item = new TreeViewItem
                    {
                        Header = drive.Name,
                        Tag = drive
                    };

                    item.Items.Add(dummyNode);
                    item.Expanded += new RoutedEventHandler(Item_Expanded);
                    foldersTreeView.Items.Add(item);
                }

                this.GoToFolder(this.SelectedPath);
                this.isInitializing = false;
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

                Folder[] folders = this.ViewModel.Application.GetFolders((Folder)item.Tag, includeHidden);

                foreach (Folder folder in folders)
                {
                    TreeViewItem subitem = new TreeViewItem
                    {
                        Header = folder.Name,
                        Tag = folder
                    };

                    subitem.Items.Add(dummyNode);
                    subitem.Expanded += new RoutedEventHandler(Item_Expanded);
                    item.Items.Add(subitem);
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        private void Item_Expanded(object sender, RoutedEventArgs e)
        {
            if (this.isInitializing)
                return;

            TreeViewItem item = (TreeViewItem)sender;

            if (LacksSubItems(item))
            {
                // TODO: SHOULD ASK THE USER IF HE WANTS TO SEE HIDDEN FOLDERS.
                AddSubItems(item, false);
            }
        }

        private bool LacksSubItems(TreeViewItem item)
        {
            return item.Items.Count == 1 && item.Items[0] == dummyNode;
        }

        private void FoldersTreeView_SelectedItemChanged(object sender, RoutedPropertyChangedEventArgs<object> e)
        {
            try
            {
                TreeView tree = (TreeView)sender;
                TreeViewItem selectedTreeViewItem = ((TreeViewItem)tree.SelectedItem);

                if (selectedTreeViewItem == null)
                    return;

                if (selectedTreeViewItem.Tag is Folder folder)
                {
                    this.SelectedPath = folder.Path;
                    this.FolderSelected?.Invoke(this, new EventArgs());
                }
            }
            catch (Exception ex)
            {
                log.Error(ex);
            }
        }

        public void GoToFolder(string folderFullPath)
        {
            foreach (var item in this.foldersTreeView.Items)
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
