export interface SyncAssetsDirectoriesDefinition {
  id?: number;
  sourceDirectory: string;
  destinationDirectory: string;
  includeSubFolders: boolean;
  deleteAssetsNotInSource: boolean;
  order: number;
}

export interface SyncAssetsResult {
  sourceDirectory: string;
  destinationDirectory: string;
  syncedCount: number;
  deletedCount: number;
  message?: string;
  success: boolean;
}
