namespace JPPhotoManager.Domain
{
    public interface IMoveAssetsService
    {
        bool MoveAsset(Asset asset, Folder destinationFolder, bool preserveOriginalFile);
        void DeleteAsset(Asset asset, bool deleteFile);
    }
}
