import { AssetSummary } from './asset-summary.model';
import { FolderStat } from './folder-stat.model';

export interface HomeStats {
  folderCount: number;
  assetCount: number;
  lastCatalogCompletedAt: string | null;
  totalFileSize: number;
  duplicateCount: number;
  topFolders: FolderStat[];
  recentAssets: AssetSummary[];
}
