import { openDB, DBSchema } from 'idb';
import { BackgroundSyncService } from './background-sync.service';

interface SyncQueueSchema extends DBSchema {
  'photomanager-sync-queue': {
    key: number;
    value: { url: string; method: string; body: unknown; timestamp: number };
  };
}

describe('BackgroundSyncService', () => {
  let service: BackgroundSyncService;

  beforeEach(() => {
    cy.wrap(
      openDB<SyncQueueSchema>('photomanager-db', 1, {
        upgrade(db) {
          if (!db.objectStoreNames.contains('photomanager-sync-queue')) {
            db.createObjectStore('photomanager-sync-queue', { autoIncrement: true });
          }
        },
      }).then(db => db.clear('photomanager-sync-queue').then(() => db.close()))
    ).then(() => {
      service = new BackgroundSyncService();
      // Return the promise so Cypress waits for the DB connection to open
      // before running the test body (avoids queueMutation hanging on dbPromise)
      return service.getPendingCount();
    });
  });

  it('should store the entry in IndexedDB when a mutation is queued', () => {
    cy.wrap(service.queueMutation('/api/assets/1/rating', 'PATCH', { rating: 5 })).then(() => {
      cy.wrap(service.getPendingCount()).should('equal', 1);
    });
  });

  it('should return two after queuing two mutations', () => {
    cy.wrap(
      service.queueMutation('/api/assets/1/rating', 'PATCH', { rating: 4 }).then(() =>
        service.queueMutation('/api/assets/2/rating', 'PATCH', { rating: 3 })
      )
    ).then(() => {
      cy.wrap(service.getPendingCount()).should('equal', 2);
    });
  });
});
