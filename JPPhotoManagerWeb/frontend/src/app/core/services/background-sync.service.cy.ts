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

  describe('replayQueue', () => {
    it('should do nothing when the queue is empty', () => {
      const fetchStub = cy.stub(window, 'fetch');

      cy.wrap(service.replayQueue()).then(() => {
        cy.wrap(service.getPendingCount()).should('equal', 0);
        expect(fetchStub).to.not.have.been.called;
      });
    });

    it('should remove a successfully replayed mutation from the queue', () => {
      const fetchStub = cy.stub(window, 'fetch');
      fetchStub.resolves(new Response(null, { status: 200 }));

      cy.wrap(service.queueMutation('/api/assets/1/rating', 'PATCH', { rating: 5 }))
        .then(() => service.replayQueue())
        .then(() => {
          cy.wrap(service.getPendingCount()).should('equal', 0);
          expect(fetchStub).to.have.been.calledWith(
            '/api/assets/1/rating',
            Cypress.sinon.match({
              method: 'PATCH',
              body: JSON.stringify({ rating: 5 }),
              credentials: 'same-origin',
            })
          );
        });
    });

    it('should send an undefined body when the queued mutation has no body', () => {
      const fetchStub = cy.stub(window, 'fetch');
      fetchStub.resolves(new Response(null, { status: 200 }));

      cy.wrap(service.queueMutation('/api/assets/catalog', 'POST', undefined))
        .then(() => service.replayQueue())
        .then(() => {
          expect(fetchStub).to.have.been.calledWith(
            '/api/assets/catalog',
            Cypress.sinon.match({ method: 'POST', body: undefined })
          );
        });
    });

    it('should keep the mutation in the queue when the response is not ok', () => {
      const fetchStub = cy.stub(window, 'fetch');
      fetchStub.resolves(new Response(null, { status: 500 }));

      cy.wrap(service.queueMutation('/api/assets/1/rating', 'PATCH', { rating: 5 }))
        .then(() => service.replayQueue())
        .then(() => {
          cy.wrap(service.getPendingCount()).should('equal', 1);
        });
    });

    it('should keep the mutation in the queue when fetch rejects with a network error', () => {
      const fetchStub = cy.stub(window, 'fetch');
      fetchStub.rejects(new Error('network unavailable'));

      cy.wrap(service.queueMutation('/api/assets/1/rating', 'PATCH', { rating: 5 }))
        .then(() => service.replayQueue())
        .then(() => {
          cy.wrap(service.getPendingCount()).should('equal', 1);
        });
    });

    it('should only remove the successfully replayed entry out of several queued mutations', () => {
      let call = 0;
      cy.stub(window, 'fetch').callsFake(() => {
        call += 1;
        return call === 1
          ? Promise.resolve(new Response(null, { status: 200 }))
          : Promise.resolve(new Response(null, { status: 500 }));
      });

      cy.wrap(
        service.queueMutation('/api/assets/1/rating', 'PATCH', { rating: 5 }).then(() =>
          service.queueMutation('/api/assets/2/rating', 'PATCH', { rating: 4 })
        )
      )
        .then(() => service.replayQueue())
        .then(() => {
          cy.wrap(service.getPendingCount()).should('equal', 1);
        });
    });
  });
});
