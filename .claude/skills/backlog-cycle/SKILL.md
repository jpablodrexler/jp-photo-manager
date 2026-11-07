---
name: backlog-cycle
description: >-
  Keeps a record of each cycle so the estimates can be checked: plans a cycle
  from the forecast (committing the prudent set of features by default), closes
  it against what was really archived (delivered, carried over and why), writes a
  light retro, and shows what the closed cycles say about the estimates: how
  often the forecast capacity was reached, which share of the commitment was
  delivered, which sizes slip. backlog-forecast uses that to calibrate the next
  cycle. Cycle files live in the backlog's cycles folder and are committed by
  the user. TRIGGER when asked to plan, start, close or review a cycle or a
  week, for a retro, whether the estimates were right, or how reliable the
  commitments have been. For what fits in a cycle without recording it use
  backlog-forecast.
license: MIT
metadata:
  author: Juan Pablo Drexler
---

Record a cycle: plan it from the forecast, close it against what was delivered, and let the closed cycles calibrate the next forecast.

`<backlog>` is `docs/backlog` unless `.backlog-kit.toml` sets `[paths] backlog_dir`. Cycle files go in `<backlog>/cycles/<start date>.md` and are meant to be committed: they are the history the calibration reads. They are not a second progress log: what was delivered is read from the archived features, the same dates `backlog-report` uses, so it covers features done inside a batch run and outside one. A batch report (`<reports>/feature-batch/`) stays what it is, a local log to resume a cut run; the only thing `close` takes from it is the reason a feature was blocked.

**Input**: What the user wants: plan, close, or look at the record ("start the next cycle", "close the cycle", "were my estimates right"). Map it to `plan`, `close` or `stats`. For a plan, also take a cycle length ("one-week cycle" is `--cycle-weeks 1`; the default is `[cycles] weeks` of `.backlog-kit.toml`, 2), a history window (`--since`) or a pace by hand (`--capacity`).

---

## Script

`scripts/cycle.py` does the counting and writes the file. Run it from this skill's base directory. Without `--write` it only prints; show that preview before saving anything:

```
python3 scripts/cycle.py plan  <repo-root> [--commit prudent|likely] [--features 12,14] [--start YYYY-MM-DD] [--cycle-weeks N] [--weeks N | --since YYYY-MM-DD] [--capacity POINTS_PER_WEEK] [--write]
python3 scripts/cycle.py close <repo-root> [--cycle FILE] [--reason "#12=why"] [--note "text"] [--write]
python3 scripts/cycle.py stats <repo-root> [--json]
```

### plan

1. Run `plan` without `--write` and show the preview: the features, their points and the capacity the forecast gave. By default the commitment is the **prudent** set (what 85 of 100 cycles reached), never the likely one, unless the user asks for `--commit likely`.
2. Read the "Read this first" notes it prints (little history, a pause, features without points) and say which apply.
3. If the user wants to add or drop features, rerun with `--features` listing the exact set. Say that a commitment above the capacity is a bet against the forecast.
4. After the user approves, rerun with `--write`. One cycle is open at a time: the script refuses to plan while another is open.
5. If nothing fits (the history has weeks with nothing delivered, or the first feature is bigger than the capacity), say so and offer `--commit likely`, a longer cycle or `--features`.

### close

1. Run `close` without `--write` and show the preview: delivered from the plan, delivered in total, bugs fixed, the forecast result and the carried-over features.
2. For each carried-over feature without a reason (the script lists them), ask the user why in one line. The script already fills in a feature archived after the cycle and a reason from a batch report. Rerun with `--reason "#N=text"` for each one.
3. Ask for one or two lines for the retro (what to do differently) and pass them as `--note`. Do not write a retro the user did not give: "No notes." is a valid entry.
4. After the user approves, rerun with `--write`: the file becomes `closed` and gets its `## Close` section. Closing before the end date is allowed and says so in the file.

### stats

Prints every cycle and what the closed ones say: how often the forecast capacity was reached (prudent and likely), the median share of the committed points that was delivered, and how many features of each size were carried over. `backlog-forecast` shows the same and, from 4 closed cycles with a share below 100%, scales the next-cycle sets by it (`--no-calibration` turns it off). Say that before then the figures are a first impression, and that a cycle with a single feature says very little.

## Reading the result

- A carried-over feature is information, not a failure: look for the pattern in the reasons (a missing design, a blocked dependency, a feature bigger than its points) before suggesting a change.
- Points are ordinal. "Delivered 80% of the points" is a rough figure; when the user wants to calibrate, point to the sizes that slip, not to a total.
- Never edit an old cycle file to make a result look better. A wrong reason can be corrected by hand; the figures come from the archive.
- Offer, do not decide: to start the work of a cycle, point to `features-next` and `features-batch-development`; the cycle skill does not run it.

---

## Guardrails

- The only file this skill writes is `<backlog>/cycles/<start>.md`, and only with `--write` after the user saw the preview. It edits no other backlog file.
- Never invent a delivered feature, a reason or a retro line.
- Never reuse an earlier output; run the script against the current files.
- **Never run `git commit`, `git push` or open a PR.** The cycle file is left for the user to commit with the rest of their work.
