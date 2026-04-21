export interface Folder {
  folderId?: number;
  path: string;
  name: string;
  parentPath?: string;
  children?: Folder[];
  expanded?: boolean;
}
