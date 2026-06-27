export interface CatalogNotification {
  reason: string;
  percentCompleted: number;
  folderPath?: string;
  asset?: {
    fileName: string;
  };
}
