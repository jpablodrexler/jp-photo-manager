#!/usr/bin/env bash
# Reports which SecurityConfig rule governs every REST endpoint, as a
# factual snapshot — not a judgment call. The risk this guards against: a
# new controller endpoint added under /api/** inherits only the coarse
# .authenticated() rule when it should require ADMIN, and nobody notices
# because SecurityConfig's ordered requestMatchers rules are easy to lose
# track of as controllers are added — this table makes the *actual*
# resolved rule per endpoint visible instead of requiring a manual reread
# of SecurityConfig.java every time.
#
# Parses SecurityConfig.java's ordered .requestMatchers(...).RULE() chain
# (first-match-wins, mirroring Spring Security's own matching semantics)
# and every @RestController's @RequestMapping/@GetMapping/etc annotations
# across infrastructure/web/controller/*.java, then resolves each endpoint
# against the ordered rule list. Also flags whether a method carries its
# own @PreAuthorize — a real, separate, *tighter* restriction SecurityConfig
# alone won't show, so this report surfaces it as a second column rather
# than trying to fully resolve the combined semantics itself.
#
# This is necessarily approximate — it doesn't execute Spring's real
# PathPattern matcher, just a regex approximation of Ant-style ** globs —
# so treat a surprising row as a prompt to go read SecurityConfig.java and
# the controller directly, not as ground truth on its own.
#
# Writes a dated snapshot report under
# JPPhotoManagerWeb/docs/reports/auth-coverage/, the same convention
# bundle-size-report.js/dependency-staleness-report.js use.
#
# Usage: bash scripts/auth-coverage-report.sh (run from backend/, or
# anywhere — it cd's to backend/ itself)

set -euo pipefail

cd "$(dirname "$0")/.."

REPO_ROOT="$(git rev-parse --show-toplevel)"
DATE="$(date -u +%F)"
TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"

SECURITY_CONFIG="src/main/java/com/jpablodrexler/photomanager/config/SecurityConfig.java"
CONTROLLER_DIR="src/main/java/com/jpablodrexler/photomanager/infrastructure/web/controller"

