import { of, throwError, Subject } from "rxjs";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import { FolderNavComponent } from "./folder-nav.component";
import { FolderService } from "../../core/services/folder.service";
import { Folder } from "../../core/models/folder.model";

describe("FolderNavComponent", () => {
  const mockFolders: Folder[] = [
    { folderId: 1, path: "/photos", name: "photos" },
    { folderId: 2, path: "/documents", name: "documents" },
  ];

  function mountComponent(folderServiceOverrides: Partial<FolderService> = {}) {
    const folderServiceStub: Partial<FolderService> = {
      getFolders: cy.stub().returns(of(mockFolders)),
      ...folderServiceOverrides,
    };

    return cy.mount(FolderNavComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: FolderService, useValue: folderServiceStub },
      ],
    });
  }

  it("should create the component", () => {
    mountComponent().then(({ fixture }) => {
      expect(fixture.componentInstance).to.be.ok;
    });
  });

  it("should load and display folders on init", () => {
    mountComponent();
    cy.contains("photos").should("exist");
    cy.contains("documents").should("exist");
  });

  it("should render a top-level folder when its parent is not returned by the API", () => {
    const foldersWithMissingParent: Folder[] = [
      {
        folderId: 2,
        path: "/photos/vacation",
        name: "vacation",
        parentPath: "/photos",
        // "/photos" is intentionally absent — vacation becomes a root node
      },
    ];

    const getFolders = cy.stub().returns(of(foldersWithMissingParent));
    cy.mount(FolderNavComponent, {
      providers: [
        provideNoopAnimations(),
        { provide: FolderService, useValue: { getFolders } },
      ],
    });

    cy.contains("vacation").should("exist");
  });

  it("should call getFolders without arguments on init", () => {
    const getFolders = cy.stub().returns(of(mockFolders));
    mountComponent({ getFolders });
    cy.wrap(getFolders).should("have.been.calledOnce");
    cy.wrap(getFolders).should("have.been.calledWith");
  });

  it("should emit folderSelected when a folder node is clicked", () => {
    const onFolderSelected = cy.stub();

    mountComponent().then(({ fixture }) => {
      fixture.componentInstance.folderSelected.subscribe(onFolderSelected);
    });

    cy.contains("mat-tree-node", "photos").click();
    cy.wrap(onFolderSelected).should("have.been.calledWith", "/photos");
  });

  it("should update selectedPath when a folder is clicked", () => {
    mountComponent().then(({ fixture }) => {
      cy.contains("mat-tree-node", "photos").click();
      cy.then(() => {
        expect(fixture.componentInstance.selectedPath).to.equal("/photos");
      });
    });
  });

  it("should hide the spinner after folders are loaded", () => {
    mountComponent();
    cy.get("mat-spinner").should("not.exist");
  });

  it("should hide the spinner even on load error", () => {
    mountComponent({
      getFolders: cy
        .stub()
        .returns(throwError(() => new Error("network error"))),
    });
    cy.get("mat-spinner").should("not.exist");
  });

  it("should show the spinner while folders are still loading", () => {
    const subject = new Subject<Folder[]>();
    mountComponent({ getFolders: cy.stub().returns(subject.asObservable()) });
    cy.get("mat-spinner").should("exist");
    cy.then(() => subject.next(mockFolders));
    cy.get("mat-spinner").should("not.exist");
  });

  it("should apply the selected class to the clicked node", () => {
    mountComponent();
    cy.contains("mat-tree-node", "photos").click();
    cy.contains("mat-tree-node", "photos").should("have.class", "selected");
    cy.contains("mat-tree-node", "documents").should("not.have.class", "selected");
  });

  describe("with a nested folder", () => {
    const nestedFolders: Folder[] = [
      { folderId: 1, path: "/photos", name: "photos" },
      { folderId: 2, path: "/photos/vacation", name: "vacation", parentPath: "/photos" },
    ];

    it("should show a toggle button and a collapsed chevron for a folder with children", () => {
      mountComponent({ getFolders: cy.stub().returns(of(nestedFolders)) });
      cy.contains("mat-tree-node", "photos").find('button[aria-label="Toggle photos"]').should("exist");
      cy.contains("mat-tree-node", "photos").find("mat-icon").first().should("contain.text", "chevron_right");
    });

    it("should expand a folder and reveal its child when the toggle is clicked", () => {
      mountComponent({ getFolders: cy.stub().returns(of(nestedFolders)) });
      cy.contains("vacation").should("not.exist");
      cy.get('button[aria-label="Toggle photos"]').click();
      cy.contains("vacation").should("exist");
      cy.contains("mat-tree-node", "photos").find("mat-icon").first().should("contain.text", "expand_more");
    });

    it("should select an expandable node when its row is clicked", () => {
      const onFolderSelected = cy.stub();
      mountComponent({ getFolders: cy.stub().returns(of(nestedFolders)) }).then(({ fixture }) => {
        fixture.componentInstance.folderSelected.subscribe(onFolderSelected);
      });
      cy.contains("mat-tree-node", "photos").click();
      cy.wrap(onFolderSelected).should("have.been.calledWith", "/photos");
    });
  });
});
