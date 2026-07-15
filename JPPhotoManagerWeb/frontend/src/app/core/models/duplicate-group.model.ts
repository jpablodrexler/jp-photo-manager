import { Asset } from './asset.model';

export interface DuplicateGroup {
  assets: Asset[];
  keepIndex: number;
}
