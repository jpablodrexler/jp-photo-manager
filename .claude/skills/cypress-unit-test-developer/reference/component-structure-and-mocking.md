# Cypress Unit Test Developer — Component Test Structure & Mocking Services in Components

_Part of the `cypress-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 4. Component Test Structure

### Minimal template

```typescript
import { mount } from "cypress/angular";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import { ThumbnailComponent } from "./thumbnail.component";
import { Asset } from "../../core/models/asset.model";

describe("ThumbnailComponent", () => {
  const mockAsset: Asset = {
    assetId: 1,
    fileName: "photo.jpg",
    fileSize: 204800,
    thumbnailUrl: "/api/assets/1/thumbnail",
    folderPath: "/photos",
    imageRotation: "ROTATE_0",
    fileCreationDateTime: "2024-01-01T00:00:00",
    fileModificationDateTime: "2024-01-01T00:00:00",
    thumbnailCreationDateTime: "2024-01-01T00:00:00",
  };

  it("should render the asset file name", () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: mockAsset },
      providers: [provideNoopAnimations()],
    });

    cy.contains(".thumbnail-name", "photo.jpg");
  });

  it("should apply selected class when selected input is true", () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: { asset: mockAsset, selected: true },
      providers: [provideNoopAnimations()],
    });

    cy.get("mat-card").should("have.class", "selected");
  });

  it("should hide broken image on error", () => {
    cy.mount(ThumbnailComponent, {
      componentProperties: {
        asset: { ...mockAsset, thumbnailUrl: "/bad-url" },
      },
      providers: [provideNoopAnimations()],
    });

    cy.get("img").then(($img) => {
      $img[0].dispatchEvent(new Event("error"));
    });
    cy.get("img").should("have.css", "display", "none");
  });
});
```

**Rules:**

- Always pass `provideNoopAnimations()` — Angular Material animations break Cypress otherwise.
- Use `componentProperties` (not `inputs`) — it maps directly to `@Input()` properties.
- Create typed mock objects that match the model interface exactly.

---

## 5. Mocking Services in Components

Provide a stub object with `cy.stub()` methods via the `providers` array:

```typescript
import { mount } from "cypress/angular";
import { of } from "rxjs";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { GalleryComponent } from "./gallery.component";
import { AssetService } from "../../core/services/asset.service";
import { PaginatedData } from "../../core/models/paginated-data.model";
import { Asset } from "../../core/models/asset.model";

describe("GalleryComponent", () => {
  const mockPage: PaginatedData<Asset> = {
    items: [
      {
        assetId: 1,
        fileName: "sunset.jpg",
        fileSize: 1024000,
        thumbnailUrl: "/api/assets/1/thumbnail",
        folderPath: "/photos",
        imageRotation: "ROTATE_0",
        fileCreationDateTime: "2024-06-01T10:00:00",
        fileModificationDateTime: "2024-06-01T10:00:00",
        thumbnailCreationDateTime: "2024-06-01T10:00:00",
      },
    ],
    pageIndex: 0,
    totalPages: 1,
    totalItems: 1,
  };

  function mountGallery(assetServiceOverrides: Partial<AssetService> = {}) {
    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy.stub().returns(of(mockPage)),
      catalogAssets: cy.stub().returns(new MockEventSource()),
      deleteAssets: cy.stub().returns(of(undefined)),
      ...assetServiceOverrides,
    };

    return cy.mount(GalleryComponent, {
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AssetService, useValue: assetServiceStub },
      ],
    });
  }

  it("should create the component", () => {
    mountGallery();
    cy.get("app-gallery").should("exist");
  });

  it("should display thumbnails after a folder is selected", () => {
    mountGallery();

    cy.get("app-folder-nav").then(($nav) => {
      $nav[0].dispatchEvent(
        new CustomEvent("folderSelected", { detail: "/photos", bubbles: true }),
      );
    });

    cy.get("app-thumbnail").should("have.length", 1);
  });
});
```

### MockEventSource helper

Declare a reusable mock for `EventSource` to avoid actual SSE connections:

```typescript
// cypress/support/mock-event-source.ts
export class MockEventSource implements Partial<EventSource> {
  private listeners: Record<string, EventListenerOrEventListenerObject[]> = {};

  readonly CONNECTING = 0 as const;
  readonly OPEN = 1 as const;
  readonly CLOSED = 2 as const;
  readyState: number = 1;
  url = "";
  withCredentials = false;
  onopen: ((this: EventSource, ev: Event) => unknown) | null = null;
  onmessage: ((this: EventSource, ev: MessageEvent) => unknown) | null = null;
  onerror: ((this: EventSource, ev: Event) => unknown) | null = null;

  addEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject,
  ): void {
    if (!this.listeners[type]) this.listeners[type] = [];
    this.listeners[type].push(listener);
  }

  removeEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject,
  ): void {
    this.listeners[type] = (this.listeners[type] ?? []).filter(
      (l) => l !== listener,
    );
  }

  dispatchEvent(event: Event): boolean {
    (this.listeners[event.type] ?? []).forEach((l) => {
      if (typeof l === "function") l(event);
      else l.handleEvent(event);
    });
    return true;
  }

  emit(type: string, data: unknown): void {
    const event = new MessageEvent(type, { data: JSON.stringify(data) });
    this.dispatchEvent(event);
  }

  close(): void {
    this.readyState = 2;
  }
}
```

Import it in tests and in `cypress/support/component.ts`:

```typescript
// cypress/support/component.ts
export { MockEventSource } from "./mock-event-source";
```

---

