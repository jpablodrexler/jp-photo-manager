# archive-support

Zip and tar.gz files appear as virtual folders in the gallery. Images inside archives are cataloged using a `!` path separator. Bulk downloads support both zip and tar.gz output formats.

---

## ADDED Requirements

### Requirement: Zip and tar.gz archives are treated as virtual folders during cataloging

The catalog service SHALL detect `.zip`, `.tar.gz`, and `.tgz` files, extract their image entries to a temp location, and catalog each image with a virtual `folder_path` of the form `<archivePath>!/<entryDirectory>`.

#### Scenario: Zip archive is cataloged as a virtual folder

- **GIVEN** a zip file `/photos/album.zip` containing `summer/beach.jpg` and `winter/snow.jpg`
- **WHEN** the catalog runs
- **THEN** two assets are created with `folder_path = "/photos/album.zip!/summer/"` and `"/photos/album.zip!/winter/"` respectively

#### Scenario: Tar.gz archive is cataloged

- **GIVEN** a tar.gz file `/photos/archive.tar.gz` containing `holiday.jpg`
- **WHEN** the catalog runs
- **THEN** an asset is created with `folder_path = "/photos/archive.tar.gz!/"`

### Requirement: Virtual folder nodes appear in the folder tree

The `FolderNavComponent` SHALL render virtual archive folders (paths containing `!`) with an archive icon to distinguish them from real filesystem folders.

#### Scenario: Virtual folder shown with archive icon

- **GIVEN** an archive has been cataloged creating virtual folder paths
- **WHEN** the folder tree loads
- **THEN** archive virtual folders appear with an `archive` (or similar) icon; clicking navigates to the assets within

### Requirement: Bulk downloads support tar.gz format

`GET /api/assets/download?assetIds=...&format=tar.gz` SHALL produce a tar.gz archive of the selected assets.

#### Scenario: Download as tar.gz

- **GIVEN** 5 assets are selected for download
- **WHEN** `GET /api/assets/download?assetIds=1,2,3,4,5&format=tar.gz` is called
- **THEN** the response is a `application/gzip` file containing all 5 assets in a tar.gz archive

#### Scenario: Download defaults to zip when format is not specified

- **GIVEN** 5 assets are selected for download
- **WHEN** `GET /api/assets/download?assetIds=1,2,3,4,5` is called (no format parameter)
- **THEN** the response is the existing zip format (backward compatible)
