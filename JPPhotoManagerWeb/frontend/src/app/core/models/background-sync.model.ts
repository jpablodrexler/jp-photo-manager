import { DBSchema } from 'idb';

export interface SyncQueueEntry {
  url: string;
  method: string;
  body: unknown;
  timestamp: number;
}

export interface PhotoManagerDBSchema extends DBSchema {
  'photomanager-sync-queue': {
    key: number;
    value: SyncQueueEntry;
  };
}

export interface SyncManager {
  register(tag: string): Promise<void>;
}

export interface ServiceWorkerRegistrationWithSync extends ServiceWorkerRegistration {
  sync?: SyncManager;
}
