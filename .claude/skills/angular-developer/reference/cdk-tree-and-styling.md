# Angular Developer — CDK Tree (Folder Navigation) & SCSS/Styling

_Part of the `angular-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 13. CDK Tree (Folder Navigation)

Use `FlatTreeControl` with a custom data source for hierarchical folder trees:

```typescript
treeControl = new FlatTreeControl<FlatNode>(
  node => node.level,
  node => node.expandable,
);

hasChild = (_: number, node: FlatNode) => node.expandable;

private buildTree(folders: Folder[]): FlatNode[] {
  return folders.map(folder => ({
    expandable: folder.hasChildren,
    name: folder.name,
    level: folder.depth,
    path: folder.path,
  }));
}
```

---

## 14. SCSS & Styling

Define utility classes in `styles.scss` and use them across templates:

```scss
// styles.scss — global utilities
.full-height {
  height: 100%;
}
.flex-row {
  display: flex;
  flex-direction: row;
}
.flex-col {
  display: flex;
  flex-direction: column;
}
.gap-8 {
  gap: 8px;
}
.gap-16 {
  gap: 16px;
}
.p-16 {
  padding: 16px;
}
.flex-1 {
  flex: 1;
}
```

Component SCSS uses BEM-style class names:

```scss
// gallery.component.scss
.gallery-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.thumbnail-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 16px;
}

.thumbnail-grid .selected {
  outline: 2px solid mat.get-color($primary, 500);
}
```

---

