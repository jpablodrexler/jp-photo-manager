export interface FolderStorageEntry {
  folderPath: string;
  bytes: number;
}

export interface FormatEntry {
  extension: string;
  count: number;
}

export interface MonthlyCountEntry {
  month: string;
  count: number;
}

export interface RatingEntry {
  rating: number;
  count: number;
}

export interface AnalyticsData {
  folderStorage: FolderStorageEntry[];
  formatDistribution: FormatEntry[];
  photosPerMonth: MonthlyCountEntry[];
  ratingDistribution: RatingEntry[];
}

export interface ChartEntry {
  name: string;
  value: number;
}

export interface ChartSeries {
  name: string;
  series: ChartEntry[];
}
