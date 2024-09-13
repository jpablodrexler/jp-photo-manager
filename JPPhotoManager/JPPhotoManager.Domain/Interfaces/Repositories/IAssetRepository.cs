using System.Windows.Media.Imaging;
using JPPhotoManager.Domain.Entities;

namespace JPPhotoManager.Domain.Interfaces.Repositories
{
    public interface IAssetRepository
    {
        PaginatedData<Asset> GetAssets(Folder folder, int pageIndex);
        List<Asset> GetAssetsByFolderId(int folderId);
        void AddAsset(Asset asset, Folder folder, byte[] thumbnailData);
        List<Asset> GetCataloguedAssets();
        List<Asset> GetCataloguedAssets(Folder folder);
        bool IsAssetCatalogued(Folder folder, string fileName);
        void DeleteAsset(Folder folder, string deletedFileName);
        bool ContainsThumbnail(Folder folder, string fileName);
        BitmapImage LoadThumbnail(Folder folder, string fileName, int width, int height);
        void DeleteThumbnails(Folder folder);
        void DeleteThumbnail(string thumbnailBlobName);
        string[] GetThumbnailsList();
        void RemoveThumbnailCache(string thumbnailBlobName);
        void ShrinkDatabase();
    }
}
