<!-- SPDX-License-Identifier: Apache-2.0
     https://www.apache.org/legal/release-policy.html -->

# overrides — manage agentic overrides for framework skills

The agentic-overrides mechanism is the framework's answer to
"how does an adopter modify a framework skill's behaviour
without forking the framework". An override file lives at
`.apache-magpie-overrides/<framework-skill>.md` in the
adopter repo (committed). The framework skill consults the
file at run-time **before** executing default behaviour and
applies the agent-readable instructions in it.

This sub-action helps the user manage those override files —
list them, scaffold a new one, or open an existing one.

The full *contract* (what an override file may contain, how
the framework skill applies it, the hard rules that bound the
mechanism) lives in
[`docs/setup/agentic-overrides.md`](../../docs/setup/agentic-overrides.md)
in the framework. This file is the operational helper.

## Inputs

- `<framework-skill>` — required. The skill name to scaffold
  / open the override for (e.g. `pr-management-triage`).
- `--local` — place the override in `.apache-magpie-local/`
  (personal, gitignored) instead of `.apache-magpie-overrides/`
  (committed, project-wide). Default: prompt.

## Step 0 — Pre-flight

1. The named `<framework-skill>` must exist in the snapshot at
   `<repo-root>/.apache-magpie/skills/<framework-skill>/` (or
   in the in-repo `skills/` for a self-adopted framework
   checkout). If not, name the typo and list the available
   framework skills.
2. For the committed surface (`.apache-magpie-overrides/`): the
   repo must be adopted (see [`verify.md`](verify.md) check 1 +
   check 5). If not, redirect to `/magpie-setup adopt` **or**
   suggest using `--local` to create a personal override
   without adopting.

## Step 0b — Choose the override surface

If `--local` was passed, route to the personal surface:
`<override-path>` = `<repo-root>/.apache-magpie-local/<framework-skill>.md`.

If `--local` was not passed and the repo is adopted, offer a
choice:

> *"Where should this override live?*
>
> - **Personal** (`.apache-magpie-local/`, gitignored) —
>   only you see it; works even on repos you have not adopted
>   Magpie into.  Use for per-person paths, local tooling,
>   role-specific capabilities.
> - **Shared** (`.apache-magpie-overrides/`, committed) —
>   all contributors see it on their next clone or pull.
>   Use for project-wide process changes.*"

If the repo is not adopted and `--local` was not passed:
offer only the personal surface and note that the committed
surface requires adoption first.

Default to **personal** when the user has not expressed a
preference.

## Step 1 — Resolve the override path

Per the surface chosen in Step 0b:
- **Personal:** `<override-path>` = `<repo-root>/.apache-magpie-local/<framework-skill>.md`.
- **Shared:** `<override-path>` = `<repo-root>/.apache-magpie-overrides/<framework-skill>.md`.

Also check the *other* surface: if a file already exists
there for the same skill, surface its headlines so the user
can decide whether to consolidate.

If `<override-path>` already exists, this is an *open*
operation: surface the file's current content, ask the user
what they want to change, walk through the edit. Same as if
they had opened the file in their editor — the agent is just
doing it agentically.

If `<override-path>` doesn't exist, this is a *scaffold*
operation: continue to Step 2.

## Step 2 — Scaffold a new override

Read the framework skill's structure to know what the
override might target — the skill's section headings, golden
rules, decision-table rows, etc. Surface these as candidate
override anchors.

Ask the user what they want to override:

- *"Skip Step N"* → invalidate a specific step.
- *"Replace Step N with: ..."* → replace a step's behaviour.
- *"Add a new step before/after Step N: ..."* → insert.
- *"Always do X regardless of the framework's classification"*
  → pre-empt the framework's decision logic.
- Free-form — the agent interprets at run-time.

Generate the override file with the user's instructions.
Use the canonical scaffold below.

## Override file scaffold

Use the same scaffold for both surfaces.  The `Surface:` comment
distinguishes personal from shared overrides.

```markdown
<!-- SPDX-License-Identifier: Apache-2.0
     https://www.apache.org/legal/release-policy.html -->

<!-- apache-magpie agentic override
     Framework skill:    <framework-skill>
     Surface:            personal (.apache-magpie-local/) OR
                         shared (.apache-magpie-overrides/)
     Pinned to snapshot: see ../.apache-magpie.lock for the SHA
                          this override was authored against.
     Applied by:         the framework skill at run-time, before
                          executing default behaviour. -->

# Overrides for `<framework-skill>`

## Why these overrides exist

(One paragraph explaining the local context. Why does this
adopter need to deviate from the framework's default? Future
maintainers — including the agent on a later run — read this
to know whether the override is still relevant.)

## Overrides

### Override 1 — <one-line headline>

(Free-form agent-readable instructions. The framework skill
applies these before running its default behaviour. Be
specific about which step / golden rule / decision-table row
the override targets.)

### Override 2 — <one-line headline>

(...)
```

## Step 3 — Surface the contract reminders

Whenever the skill scaffolds or opens an override file,
remind the user:

1. **Never modify the snapshot** at
   `<repo-root>/.apache-magpie/`. Local mods go in
   `.apache-magpie-local/` (personal) or
   `.apache-magpie-overrides/` (shared).
2. **`.apache-magpie-local/` is gitignored and personal.**
   Do not commit or push it. If others on the project need
   the same behaviour, move the override into the committed
   `.apache-magpie-overrides/` instead.
3. **If the override is widely useful, upstream it.** Open a
   PR against `apache/magpie` implementing the change
   in the framework skill itself. The framework will then
   apply the change on every adopter's next
   `/magpie-setup upgrade`, and this adopter's override
   becomes redundant — at which point the user deletes it.
4. **Re-anchor on framework upgrades.** The skill's
   [`upgrade.md`](upgrade.md) sub-action surfaces conflicts
   when a framework upgrade restructures a skill the user has
   an override for. Re-anchor when prompted.

## Failure modes

- **Snapshot missing** → redirect to `/magpie-setup upgrade`.
- **Skill name typo** → list available skills, ask again.
- **Non-adopted repo + no `--local` flag** → the committed
  surface is unavailable; offer the personal surface
  (`.apache-magpie-local/`) as the fallback and note the
  user should run `/magpie-setup` to adopt if they want
  the shared surface.
- **The override target is on a framework skill that does
  not consult overrides** → the framework treats overrides
  as opt-in per skill (each skill that supports overrides
  documents this in its own `SKILL.md`). If the named
  skill doesn't yet support overrides, surface that and
  suggest opening a framework-side issue requesting the
  hook.
