import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/services/auth.service';
import { SessionInfo } from '../../../core/models/auth.model';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-sessions',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatButtonModule, MatIconModule, MatCardModule],
  templateUrl: './sessions.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './sessions.component.scss'
})
export class SessionsComponent implements OnInit {
  sessions: SessionInfo[] = [];
  displayedColumns = ['deviceHint', 'lastUsedAt', 'actions'];
  errorMessage: string | null = null;

  constructor(
    private authService: AuthService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadSessions();
  }

  loadSessions(): void {
    this.authService.getSessions().subscribe({
      next: sessions => (this.sessions = sessions),
      error: () => (this.errorMessage = 'Failed to load sessions.')
    });
  }

  revokeSession(session: SessionInfo): void {
    this.authService.revokeSession(session.id).subscribe({
      next: () => {
        this.sessions = this.sessions.filter(s => s.id !== session.id);
        this.errorMessage = null;
        this.snackBar.open('Session revoked', undefined, { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to revoke session', 'Dismiss', { duration: 3000 })
    });
  }

  signOutEverywhereElse(): void {
    const dialogRef = this.dialog.open<ConfirmDialogComponent, unknown, boolean>(
      ConfirmDialogComponent,
      {
        data: {
          title: 'Sign out everywhere else',
          message: 'This will revoke all of your other active sessions. Continue?',
          confirmLabel: 'Sign out'
        }
      }
    );
    dialogRef.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.authService.revokeAllOtherSessions().subscribe({
        next: () => {
          this.sessions = this.sessions.filter(s => s.current);
          this.errorMessage = null;
          this.snackBar.open('Signed out of all other sessions', undefined, { duration: 2000 });
        },
        error: () => this.snackBar.open('Failed to sign out of other sessions', 'Dismiss', { duration: 3000 })
      });
    });
  }
}
