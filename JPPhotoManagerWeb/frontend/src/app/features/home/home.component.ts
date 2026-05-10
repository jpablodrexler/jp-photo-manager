import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { HomeService } from '../../core/services/home.service';
import { HomeStats } from '../../core/models/home-stats.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, DatePipe, MatCardModule, MatIconModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  stats: HomeStats | null = null;

  constructor(private homeService: HomeService) {}

  ngOnInit(): void {
    this.homeService.getStats().subscribe({
      next: stats => (this.stats = stats)
    });
  }
}
