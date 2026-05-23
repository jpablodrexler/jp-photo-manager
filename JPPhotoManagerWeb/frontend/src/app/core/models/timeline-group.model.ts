import { Asset } from './asset.model';

export interface TimelineGroup {
  localDate: string;
  label: string;
  assets: Asset[];
}
