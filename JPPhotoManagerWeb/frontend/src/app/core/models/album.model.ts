import { PaginatedData } from './paginated-data.model';
import { Asset } from './asset.model';

export interface AlbumFilterJson {
  search?: string;
  dateFrom?: string;
  dateTo?: string;
  minRating?: number;
}

export interface AlbumSummary {
  albumId: number;
  name: string;
  description: string | null;
  assetCount: number;
  createdAt: string;
  filterJson?: AlbumFilterJson | null;
}

export interface Album {
  albumId: number;
  name: string;
  description: string | null;
  createdAt: string;
  assets: PaginatedData<Asset>;
  filterJson?: AlbumFilterJson | null;
}

export interface CreateAlbumRequest {
  name: string;
  description?: string;
  filterJson?: AlbumFilterJson;
}

export interface UpdateAlbumRequest {
  name: string;
  description?: string;
  filterJson?: AlbumFilterJson | null;
}

export interface AlbumAssetIdsRequest {
  assetIds: number[];
}
