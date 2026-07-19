export interface Folder {
  folderId?: number;
  path: string;
  name: string;
  parentPath?: string;
  children?: Folder[];
  expanded?: boolean;
}

export interface FlatFolder {
  expandable: boolean;
  name: string;
  path: string;
  level: number;
}
