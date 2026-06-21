import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-accent-color-picker',
  standalone: true,
  imports: [AsyncPipe, MatIconModule],
  templateUrl: './accent-color-picker.component.html',
  styleUrl: './accent-color-picker.component.scss',
})
export class AccentColorPickerComponent {
  private readonly themeService = inject(ThemeService);

  readonly accentColor$ = this.themeService.accentColor$;

  readonly PALETTE = [
    { label: 'Forest Green', value: '#2e7d32' },
    { label: 'Ocean Blue',   value: '#1565c0' },
    { label: 'Deep Purple',  value: '#6a1b9a' },
    { label: 'Teal',         value: '#00695c' },
    { label: 'Rust Orange',  value: '#bf360c' },
    { label: 'Slate Grey',   value: '#37474f' },
    { label: 'Crimson Red',  value: '#b71c1c' },
    { label: 'Indigo',       value: '#283593' },
  ];

  select(color: string): void {
    this.themeService.setAccentColor(color);
  }
}
