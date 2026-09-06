# Accessibility Audit Report (frontend) — 2026-09-06

**Commit:** c0f4ef0
**Generated:** 2026-09-06T03:03:03.632Z
**Scope:** 11 authenticated routes (cypress-axe/axe-core, mocked session + intercepted `/api/**` calls — no live backend), the deep complement to `npm run lighthouse:report`'s Lighthouse accessibility score for `/login`: /home, /gallery, /sync, /convert, /duplicates, /admin/users, /albums, /albums/1, /recycle-bin, /analytics, /profile/sessions.

**Total violations:** 50

## Violations by impact

| Impact | Count |
| --- | --- |
| critical | 5 |
| serious | 13 |
| moderate | 32 |
| minor | 0 |

## Violations by route

### `/home` (3)

| Rule | Impact | Nodes | Description | Help |
| --- | --- | --- | --- | --- |
| `color-contrast` | serious | 14 (.active > .mdc-button__label; a[href$="gallery"] > .mdc-button__label; a[href$="sync"] > .mdc-button__label; +11 more) | Ensure the contrast between foreground and background colors meets WCAG 2 AA minimum contrast ratio thresholds | [Elements must meet minimum color contrast ratio thresholds](https://dequeuniversity.com/rules/axe/4.13/color-contrast?application=axeAPI) |
| `landmark-one-main` | moderate | 1 (html) | Ensure the document has a main landmark | [Document should have one main landmark](https://dequeuniversity.com/rules/axe/4.13/landmark-one-main?application=axeAPI) |
| `region` | moderate | 24 (span[_ngcontent-ng-c3661584105=""]:nth-child(2); .active > .mdc-button__label; a[href$="gallery"] > .mdc-button__label; +21 more) | Ensure all page content is contained by landmarks | [All page content should be contained by landmarks](https://dequeuniversity.com/rules/axe/4.13/region?application=axeAPI) |

### `/gallery` (6)

| Rule | Impact | Nodes | Description | Help |
| --- | --- | --- | --- | --- |
| `aria-treeitem-name` | serious | 1 (mat-tree-node) | Ensure every ARIA treeitem node has an accessible name | [ARIA treeitem nodes should have an accessible name](https://dequeuniversity.com/rules/axe/4.13/aria-treeitem-name?application=axeAPI) |
| `button-name` | critical | 1 (.mat-mdc-button-disabled) | Ensure buttons have discernible text | [Buttons must have discernible text](https://dequeuniversity.com/rules/axe/4.13/button-name?application=axeAPI) |
| `color-contrast` | serious | 12 (a[routerlink="/home"] > .mdc-button__label; a[routerlink="/sync"] > .mdc-button__label; a[routerlink="/convert"] > .mdc-button__label; +9 more) | Ensure the contrast between foreground and background colors meets WCAG 2 AA minimum contrast ratio thresholds | [Elements must meet minimum color contrast ratio thresholds](https://dequeuniversity.com/rules/axe/4.13/color-contrast?application=axeAPI) |
| `landmark-one-main` | moderate | 1 (html) | Ensure the document has a main landmark | [Document should have one main landmark](https://dequeuniversity.com/rules/axe/4.13/landmark-one-main?application=axeAPI) |
| `page-has-heading-one` | moderate | 1 (html) | Ensure that the page, or at least one of its frames contains a level-one heading | [Page should contain a level-one heading](https://dequeuniversity.com/rules/axe/4.13/page-has-heading-one?application=axeAPI) |
| `region` | moderate | 28 (span[_ngcontent-ng-c3661584105=""]:nth-child(2); a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; +25 more) | Ensure all page content is contained by landmarks | [All page content should be contained by landmarks](https://dequeuniversity.com/rules/axe/4.13/region?application=axeAPI) |

### `/sync` (6)

| Rule | Impact | Nodes | Description | Help |
| --- | --- | --- | --- | --- |
| `button-name` | critical | 3 (.mat-mdc-button-disabled.mdc-icon-button[mat-ripple-loader-disabled=""]:nth-child(1); .mat-mdc-button-disabled.mdc-icon-button[mat-ripple-loader-disabled=""]:nth-child(2); .mat-warn) | Ensure buttons have discernible text | [Buttons must have discernible text](https://dequeuniversity.com/rules/axe/4.13/button-name?application=axeAPI) |
| `color-contrast` | serious | 11 (a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; .active > .mdc-button__label; +8 more) | Ensure the contrast between foreground and background colors meets WCAG 2 AA minimum contrast ratio thresholds | [Elements must meet minimum color contrast ratio thresholds](https://dequeuniversity.com/rules/axe/4.13/color-contrast?application=axeAPI) |
| `label` | critical | 2 (#mat-mdc-checkbox-0-input; #mat-mdc-checkbox-1-input) | Ensure every form element has a label | [Form elements must have labels](https://dequeuniversity.com/rules/axe/4.13/label?application=axeAPI) |
| `landmark-one-main` | moderate | 1 (html) | Ensure the document has a main landmark | [Document should have one main landmark](https://dequeuniversity.com/rules/axe/4.13/landmark-one-main?application=axeAPI) |
| `page-has-heading-one` | moderate | 1 (html) | Ensure that the page, or at least one of its frames contains a level-one heading | [Page should contain a level-one heading](https://dequeuniversity.com/rules/axe/4.13/page-has-heading-one?application=axeAPI) |
| `region` | moderate | 18 (span[_ngcontent-ng-c3661584105=""]:nth-child(2); a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; +15 more) | Ensure all page content is contained by landmarks | [All page content should be contained by landmarks](https://dequeuniversity.com/rules/axe/4.13/region?application=axeAPI) |

### `/convert` (6)

| Rule | Impact | Nodes | Description | Help |
| --- | --- | --- | --- | --- |
| `button-name` | critical | 3 (.mat-mdc-button-disabled.mdc-icon-button[mat-ripple-loader-disabled=""]:nth-child(1); .mat-mdc-button-disabled.mdc-icon-button[mat-ripple-loader-disabled=""]:nth-child(2); .mat-warn) | Ensure buttons have discernible text | [Buttons must have discernible text](https://dequeuniversity.com/rules/axe/4.13/button-name?application=axeAPI) |
| `color-contrast` | serious | 10 (a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; a[routerlink="/sync"] > .mdc-button__label; +7 more) | Ensure the contrast between foreground and background colors meets WCAG 2 AA minimum contrast ratio thresholds | [Elements must meet minimum color contrast ratio thresholds](https://dequeuniversity.com/rules/axe/4.13/color-contrast?application=axeAPI) |
| `label` | critical | 1 (#mat-mdc-checkbox-0-input) | Ensure every form element has a label | [Form elements must have labels](https://dequeuniversity.com/rules/axe/4.13/label?application=axeAPI) |
| `landmark-one-main` | moderate | 1 (html) | Ensure the document has a main landmark | [Document should have one main landmark](https://dequeuniversity.com/rules/axe/4.13/landmark-one-main?application=axeAPI) |
| `page-has-heading-one` | moderate | 1 (html) | Ensure that the page, or at least one of its frames contains a level-one heading | [Page should contain a level-one heading](https://dequeuniversity.com/rules/axe/4.13/page-has-heading-one?application=axeAPI) |
| `region` | moderate | 17 (span[_ngcontent-ng-c3661584105=""]:nth-child(2); a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; +14 more) | Ensure all page content is contained by landmarks | [All page content should be contained by landmarks](https://dequeuniversity.com/rules/axe/4.13/region?application=axeAPI) |

### `/duplicates` (4)

| Rule | Impact | Nodes | Description | Help |
| --- | --- | --- | --- | --- |
| `color-contrast` | serious | 11 (a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; a[routerlink="/sync"] > .mdc-button__label; +8 more) | Ensure the contrast between foreground and background colors meets WCAG 2 AA minimum contrast ratio thresholds | [Elements must meet minimum color contrast ratio thresholds](https://dequeuniversity.com/rules/axe/4.13/color-contrast?application=axeAPI) |
| `landmark-one-main` | moderate | 1 (html) | Ensure the document has a main landmark | [Document should have one main landmark](https://dequeuniversity.com/rules/axe/4.13/landmark-one-main?application=axeAPI) |
| `page-has-heading-one` | moderate | 1 (html) | Ensure that the page, or at least one of its frames contains a level-one heading | [Page should contain a level-one heading](https://dequeuniversity.com/rules/axe/4.13/page-has-heading-one?application=axeAPI) |
| `region` | moderate | 18 (span[_ngcontent-ng-c3661584105=""]:nth-child(2); a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; +15 more) | Ensure all page content is contained by landmarks | [All page content should be contained by landmarks](https://dequeuniversity.com/rules/axe/4.13/region?application=axeAPI) |

### `/admin/users` (4)

| Rule | Impact | Nodes | Description | Help |
| --- | --- | --- | --- | --- |
| `color-contrast` | serious | 10 (a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; a[routerlink="/sync"] > .mdc-button__label; +7 more) | Ensure the contrast between foreground and background colors meets WCAG 2 AA minimum contrast ratio thresholds | [Elements must meet minimum color contrast ratio thresholds](https://dequeuniversity.com/rules/axe/4.13/color-contrast?application=axeAPI) |
| `landmark-one-main` | moderate | 1 (html) | Ensure the document has a main landmark | [Document should have one main landmark](https://dequeuniversity.com/rules/axe/4.13/landmark-one-main?application=axeAPI) |
| `page-has-heading-one` | moderate | 1 (html) | Ensure that the page, or at least one of its frames contains a level-one heading | [Page should contain a level-one heading](https://dequeuniversity.com/rules/axe/4.13/page-has-heading-one?application=axeAPI) |
| `region` | moderate | 18 (span[_ngcontent-ng-c3661584105=""]:nth-child(2); a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; +15 more) | Ensure all page content is contained by landmarks | [All page content should be contained by landmarks](https://dequeuniversity.com/rules/axe/4.13/region?application=axeAPI) |

### `/albums` (4)

| Rule | Impact | Nodes | Description | Help |
| --- | --- | --- | --- | --- |
| `color-contrast` | serious | 10 (a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; a[routerlink="/sync"] > .mdc-button__label; +7 more) | Ensure the contrast between foreground and background colors meets WCAG 2 AA minimum contrast ratio thresholds | [Elements must meet minimum color contrast ratio thresholds](https://dequeuniversity.com/rules/axe/4.13/color-contrast?application=axeAPI) |
| `landmark-one-main` | moderate | 1 (html) | Ensure the document has a main landmark | [Document should have one main landmark](https://dequeuniversity.com/rules/axe/4.13/landmark-one-main?application=axeAPI) |
| `page-has-heading-one` | moderate | 1 (html) | Ensure that the page, or at least one of its frames contains a level-one heading | [Page should contain a level-one heading](https://dequeuniversity.com/rules/axe/4.13/page-has-heading-one?application=axeAPI) |
| `region` | moderate | 15 (span[_ngcontent-ng-c3661584105=""]:nth-child(2); a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; +12 more) | Ensure all page content is contained by landmarks | [All page content should be contained by landmarks](https://dequeuniversity.com/rules/axe/4.13/region?application=axeAPI) |

### `/albums/1` (4)

| Rule | Impact | Nodes | Description | Help |
| --- | --- | --- | --- | --- |
| `color-contrast` | serious | 9 (a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; a[routerlink="/sync"] > .mdc-button__label; +6 more) | Ensure the contrast between foreground and background colors meets WCAG 2 AA minimum contrast ratio thresholds | [Elements must meet minimum color contrast ratio thresholds](https://dequeuniversity.com/rules/axe/4.13/color-contrast?application=axeAPI) |
| `landmark-one-main` | moderate | 1 (html) | Ensure the document has a main landmark | [Document should have one main landmark](https://dequeuniversity.com/rules/axe/4.13/landmark-one-main?application=axeAPI) |
| `page-has-heading-one` | moderate | 1 (html) | Ensure that the page, or at least one of its frames contains a level-one heading | [Page should contain a level-one heading](https://dequeuniversity.com/rules/axe/4.13/page-has-heading-one?application=axeAPI) |
| `region` | moderate | 14 (span[_ngcontent-ng-c3661584105=""]:nth-child(2); a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; +11 more) | Ensure all page content is contained by landmarks | [All page content should be contained by landmarks](https://dequeuniversity.com/rules/axe/4.13/region?application=axeAPI) |

### `/recycle-bin` (4)

| Rule | Impact | Nodes | Description | Help |
| --- | --- | --- | --- | --- |
| `color-contrast` | serious | 9 (a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; a[routerlink="/sync"] > .mdc-button__label; +6 more) | Ensure the contrast between foreground and background colors meets WCAG 2 AA minimum contrast ratio thresholds | [Elements must meet minimum color contrast ratio thresholds](https://dequeuniversity.com/rules/axe/4.13/color-contrast?application=axeAPI) |
| `landmark-one-main` | moderate | 1 (html) | Ensure the document has a main landmark | [Document should have one main landmark](https://dequeuniversity.com/rules/axe/4.13/landmark-one-main?application=axeAPI) |
| `page-has-heading-one` | moderate | 1 (html) | Ensure that the page, or at least one of its frames contains a level-one heading | [Page should contain a level-one heading](https://dequeuniversity.com/rules/axe/4.13/page-has-heading-one?application=axeAPI) |
| `region` | moderate | 14 (span[_ngcontent-ng-c3661584105=""]:nth-child(2); a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; +11 more) | Ensure all page content is contained by landmarks | [All page content should be contained by landmarks](https://dequeuniversity.com/rules/axe/4.13/region?application=axeAPI) |

### `/analytics` (5)

| Rule | Impact | Nodes | Description | Help |
| --- | --- | --- | --- | --- |
| `color-contrast` | serious | 9 (a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; a[routerlink="/sync"] > .mdc-button__label; +6 more) | Ensure the contrast between foreground and background colors meets WCAG 2 AA minimum contrast ratio thresholds | [Elements must meet minimum color contrast ratio thresholds](https://dequeuniversity.com/rules/axe/4.13/color-contrast?application=axeAPI) |
| `landmark-one-main` | moderate | 1 (html) | Ensure the document has a main landmark | [Document should have one main landmark](https://dequeuniversity.com/rules/axe/4.13/landmark-one-main?application=axeAPI) |
| `page-has-heading-one` | moderate | 1 (html) | Ensure that the page, or at least one of its frames contains a level-one heading | [Page should contain a level-one heading](https://dequeuniversity.com/rules/axe/4.13/page-has-heading-one?application=axeAPI) |
| `region` | moderate | 17 (span[_ngcontent-ng-c3661584105=""]:nth-child(2); a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; +14 more) | Ensure all page content is contained by landmarks | [All page content should be contained by landmarks](https://dequeuniversity.com/rules/axe/4.13/region?application=axeAPI) |
| `scrollable-region-focusable` | serious | 1 (.app-content) | Ensure elements that have scrollable content are accessible by keyboard in Safari | [Scrollable region must have keyboard access](https://dequeuniversity.com/rules/axe/4.13/scrollable-region-focusable?application=axeAPI) |

### `/profile/sessions` (4)

| Rule | Impact | Nodes | Description | Help |
| --- | --- | --- | --- | --- |
| `color-contrast` | serious | 10 (a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; a[routerlink="/sync"] > .mdc-button__label; +7 more) | Ensure the contrast between foreground and background colors meets WCAG 2 AA minimum contrast ratio thresholds | [Elements must meet minimum color contrast ratio thresholds](https://dequeuniversity.com/rules/axe/4.13/color-contrast?application=axeAPI) |
| `landmark-one-main` | moderate | 1 (html) | Ensure the document has a main landmark | [Document should have one main landmark](https://dequeuniversity.com/rules/axe/4.13/landmark-one-main?application=axeAPI) |
| `page-has-heading-one` | moderate | 1 (html) | Ensure that the page, or at least one of its frames contains a level-one heading | [Page should contain a level-one heading](https://dequeuniversity.com/rules/axe/4.13/page-has-heading-one?application=axeAPI) |
| `region` | moderate | 18 (span[_ngcontent-ng-c3661584105=""]:nth-child(2); a[routerlink="/home"] > .mdc-button__label; a[routerlink="/gallery"] > .mdc-button__label; +15 more) | Ensure all page content is contained by landmarks | [All page content should be contained by landmarks](https://dequeuniversity.com/rules/axe/4.13/region?application=axeAPI) |
