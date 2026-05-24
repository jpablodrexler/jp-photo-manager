import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-about-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './about-dialog.component.html',
  styleUrl: './about-dialog.component.scss',
})
export class AboutDialogComponent {
  readonly version = '1.0.0';
  readonly githubUrl = 'https://github.com/jpablodrexler/jp-photo-manager';
}