TABLE="$(perl -e '
  use strict;
  use warnings;

  my $security_config_path = shift @ARGV;
  my $controller_dir = shift @ARGV;

  # --- Parse SecurityConfig.java: ordered list of [patterns, rule] pairs,
  # plus the final anyRequest() fallback. ---
  open(my $sc, "<", $security_config_path) or die "cannot open $security_config_path: $!";
  local $/;
  my $sc_src = <$sc>;
  close $sc;

  my @rules;
  while ($sc_src =~ /\.requestMatchers\(([^)]*)\)\.(\w+)\(([^)]*)\)/g) {
    my ($patterns_raw, $rule, $rule_arg) = ($1, $2, $3);
    my @patterns = ($patterns_raw =~ /"([^"]+)"/g);
    next if $rule eq "dispatcherTypeMatchers"; # not a path matcher
    my $label = $rule eq "hasRole" ? "hasRole($rule_arg)" : "$rule()";
    push @rules, { patterns => \@patterns, label => $label };
  }
  my ($any_rule) = ($sc_src =~ /\.anyRequest\(\)\.(\w+)\(\)/);
  $any_rule = $any_rule ? "$any_rule() [anyRequest fallback]" : "UNKNOWN";

  sub pattern_to_regex {
    my ($pattern) = @_;
    my $re = quotemeta($pattern);
    $re =~ s/\\\*\\\*/.*/g;   # ** -> any depth
    $re =~ s/\\\*/[^\/]*/g;  # *  -> single segment
    return qr/^$re$/;
  }

  sub resolve_rule {
    my ($path) = @_;
    for my $r (@rules) {
      for my $p (@{ $r->{patterns} }) {
        my $re = pattern_to_regex($p);
        return $r->{label} if $path =~ $re;
      }
    }
    return $any_rule;
  }

  # --- Parse each controller: class-level @RequestMapping base path, then
  # every method-level @XxxMapping (+ optional @PreAuthorize right after). ---
  opendir(my $dh, $controller_dir) or die "cannot open $controller_dir: $!";
  my @files = sort grep { /\.java$/ } readdir($dh);
  closedir $dh;

  for my $file (@files) {
    open(my $fh, "<", "$controller_dir/$file") or next;
    local $/;
    my $src = <$fh>;
    close $fh;

    # The class-level @RequestMapping is not always immediately adjacent to
    # "public class" (other annotations like @RequiredArgsConstructor can
    # sit between them) — take the first @RequestMapping anywhere in the
    # file. A controller with no class-level @RequestMapping at all
    # (MediaController puts the full absolute path directly on each
    # @GetMapping) defaults to an empty base rather than being skipped.
    my ($base) = ($src =~ /\@RequestMapping\("([^"]*)"\)/);
    $base //= "";
    my ($class_name) = ($file =~ /^(.*)\.java$/);

    while ($src =~ /\@(Get|Post|Put|Delete|Patch)Mapping(?:\("([^"]*)"\))?\s*\n(\s*\@PreAuthorize\("([^"]*)"\)\s*\n)?/g) {
      my ($verb, $sub_path, $preauth_block, $preauth_expr) = ($1, $2, $3, $4);
      $sub_path //= "";
      my $full_path = $base . $sub_path;
      $full_path =~ s{//+}{/}g;
      my $rule = resolve_rule($full_path);
      my $preauth = defined $preauth_expr ? $preauth_expr : "-";
      print uc($verb) . "\t$full_path\t$class_name\t$rule\t$preauth\n";
    }
  }
' "$SECURITY_CONFIG" "$CONTROLLER_DIR" | sort -t$'\t' -k2,2)"

ENDPOINT_COUNT="$(echo "$TABLE" | grep -c . || true)"
FALLBACK_COUNT="$(echo "$TABLE" | grep -c 'anyRequest fallback' || true)"

REPORT_DIR="$REPO_ROOT/JPPhotoManagerWeb/docs/reports/auth-coverage"
mkdir -p "$REPORT_DIR"
REPORT_PATH="$REPORT_DIR/AUTH_COVERAGE_REPORT_${DATE}_backend.md"

{
  echo "# Auth Coverage Report (backend) — $DATE"
  echo
  echo "**Commit:** $COMMIT"
  echo "**Generated:** $TIMESTAMP"
  echo "**Scope:** $ENDPOINT_COUNT endpoints across infrastructure/web/controller/*.java, resolved against SecurityConfig.java's ordered requestMatchers rules"
  echo
  echo "Factual snapshot of which SecurityConfig rule governs each endpoint — not a pass/fail gate. An endpoint resolving to the \`anyRequest\` fallback isn't automatically wrong (SecurityConfig's own fallback is \`permitAll()\`, and some endpoints are intentionally public), but it means no explicit \`requestMatchers\` rule covers it — worth a second look if that endpoint touches user data. \`@PreAuthorize\` (if present) is a real, separate, tighter restriction this table doesn't attempt to merge into the \"Governing rule\" column — read both."
  echo
  if [ "$FALLBACK_COUNT" -gt 0 ]; then
    echo "**⚠ $FALLBACK_COUNT endpoint(s) resolve only to the \`anyRequest\` fallback** — no explicit \`requestMatchers\` rule in SecurityConfig.java covers them."
    echo
  fi
  echo "| Method | Path | Controller | Governing rule | @PreAuthorize |"
  echo "| --- | --- | --- | --- | --- |"
  echo "$TABLE" | awk -F'\t' '{printf "| %s | %s | %s | %s | %s |\n", $1, $2, $3, $4, $5}'
  echo
} > "$REPORT_PATH"

cat "$REPORT_PATH"
echo
echo "Report written to $REPORT_PATH"
