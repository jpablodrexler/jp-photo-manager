# Dependency Vulnerability (SCA) Report (frontend) — 2026-08-15

**Commit:** a62ecce
**Generated:** 2026-08-15T20:28:53.149Z
**Scope:** `frontend/` npm dependency tree (`npm audit`, npm's own advisory database)

**12 vulnerable package(s):** 0 critical, 7 high, 5 moderate, 0 low, 0 info.

| Package | Severity | Vulnerable range | Fix available | Advisory |
| --- | --- | --- | --- | --- |
| @angular-devkit/build-angular | high | <=22.2.0-next.0 | yes (@angular-devkit/build-angular@0.1002.1) | less; webpack-dev-server |
| brace-expansion | high | <=1.1.17 || 2.0.0 - 2.1.3 || 3.0.0 - 5.0.8 | yes | brace-expansion: Large numeric range defeats documented `max` DoS protection; brace-expansion: DoS via exponential-time expansion of consecutive non-expanding {} groups; brace-expansion: DoS via exponential-time expansion of consecutive non-expanding {} groups; brace-expansion: DoS via exponential-time expansion of consecutive non-expanding {} groups; brace-expansion: DoS via unbounded expansion length causing an out-of-memory process crash; brace-expansion: DoS via unbounded expansion length causing an out-of-memory process crash; brace-expansion: DoS via unbounded expansion length causing an out-of-memory process crash; brace-expansion: DoS via unbounded intermediate arrays, bypassing the CVE-2026-14257 mitigation; brace-expansion: DoS via unbounded intermediate arrays, bypassing the CVE-2026-14257 mitigation; brace-expansion: DoS via unbounded intermediate arrays, bypassing the CVE-2026-14257 mitigation |
| fast-uri | high | 3.0.0 - 3.1.4 | yes | fast-uri vulnerable to host confusion via literal backslash authority delimiter; fast-uri vulnerable to host confusion via backslash authority introducer; fast-uri vulnerable to path traversal via percent-encoded dot segments; fast-uri vulnerable to host confusion via percent-encoded authority delimiters; fast-uri vulnerable to host confusion via failed IDN canonicalization |
| image-size | high | * | yes (@angular-devkit/build-angular@0.1002.1) | image-size: ICNS parser allows denial of service through an infinite loop; image-size: JXL and HEIF parsers allow denial of service through infinite loops |
| js-yaml | high | <=3.15.0 | yes | JS-YAML: Quadratic-complexity DoS in merge key handling via repeated aliases; js-yaml: YAML merge-key chains can force quadratic CPU consumption; JS-YAML: Quadratic CPU consumption in !!omap resolution (3.x and 4.x) — CVE-2026-59870 fix not backported |
| less | high | 2.2.0 - 4.6.7 | yes (@angular-devkit/build-angular@0.1002.1) | image-size |
| tmp | high | <0.2.6 | yes | tmp has Path Traversal via unsanitized prefix/postfix that enables directory escape |
| qs | moderate | 6.11.1 - 6.15.1 | yes | qs has a remotely triggerable DoS: qs.stringify crashes with TypeError on null/undefined entries in comma-format arrays when encodeValuesOnly is set |
| sockjs | moderate | >=0.3.17 | yes (@angular-devkit/build-angular@0.1002.1) | uuid |
| typed-rest-client | moderate | 2.3.1 - 3.0.0 | yes | qs |
| uuid | moderate | <11.1.1 | yes (@angular-devkit/build-angular@0.1002.1) | uuid: Missing buffer bounds check in v3/v5/v6 when buf is provided |
| webpack-dev-server | moderate | 2.0.0-beta - 5.2.6 | yes (@angular-devkit/build-angular@0.1002.1) | sockjs |

Critical/high severity with a fix available is the priority — `npm audit fix` (or `--force` for a breaking major bump, review the changelog first) resolves those directly. A vulnerability with no fix available needs a judgment call: is the vulnerable code path actually reachable in this app (many advisories are in a devDependency's own build tooling, never shipped or executed against untrusted input), and if so, is there an interim mitigation until upstream publishes a fix.
