﻿namespace JPPhotoManager.Domain
{
    public enum AppModeEnum
    {
        Thumbnails,
        Viewer
    }

    public enum ReasonEnum
    {
        Created,
        Updated,
        Deleted
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

    public enum ImportNewAssetsStepEnum
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
