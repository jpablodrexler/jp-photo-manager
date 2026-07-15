import { Injectable } from '@angular/core';
import { openDB, IDBPDatabase } from 'idb';
import { PhotoManagerDBSchema, ServiceWorkerRegistrationWithSync } from '../models/background-sync.model';

@Injectable({ providedIn: 'root' })
export class BackgroundSyncService {
  private readonly dbPromise: Promise<IDBPDatabase<PhotoManagerDBSchema>>;

  constructor() {
    this.dbPromise = openDB<PhotoManagerDBSchema>('photomanager-db', 1, {
      upgrade(db) {
        if (!db.objectStoreNames.contains('photomanager-sync-queue')) {
          db.createObjectStore('photomanager-sync-queue', { autoIncrement: true });
        }
      },
    });
  }

  async queueMutation(url: string, method: string, body: unknown): Promise<void> {
    const db = await this.dbPromise;
    await db.add('photomanager-sync-queue', { url, method, body, timestamp: Date.now() });
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.ready
        .then(reg => (reg as ServiceWorkerRegistrationWithSync).sync?.register('asset-mutations'))
        .catch(() => { /* Background Sync API not supported */ });
    }
  }

  async getPendingCount(): Promise<number> {
    const db = await this.dbPromise;
    return db.count('photomanager-sync-queue');
  }

  async replayQueue(): Promise<void> {
    const db = await this.dbPromise;
    const entries = await db.getAll('photomanager-sync-queue');
    const keys = await db.getAllKeys('photomanager-sync-queue');

    for (let i = 0; i < entries.length; i++) {
      const entry = entries[i];
      const key = keys[i];
      try {
        const response = await fetch(entry.url, {
          method: entry.method,
          headers: { 'Content-Type': 'application/json' },
          body: entry.body != null ? JSON.stringify(entry.body) : undefined,
          credentials: 'same-origin',
        });
        if (response.ok) {
          await db.delete('photomanager-sync-queue', key);
        }
      } catch {
        // Network unavailable; leave entry in queue for next retry
      }
    }
  }
}
