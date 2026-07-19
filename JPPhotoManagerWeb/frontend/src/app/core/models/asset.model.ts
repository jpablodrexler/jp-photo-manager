export type ImageRotation = 'ROTATE_0' | 'ROTATE_90' | 'ROTATE_180' | 'ROTATE_270';
export type FileType = 'IMAGE' | 'AUDIO' | 'VIDEO' | 'PLAYLIST';

export interface Asset {
  assetId: number;
  folderId: number;
  folderPath: string;
  fileName: string;
  fileSize: number;
  pixelWidth?: number;
  pixelHeight?: number;
  thumbnailPixelWidth?: number;
  thumbnailPixelHeight?: number;
  imageRotation?: ImageRotation;
  thumbnailCreationDateTime: string;
  hash: string;
  fileCreationDateTime?: string;
  fileModificationDateTime?: string;
  thumbnailUrl: string;
  imageUrl: string;
  rating: number;
  tags: string[];
  fileType: FileType;
  isVideo: boolean;
}

export type ProcessingStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface UploadAssetResponse {
  assetId: number;
  status: ProcessingStatus;
}

export interface RenamePreview {
  assetId: number;
  oldName: string;
  newName: string;
}

export interface RenameAssetsRequest {
  assetIds: number[];
  pattern: string;
  applied: boolean;
}

export interface RenameAssetsResponse {
  previews: RenamePreview[];
  applied: boolean;
}

export interface CropAssetRequest {
  formatKey: string;
  x: number;
  y: number;
  width: number;
  height: number;
}

export type SortCriteria =
  | 'FILE_NAME'
  | 'FILE_SIZE'
  | 'FILE_CREATION_DATE_TIME'
  | 'FILE_MODIFICATION_DATE_TIME'
  | 'THUMBNAIL_CREATION_DATE_TIME'
  | 'RATING';

export interface AssetTimelineFilters {
  search?: string;
  dateFrom?: string;
  dateTo?: string;
  minRating?: number;
}
