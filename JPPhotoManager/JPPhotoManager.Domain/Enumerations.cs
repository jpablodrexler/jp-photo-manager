namespace JPPhotoManager.Domain
{
    public enum AppModeEnum
    {
        Thumbnails,
        Viewer
    }

    public enum ReasonEnum
    {
        AssetCreated,
        AssetUpdated,
        AssetDeleted,
        FolderCreated,
        FolderDeleted
    }

    public enum WallpaperStyle
    {
        Center,
        Fill,
        Fit,
        Span,
        Stretch,
        Tile
    }

    public enum ProcessStepEnum
    {
        Configure,
        Import,
        ViewResults
    }

    public enum SortCriteriaEnum
    {
        Undefined,
        FileName,
        FileSize,
        FileCreationDateTime,
        FileModificationDateTime,
        ThumbnailCreationDateTime
    }
}
