export interface SearchPreset {
  presetId: number;
  name: string;
  createdAt: string;
  search?: string;
  dateFrom?: string;
  dateTo?: string;
  minRating?: number;
}

export interface CreatePresetRequest {
  name: string;
  search?: string;
  dateFrom?: string;
  dateTo?: string;
  minRating?: number;
}
