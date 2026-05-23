# image-comparison-viewer

Selecting exactly two assets in the gallery enables a "Compare" toolbar action that opens a split-screen `ComparisonViewerComponent` showing both images side by side with metadata beneath each panel.

---

## ADDED Requirements

### Requirement: Compare action is available when exactly two assets are selected

The gallery toolbar SHALL show an enabled "Compare" button when exactly two assets are selected, and SHALL hide or disable it otherwise.

#### Scenario: Compare button enabled for two selected assets

- **GIVEN** the user has selected exactly two assets in the gallery
- **WHEN** the toolbar is rendered
- **THEN** the "Compare" button is visible and enabled

#### Scenario: Compare button not shown for one selected asset

- **GIVEN** the user has selected only one asset
- **WHEN** the toolbar is rendered
- **THEN** the "Compare" button is not shown or is disabled

#### Scenario: Compare button not shown for three or more selected assets

- **GIVEN** the user has selected three or more assets
- **WHEN** the toolbar is rendered
- **THEN** the "Compare" button is not shown or is disabled

### Requirement: ComparisonViewerComponent displays two images side by side

The `ComparisonViewerComponent` SHALL display both selected assets in a 50/50 split layout, each showing the full image with a metadata row beneath.

#### Scenario: Both images are displayed

- **GIVEN** the user clicks "Compare" with two assets selected
- **WHEN** the `ComparisonViewerComponent` opens
- **THEN** both images are displayed side by side, each loading from `GET /api/assets/{id}/image`

#### Scenario: Metadata is shown beneath each image

- **GIVEN** the comparison viewer is open
- **WHEN** the panels are rendered
- **THEN** each panel shows the asset's filename, file size (human-readable), and image dimensions beneath the image

### Requirement: Zoom level is synchronized across both panels

A zoom slider SHALL control the scale of both images simultaneously.

#### Scenario: Zoom slider scales both images equally

- **GIVEN** the comparison viewer is open
- **WHEN** the user moves the zoom slider to 2x
- **THEN** both images scale to 2x simultaneously

### Requirement: Closing the comparison viewer returns to the gallery

A "Close" button SHALL dismiss the `ComparisonViewerComponent` and return to the gallery with the same selection state.

#### Scenario: Close returns to gallery

- **GIVEN** the comparison viewer is open
- **WHEN** the user clicks "Close"
- **THEN** the comparison viewer is dismissed and the gallery is visible with the two assets still selected
