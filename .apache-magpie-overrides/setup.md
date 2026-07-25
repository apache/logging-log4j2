# Override: setup (snapshot management)

On this machine the `.apache-magpie/` snapshot is **not** a
`git clone` made by the install recipe. It is a **git worktree**
of the local framework checkout at `~/workspace/magpie`, created
with:

    git -C ~/workspace/magpie worktree add --detach <main-checkout>/.apache-magpie <tag>

and it lives in the **main checkout** of this repository
(`git worktree list` shows it first); every other worktree of this
repository reaches it through a `.apache-magpie` symlink, as
`worktree-init` prescribes.

Consequences for the `magpie-setup` sub-actions:

- **`upgrade` must not delete `.apache-magpie/` and re-clone.**
  Deleting it would leave stale worktree metadata in
  `~/workspace/magpie/.git/worktrees`. Instead, upgrade with:

      git -C .apache-magpie fetch apache --tags
      git -C .apache-magpie checkout --detach <new-tag>

  then update both lock files as usual.
- **`unadopt`** should remove the snapshot with
  `git -C ~/workspace/magpie worktree remove <path>` (or follow
  removal with `git -C ~/workspace/magpie worktree prune`).
- Everything else (lock semantics, symlink wiring, drift
  detection against the committed lock) applies unchanged; the
  snapshot's `git -C .apache-magpie rev-parse HEAD` is the
  `fetched_commit` for the local lock.