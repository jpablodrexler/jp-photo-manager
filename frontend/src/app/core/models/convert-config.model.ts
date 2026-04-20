export interface ConvertAssetsDirectoriesDefinition {
  id?: number;
  sourceDirectory: string;
  destinationDirectory: string;
  includeSubFolders: boolean;
  deleteAssetsNotInSource: boolean;
  order: number;
}

export interface ConvertAssetsResult {
  sourceDirectory: string;
  destinationDirectory: string;
  convertedCount: number;
  failedCount: number;
  message?: string;
  success: boolean;
}
