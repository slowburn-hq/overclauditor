# Taking Clauditor updates

`tools/upstream-check.sh` printed new upstream commits. Nothing is merged automatically: we
have rewritten parts of the same files, so every commit gets a decision.

1. **Decide each commit yourself** (owner, 2026-11-17: merge upstream changes without asking, after
   testing): take / skip / conflicts, as below. Report what you took afterwards.
   - *take*: a fix or feature that doesn't touch what we changed, or improves it.
   - *skip*: undoes or duplicates our change (e.g. reintroduces the `max` button, the commit
     button, the name prompt, title-from-first-prompt).
   - *conflicts*: touches code we rewrote. Our hot spots: `ClaudeSessionEditor.kt` (toolbar,
     dropdowns, summary, bars), `ClaudeSessionService.kt` (titles, streaming parse, index),
     `ClaudeStatusService.kt` (status-line wrapper), `ActivityMonitoringTtyConnector.kt`,
     `SessionDisplay.kt`, plugin id/name in `plugin.xml`, `build.gradle.kts`, `gradle.properties`.
2. **Warn the owner before publishing** only if the update could disrupt their running sessions.
3. On a branch: `git cherry-pick <sha>` each pick, oldest first. Resolve conflicts keeping our
   behaviour plus their intent; never take their `pluginVersion`, plugin `id` or `name`.
4. `./gradlew buildPlugin test` must pass.
5. Merge to `main`, `tools/publish-local.sh`, `git push`.
6. Skipped commits stay listed by the check forever; that is fine, the user already decided.
   If they become noise, record skipped SHAs in this file under "Skipped" and filter them.

Already-taken commits: the check filters any upstream commit whose patch `main` already carries
under a different SHA (`--cherry-pick`). In 2026-11 it listed 18 such commits (7ba89dd and
earlier, up to the PR #4 merge 64a8b1c); `git cherry -v main upstream/main` showed every one as
`-`, so nothing was taken.
