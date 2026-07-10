---
name: commit
description: >
  Create small, focused commits using Conventional Commits format. Use whenever the user wants
  to commit, save progress, or says "commit", "save my work".
---

# Commit

Create small, focused commits with Conventional Commits messages.

## Workflow

### 1. Branch safety check

Run `git branch --show-current`.
- If on `main`, **stop** — releases cut from `main`; commit on `development` or a
  `development-<name>` branch instead and let it flow through the branching topology in
  CLAUDE.md.
- If the change is a large API-surface update per CLAUDE.md's "Major Update Workflow" but the
  current branch isn't `development-<name>`, point this out before committing — don't silently
  commit a breaking change straight to `development`.
- Otherwise proceed (small changes land directly on `development` in this repo).

### 2. Gather context

Run in parallel:
- `git status` — see all changes (never use `-uall`)
- `git diff` and `git diff --staged` — unstaged and staged changes
- `git log --oneline -5` — recent commit message style

### 3. Plan small commits

Group related changes into small, focused commits — one logical unit each:
- **Refactoring** separate from **new features**
- **Tests** separate from **implementation**
- **Config/CI** separate from **code changes**
- **Docs** (CLAUDE.md, README, `documents/`) separate from **code changes**

### 4. For each commit

a. Stage only the files for this logical change with `git add <specific files>`.
   - Never commit files that likely contain secrets (`.env`, credentials, etc.).
   - Prefer specific paths over `git add -A` / `git add .`.

b. Create the commit using Conventional Commits format via a HEREDOC:

```bash
git commit -m "$(cat <<'EOF'
type(scope): description

Optional body explaining why, not what.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

   - Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `style`, `ci`
   - Scope: the module touched — `core` (`ket4j-core`), `log4j2` (`ket4j-log4j2`), or omit the
     scope for repo-wide changes (root `pom.xml`, CI, CLAUDE.md)
   - Keep the first line under 72 characters
   - Focus on *why*, not *what* — the diff already shows what changed

c. Repeat for each logical group.

### 5. Push — only if the user asked to push

Only run this step if the user's request said "push" (or equivalent) — committing alone does not
imply pushing.

```bash
git push
```
- If the branch has no upstream: `git push -u origin HEAD`.
- Verify upstream tracks the right branch — `git rev-parse --abbrev-ref @{upstream}` should match
  the current branch name, not `origin/main` or an unrelated `origin/development-*` branch.
- Pushing to `development` triggers a CI pre-release deploy (`<version>-BUILD.<run id>`) — that's
  expected and fine. Pushing to `main` deploys the exact version in `pom.xml` for real — confirm
  that's intended before pushing there.

### 6. Report

Report the commit hash(es) for each commit, and whether/where it was pushed.
