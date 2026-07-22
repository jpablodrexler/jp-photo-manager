import { ErrorHandler, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

const DEFAULT_MESSAGE = 'An unexpected error occurred';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  constructor(private snackBar: MatSnackBar) {}

  handleError(error: unknown): void {
    const message = this.extractMessage(error);
    this.snackBar.open(message, 'Dismiss', { duration: 5000 });
  }

  private extractMessage(error: unknown): string {
    if (error instanceof Error && error.message) {
      return error.message;
    }
    return DEFAULT_MESSAGE;
  }
}
