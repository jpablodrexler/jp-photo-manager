import { mount } from "cypress/angular";
import { of, throwError } from "rxjs";
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
});
