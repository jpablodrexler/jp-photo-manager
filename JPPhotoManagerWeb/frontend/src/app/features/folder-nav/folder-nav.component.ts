import { Component, OnInit, Output, EventEmitter } from "@angular/core";
import { CommonModule } from "@angular/common";
import {
  MatTreeModule,
  MatTreeFlatDataSource,
  MatTreeFlattener,
} from "@angular/material/tree";
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { FlatTreeControl } from "@angular/cdk/tree";
import { FolderService } from "../../core/services/folder.service";
import { Folder } from "../../core/models/folder.model";

interface FlatFolder {
  expandable: boolean;
  name: string;
  path: string;
  level: number;
}

@Component({
  selector: "app-folder-nav",
  standalone: true,
  imports: [
    CommonModule,
    MatTreeModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: "./folder-nav.component.html",
  styleUrl: "./folder-nav.component.scss",
})
export class FolderNavComponent implements OnInit {
  @Output() folderSelected = new EventEmitter<string>();

  selectedPath: string = "";
  loading = false;

  private transformer = (node: Folder, level: number): FlatFolder => ({
    expandable: !!node.children && node.children.length > 0,
    name: node.name,
    path: node.path,
    level,
  });

  treeControl = new FlatTreeControl<FlatFolder>(
    (node) => node.level,
    (node) => node.expandable,
  );

  treeFlattener = new MatTreeFlattener<Folder, FlatFolder>(
    this.transformer,
    (node) => node.level,
    (node) => node.expandable,
    (node) => node.children ?? [],
  );

  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

  constructor(private folderService: FolderService) {}

  ngOnInit(): void {
    this.loading = true;
    this.folderService.getFolders().subscribe({
      next: (folders) => {
        this.dataSource.data = this.buildTree(folders);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  hasChild = (_: number, node: FlatFolder) => node.expandable;

  selectFolder(node: FlatFolder): void {
    this.selectedPath = node.path;
    this.folderSelected.emit(node.path);
  }

  private buildTree(folders: Folder[]): Folder[] {
    const byParent = new Map<string, Folder[]>();
    const existingPaths = new Set(folders.map((folder) => folder.path));
    const roots: Folder[] = [];

    for (const f of folders) {
      if (f.parentPath && existingPaths.has(f.parentPath)) {
        const siblings = byParent.get(f.parentPath) ?? [];
        siblings.push(f);
        byParent.set(f.parentPath, siblings);
      } else {
        roots.push(f);
      }
    }

    const attach = (nodes: Folder[]) => {
      for (const node of nodes) {
        node.children = byParent.get(node.path) ?? [];
        attach(node.children);
      }
    };
    attach(roots);
    return roots;
  }
}
