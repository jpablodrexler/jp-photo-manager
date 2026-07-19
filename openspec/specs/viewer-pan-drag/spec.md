# viewer-pan-drag

The zoomed image viewer supports drag-to-pan using mouse and touch events. Pan resets automatically when zoom returns to 1× or the asset changes.

---

## Requirements

### Requirement: Zoomed image can be panned by mouse drag

When the viewer zoom level is greater than 1×, the user SHALL be able to pan the image by clicking and dragging within the viewer container.

#### Scenario: Dragging pans the image

- **GIVEN** the viewer is zoomed to 2×
- **WHEN** the user clicks and drags the image 100px to the right
- **THEN** the image shifts right (panX increases) so a different region of the image is visible

#### Scenario: Cursor changes to grabbing during drag

- **GIVEN** the viewer is zoomed in
- **WHEN** the user holds the mouse button down over the image
- **THEN** the cursor changes from `grab` to `grabbing`

#### Scenario: Mouse leaving the container ends the drag

- **GIVEN** the user is dragging
- **WHEN** the cursor leaves the viewer container
- **THEN** dragging stops (no accidental continued panning)

### Requirement: Zoomed image can be panned by touch drag on mobile

The touch drag-to-pan behavior SHALL mirror the mouse drag behavior for single-finger touch events.

#### Scenario: Single-finger drag pans the image on mobile

- **GIVEN** the viewer is zoomed to 3× on a mobile device
- **WHEN** the user drags with one finger
- **THEN** the image pans in the drag direction

### Requirement: Pan resets to zero when zoom returns to 1×

When the zoom level is set back to 1× (no zoom), `panX` and `panY` SHALL reset to zero.

#### Scenario: Pan resets on zoom-out to 1×

- **GIVEN** the image has been panned to `panX = 200px, panY = 100px` at 2× zoom
- **WHEN** the user sets the zoom slider back to 1×
- **THEN** `panX` and `panY` reset to 0 and the image is centered

### Requirement: Pan resets when the displayed asset changes

When the viewer navigates to a different asset, `panX` and `panY` SHALL reset to zero.

#### Scenario: Pan resets on asset change

- **GIVEN** the user has panned the image while viewing asset A
- **WHEN** the user navigates to asset B
- **THEN** asset B is displayed centered with `panX = panY = 0`
