# Feature 2 — virtual-scrolling-gallery

Replace the CSS grid with a fixed-height list layout; render only visible items in the DOM via `CdkVirtualScrollViewport` with `*cdkVirtualFor`; remove the unused `ScrollingModule` import and the IntersectionObserver sentinel; each list row shows the thumbnail on the left and filename, size, and date metadata on the right
