import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'gallery',
    pathMatch: 'full'
  },
  {
    path: 'gallery',
    loadComponent: () =>
      import('./features/gallery/gallery.component').then(m => m.GalleryComponent)
  },
  {
    path: 'sync',
    loadComponent: () =>
      import('./features/sync/sync.component').then(m => m.SyncComponent)
  },
  {
    path: 'convert',
    loadComponent: () =>
      import('./features/convert/convert.component').then(m => m.ConvertComponent)
  },
  {
    path: 'duplicates',
    loadComponent: () =>
      import('./features/duplicates/duplicates.component').then(m => m.DuplicatesComponent)
  }
];
