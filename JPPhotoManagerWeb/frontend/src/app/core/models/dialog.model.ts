import { AlbumSummary } from './album.model';

export interface AddToAlbumDialogData {
  albums: AlbumSummary[];
}

export interface AddToAlbumDialogResult {
  albumId: number | null;
  newAlbumName: string | null;
}

export interface BatchRenameDialogData {
  assetIds: number[];
  assetCount: number;
}

export interface BulkTagDialogData {
  assetIds: number[];
}

export interface FolderPickerDialogData {
  mode: 'move' | 'copy';
  assetCount: number;
  sourceFolder: string;
}

export interface FolderPickerDialogResult {
  destinationFolder: string;
}

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
}
