# Changelog

Overclauditor versions are `<Clauditor base>-oc.<n>`. Everything below is on top of
Clauditor 0.7.30 (https://github.com/bdkent/clauditor).

## 0.7.30-oc

### Added
- `reopenSessionsAtStart` in `~/.overclauditor/config.json` (default `true`): `false` opens the
  project with no session tabs instead of reopening the ones left open.
- Session tabs wrap onto more rows when they run out of space; other editor tab strips keep the
  IDE's own one-row setting.
- Right-click the branch name or the summary text on the toolbar to copy it (the whole summary,
  the green topic or the teal what-to-test line).
- Clicking a usage bar (5h, 7d, per-model) opens a menu with "Open Claude Usage Page"
  (https://claude.ai/settings/usage).
- Model and effort dropdowns on the toolbar, colour-coded, sized to their options; models show
  their version (learned live from the status line). Effort: low, medium, high, xhigh, max.
- Branch, change count (`✎4 (2)`: changed files, how many by this session) and the session
  summary ("what it's about - what to test") on the same row; the summary wraps smaller
  instead of growing the bar.
- Configurable command buttons (`~/.overclauditor/config.json`): slash commands or key chords,
  text or icon. Defaults: plan, merge, push, pull. Bundled `/overclauditor:merge`, `:push`, `:pull` commands, loaded per session with `--plugin-dir` (nothing installed).
- Bottom bar: 5-hour, weekly and per-model weekly usage, prompt-cache countdown, context; all
  bars ramp green to red.
- Built-in session summary via a background Haiku call, or read from your own file.
- Pinned tab titles that change only on a worktree switch, the first file edit, /rename, or a
  new CLI title. Short tab titles (a few whole words).
- New session from the editor tab bar (right-click or double-click, and the tab menu), and a
  Resume submenu with the last 10 sessions; no name
  prompt, the CLI titles it.
- Icons for fork, Messages, and icon buttons; a new plugin icon.
- `hideTerminalStatusLine`: hide your own status line inside the plugin (off by default).
- Daily upstream check for maintainers (`tools/upstream-check.sh`, `docs/upstream-updates.md`).

- Red tab badge for a session whose latest reply is an API error (logged out, rate limited,
  overloaded): a ring until you look at the tab, a dot after, gone when the session recovers.
  Read from the transcript's own error flag; no model calls.

### Changed
- A session's name is its worktree's name while it is in one (set when it enters a worktree it
  hasn't been in), else its short task title. The plugin also gives Claude that name (`/rename`,
  sent only while the session is idle at its prompt), so the session's cross-session address in
  ListAgents / SendMessage is `comet-art`, not `emberskies-12`. A user's own /rename still wins.
  Two open sessions in one worktree get `comet-art` and `comet-art-2`, so each address is unique.
- Session list loads in the background (Clauditor built it on the UI thread: 16-26 s freezes
  on large histories), parses with a streaming reader (about 2x faster), and keeps a saved
  index so each start reads only new lines.
- The blank status-line row under the prompt is collapsed.
- Clauditor's worktree bar (Commit, Update, Merge, Show Changes, Open in...) is behind a
  setting, off by default; when off it is never built and polls nothing.
- The Commit button is gone; caches moved to the IDE system folder and are removed on uninstall.

### Fixed
- The "work started" retitle took the summary of the latest request, which can be a side job
  (another session asking for a fix): "Asset Bench moved to localhost" on a HUD session. It now
  prefers the CLI's title for the whole session, and the built-in summary skips messages from
  other sessions.
- A session started inside a worktree (its transcript lives under the worktree) was missing
  from the Sessions list and the Resume menu, listed only in Worktrees. Both list every session now.
- False "Session unresponsive" warnings: any input with no output within 3s counted as a hang,
  but Claude Code draws nothing (or seconds later) for focus in/out, Shift+Tab, arrows and Enter
  on an empty box. Only typed text (which echoes in ~10ms) arms the check now, after 10s.
- The tab's tree icon showed only for sessions started as worktree sessions; it now shows while
  the session is working in a worktree, including after EnterWorktree. Half its old size.
- Tab titles changed too often: every move between worktrees retitled (and /merge and /push step
  into main and back), as did every new CLI title. Now a title changes only for a worktree the
  session has never been in, the first file edit, a CLI title naming clearly different work (at
  most a third of the words shared), or /rename.
- Restored tabs came back in the order they were first opened, not the order you'd dragged them
  into; the saved list now follows the tab order.
- A new session's tab could stay "New Session" for good: the tab was tied to its Claude session
  by guessing from transcript timestamps, which missed or picked the wrong session (and an
  unlinked tab got no title, no retitles and no restore after a restart). It now links from the
  `session_id` in the status line Claude reports for that tab.
- Closing a session tab could freeze the IDE for up to 2 seconds while it waited for Claude to
  exit on the UI thread; the wait now runs in the background. (Tab drags no longer close the
  session at all.)
- Dragging a session tab (reordering, splitting, moving to another window) restarted the session
  and lost its terminal history: the IDE closes and reopens the tab, and the plugin killed Claude
  on close and ran `claude --resume` on reopen. The running session now lives on the tab's file,
  as the IDE's own terminal tabs do, and a close made to reopen (CLOSING_TO_REOPEN) hands it to
  the new editor.
- The model and effort dropdowns saved the pick as Claude Code's default for every new session
  (and said so), so one tab's "low" became every resumed session's effort. They now drive the
  `/model` and `/effort` pickers' session-only key, reading the screen at each step; settings.json
  is never touched. Resumed sessions and forks start on the effort their transcript last records
  (`--effort`; Claude Code restores the model itself but starts effort on the saved default); new
  sessions on `defaultModel` / `defaultEffort`.
- A summary that came back as a sentence ("Cannot build the status line: the images did not
  come through...") could become the tab title. Titles must now be title-shaped (48 characters,
  8 words, no colon, code, path or closing full stop); a bad pinned title is repaired on load, and
  neither the built-in summarizer nor the summary bar keeps such an answer.
- Command buttons and the model/effort dropdowns no longer submit a half-typed prompt along
  with the command: a draft in the input box is stashed first (Claude Code's ctrl+s) and comes
  back once the command has run. The `plan` button now actually enters plan mode with a draft
  present.
- Error text (e.g. "Invalid API key · Please run /login") could become a tab title or the summary:
  titles now never take error-like text, and failed summarizer runs are never saved.
- Titles showed the raw first prompt or `<local-command-caveat>`; now the CLI's own title.
- The model dropdown showed Sonnet until the first status update.
- "Session unresponsive" fired while a resume was loading and never cleared once raised.
- Open tabs didn't restore on restart unless the Sessions pane was open.
