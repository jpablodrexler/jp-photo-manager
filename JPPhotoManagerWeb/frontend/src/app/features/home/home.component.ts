import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatListModule } from '@angular/material/list';
import { HomeService } from '../../core/services/home.service';
import { HomeStats } from '../../core/models/home-stats.model';
import { ThumbnailComponent } from '../../shared/components/thumbnail/thumbnail.component';
import { FileSizePipe } from '../../shared/pipes/file-size.pipe';
import { AssetSummary } from '../../core/models/asset-summary.model';
import { Asset } from '../../core/models/asset.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
    MatListModule,
    ThumbnailComponent,
    FileSizePipe,
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  stats: HomeStats | null = null;

  constructor(
    private homeService: HomeService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.homeService.getStats().subscribe({
      next: stats => (this.stats = stats)
    });
  }

  get maxFolderCount(): number {
    if (!this.stats?.topFolders?.length) return 1;
    return this.stats.topFolders[0].assetCount;
  }

  navigateToGalleryFolder(asset: AssetSummary): void {
    this.router.navigate(['/gallery'], { queryParams: { folder: asset.folderPath } });
  }

  assetSummaryToAsset(summary: AssetSummary): Asset {
    return {
      assetId: summary.assetId,
      folderId: 0,
      folderPath: summary.folderPath,
      fileName: summary.fileName,
      fileSize: 0,
      thumbnailCreationDateTime: '',
      hash: '',
      thumbnailUrl: summary.thumbnailUrl,
      imageUrl: '',
      rating: 0,
      tags: [],
    };
  }
}
