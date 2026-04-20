export type ImageRotation = 'ROTATE_0' | 'ROTATE_90' | 'ROTATE_180' | 'ROTATE_270';

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
}

export type SortCriteria =
  | 'FILE_NAME'
  | 'FILE_SIZE'
  | 'FILE_CREATION_DATE_TIME'
  | 'FILE_MODIFICATION_DATE_TIME'
  | 'THUMBNAIL_CREATION_DATE_TIME';
