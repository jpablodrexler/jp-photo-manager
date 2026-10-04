# Feature 24 — wallpaper-suggestion

Add `aspect_ratio` float column to `assets` (populated during cataloging from the existing `pixel_width`/`pixel_height` columns); `GET /api/assets/wallpaper-suggestion?screenWidth=W&screenHeight=H` returns a random non-deleted asset where `pixel_width >= W`, `pixel_height >= H`, and `aspect_ratio` is within ±0.02 of the desktop ratio; frontend reads `window.screen.width`/`height`, calls the endpoint, and shows the suggested image with a download button
