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
    // Clear the sync queue without closing any existing connections (avoids deleteDatabase deadlock)
    cy.wrap(
      openDB<SyncQueueSchema>('photomanager-db', 1, {
        upgrade(db) {
          if (!db.objectStoreNames.contains('photomanager-sync-queue')) {
            db.createObjectStore('photomanager-sync-queue', { autoIncrement: true });
          }
        },
      }).then(db => db.clear('photomanager-sync-queue').then(() => { db.close(); }))
    ).then(() => {
      service = new BackgroundSyncService();
    });
  });

  it('queueMutation_called_storesEntryInIndexedDB', () => {
    cy.wrap(service.queueMutation('/api/assets/1/rating', 'PATCH', { rating: 5 })).then(() => {
      cy.wrap(service.getPendingCount()).should('equal', 1);
    });
  });

  it('getPendingCount_afterQueuingTwoMutations_returnsTwo', () => {
    cy.wrap(
      service.queueMutation('/api/assets/1/rating', 'PATCH', { rating: 4 }).then(() =>
        service.queueMutation('/api/assets/2/rating', 'PATCH', { rating: 3 })
      )
    ).then(() => {
      cy.wrap(service.getPendingCount()).should('equal', 2);
    });
  });
});
