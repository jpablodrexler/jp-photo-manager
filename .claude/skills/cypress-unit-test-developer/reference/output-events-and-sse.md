# Cypress Unit Test Developer — Testing Angular Output Events & EventSource/SSE in Components

_Part of the `cypress-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 8. Testing Angular Output Events

Use `cy.stub()` on a callback function and bind it as an output handler:

```typescript
import { mount } from "cypress/angular";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import { FolderNavComponent } from "./folder-nav.component";
import { FolderService } from "../../core/services/folder.service";
import { of } from "rxjs";
import { Folder } from "../../core/models/folder.model";

describe("FolderNavComponent", () => {
  const mockFolders: Folder[] = [
    { folderId: 1, path: "/photos", name: "photos", hasChildren: false },
  ];

  it("should emit folderSelected when a folder node is clicked", () => {
    const folderServiceStub: Partial<FolderService> = {
      getInitialFolder: cy.stub().returns(of("/photos")),
      getDrives: cy.stub().returns(of(["C:"])),
      getFolders: cy.stub().returns(of(mockFolders)),
    };

    const onFolderSelected = cy.stub();

    cy.mount(FolderNavComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: FolderService, useValue: folderServiceStub },
      ],
    }).then(({ fixture }) => {
      fixture.componentInstance.folderSelected.subscribe(onFolderSelected);
    });

    cy.contains("photos").click();
    cy.wrap(onFolderSelected).should("have.been.calledWith", "/photos");
  });
});
```

---

## 9. Testing EventSource / SSE in Components

Use `MockEventSource` (from section 5) and trigger synthetic events:

```typescript
import { mount } from "cypress/angular";
import { of } from "rxjs";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { GalleryComponent } from "./gallery.component";
import { AssetService } from "../../core/services/asset.service";
import { MockEventSource } from "../../../cypress/support/mock-event-source";

describe("GalleryComponent — catalog SSE", () => {
  it("should update catalogProgress when a catalog event is received", () => {
    const mockSource = new MockEventSource();

    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy
        .stub()
        .returns(of({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 })),
      catalogAssets: cy.stub().returns(mockSource),
    };

    cy.mount(GalleryComponent, {
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AssetService, useValue: assetServiceStub },
      ],
    });

    cy.then(() => {
      mockSource.emit("catalog", {
        percentCompleted: 50,
        message: "Scanning…",
        reason: "SCANNING",
      });
    });

    cy.get("mat-progress-bar").should("have.attr", "aria-valuenow", "50");
  });

  it("should stop cataloging on SSE error", () => {
    const mockSource = new MockEventSource();

    const assetServiceStub: Partial<AssetService> = {
      getAssets: cy
        .stub()
        .returns(of({ items: [], pageIndex: 0, totalPages: 0, totalItems: 0 })),
      catalogAssets: cy.stub().returns(mockSource),
    };

    cy.mount(GalleryComponent, {
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AssetService, useValue: assetServiceStub },
      ],
    }).then(({ fixture }) => {
      expect(fixture.componentInstance.cataloging).to.be.true;
    });

    cy.then(() => {
      mockSource.dispatchEvent(new Event("error"));
    });

    cy.then(
      ({ fixture }: { fixture: { componentInstance: GalleryComponent } }) => {
        expect(fixture.componentInstance.cataloging).to.be.false;
      },
    );
  });
});
```

---

