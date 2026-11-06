---
name: backlog-forecast
description: >-
  Forecasts what the backlog can deliver from what it delivered before: what
  fits in the next cycle (a prudent and a likely set), a cycle-by-cycle plan
  with dates, how long a given set of features takes, and how many cycles and
  what date it takes to finish the known backlog, each as a range (typical,
  prudent, very prudent) learnt from the weekly pace of the last weeks. That
  pace already contains the limits of the Claude subscription, the time spent
  on bugs and the interruptions, so none of them is typed in. Read-only.
  TRIGGER when asked what could be built next or in the next cycle, when the
  pending backlog could be done, how many cycles or weeks are left, for a
  delivery date or a roadmap, or how long some features would take. For what
  was already delivered use backlog-report instead.
license: MIT
metadata:
  author: Juan Pablo Drexler
---

Forecast what the backlog can still deliver and by when, from `<backlog>/features-planned.md` and the history of `<backlog>/features-implemented.md`. Read-only: it prints a forecast and never edits a file.

`<backlog>` is `docs/backlog` unless `.backlog-kit.toml` sets `[paths] backlog_dir`.

**Input**: Optionally what the user wants to know and how to look at it. Map their words to the script's options: "in two-week cycles" is `--cycle-weeks 2`; "how long would #19 and #22 take" is `--features 19,22`; "the pace since I changed plan" is `--since YYYY-MM-DD`; "assume 15 points a week" is `--capacity 15`. With nothing given, use the defaults: one-week cycles, the last 12 complete weeks as history, a plan of six cycles.

---

## Script

`scripts/forecast.py` does the counting and the simulation. Run it from this skill's base directory and show its output as it is, instead of working the numbers out by hand:

```
python3 scripts/forecast.py <repo-root> [--weeks N | --since YYYY-MM-DD] [--cycle-weeks N] [--cycles N] [--capacity POINTS_PER_WEEK] [--features 19,22] [--json]
```

It learns the pace from the points and the features delivered in each complete week of the history window, resamples those weeks at random 20,000 times with a fixed seed (the same data gives the same output) and reads the answer off the runs:

- **Typical (P50):** half of the runs finish by then.
- **Prudent (P85):** 85 of 100 runs finish by then. For a cycle, the prudent capacity is what 85 of 100 cycles reached.
- **Very prudent (P95):** used only for the finishing date of the whole backlog.

It prints the history, the pace of one cycle, the **next cycle** (a prudent and a likely set of features), the **set** you asked about, a **cycle-by-cycle table** with the date each group of features is reached (typical and prudent), the **finish of the known backlog** by points and by number of features, a **Read this first** list of warnings and a **What this does not say** list.
The order of the work is the one `features-next` recommends, with a feature placed after the pending prerequisite it is blocked on.
The history and the dates behind it come from the same code as `backlog-report`, including the date of the archived SDD change.

How the subscription enters: the kit cannot read the limit of the user's plan and does not need to. The weekly pace already holds it, together with the time spent on bugs, the interruptions and how fast Claude worked. The price is that the forecast assumes the next weeks look like the window. If the plan, its limits or the way of working changed, start the window after the change with `--since`.

## Reading the result

- **Give ranges, never a single date.** Say what the columns mean in plain words ("in half of the simulated runs it is done by 20 November; in 85 of 100 by 27 November") and say which history window the pace comes from.
- **Start with "Read this first" when it has anything.** Each note names a reason to doubt the numbers: little history, a recent pause, many weeks with nothing delivered, a very wide range, points and features disagreeing, features without points, dates that are not reliable. Say which apply before quoting a date. A **`no_history`** warning means there is nothing to forecast from: do not invent a pace; ask for a window with deliveries (`--since`) or a pace by hand (`--capacity`).
- **Points are ordinal.** A sum of points is a rough size; that is why the finish is also given by number of features. When the two differ a lot, say the remaining work is bigger or smaller than what was delivered.
- **Bugs are not forecast apart.** Their cost is already in the pace. If the user expects a burst of bugs, or fewer, suggest a pace by hand (`--capacity`) instead of adjusting the dates yourself.
- **Only the known backlog.** Features planned later are not in it, so the finishing date is a date for what is written down today.
- **Unsized features are left out.** A pending feature without points is not in the cycles or in the points; offer to estimate it with `feature-plan`.
- **Offer, do not decide.** If the user wants to act on the next cycle, point to `features-next` (which feature to start) and `features-batch-development` (to run a list); do not start work from here.

---

## Guardrails

- Read-only: never edit a backlog file or save the forecast. If the user wants to keep it, show it in the chat for them to copy.
- Always run the script against the current files; never reuse an earlier forecast.
- Never present the output as a promise. The forecast is a range under the assumption that the pace of the window continues.
- If `features-planned.md` is missing or has no table, report that and stop.
- **Never run `git commit`, `git push` or open a PR.**
