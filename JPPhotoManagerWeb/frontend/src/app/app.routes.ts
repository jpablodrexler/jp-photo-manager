import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'home',
    loadComponent: () =>
      import('./features/home/home.component').then(m => m.HomeComponent),
    canActivate: [authGuard]
  },
  {
    path: 'gallery',
    loadComponent: () =>
      import('./features/gallery/gallery.component').then(m => m.GalleryComponent),
    canActivate: [authGuard]
  },
  {
    path: 'sync',
    loadComponent: () =>
      import('./features/sync/sync.component').then(m => m.SyncComponent),
    canActivate: [authGuard]
  },
  {
    path: 'convert',
    loadComponent: () =>
      import('./features/convert/convert.component').then(m => m.ConvertComponent),
    canActivate: [authGuard]
  },
  {
    path: 'duplicates',
    loadComponent: () =>
      import('./features/duplicates/duplicates.component').then(m => m.DuplicatesComponent),
    canActivate: [authGuard]
  },
  {
    path: 'admin/users',
    loadComponent: () =>
      import('./features/admin/users/user-admin.component').then(m => m.UserAdminComponent),
    canActivate: [authGuard]
  },
  {
    path: 'albums',
    loadComponent: () =>
      import('./features/albums/albums.component').then(m => m.AlbumsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'albums/:id',
    loadComponent: () =>
      import('./features/albums/album-detail/album-detail.component').then(m => m.AlbumDetailComponent),
    canActivate: [authGuard]
  },
  {
    path: 'recycle-bin',
    loadComponent: () =>
      import('./features/recycle-bin/recycle-bin.component').then(m => m.RecycleBinComponent),
    canActivate: [authGuard]
  }
];
