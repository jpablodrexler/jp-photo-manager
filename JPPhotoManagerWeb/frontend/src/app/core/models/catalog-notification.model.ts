export type CatalogState = 'idle' | 'running';

export interface CatalogNotificationAsset {
  fileName: string;
}

export interface CatalogNotification {
  reason: string;
  percentCompleted: number;
  folderPath?: string;
  asset?: CatalogNotificationAsset;
}
