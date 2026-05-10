import { PaginatedData } from './paginated-data.model';
import { Asset } from './asset.model';

export interface AlbumSummary {
  albumId: number;
  name: string;
  description: string | null;
  assetCount: number;
  createdAt: string;
}

export interface Album {
  albumId: number;
  name: string;
  description: string | null;
  createdAt: string;
  assets: PaginatedData<Asset>;
}

export interface CreateAlbumRequest {
  name: string;
  description?: string;
}

export interface UpdateAlbumRequest {
  name: string;
  description?: string;
}

export interface AlbumAssetIdsRequest {
  assetIds: number[];
}
