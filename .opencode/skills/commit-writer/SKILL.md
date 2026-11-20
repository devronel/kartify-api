---
name: commit-writer
description: Write a Git commit title and message for the user to copy and paste. Use whenever the user asks for a commit message, commit title, or help summarizing changes or a diff into a commit. Text only - never commits, pushes, or edits anything.
---

# Commit Writer

## Rules

- Only write text. **Never** run `git commit`, `push`, `add`, or any command that changes state.
- **Never** edit, create, or delete any file or code.
- Read-only commands are OK: `git status`, `git diff`, `git diff --staged`.
- If there's no diff, ask the user to paste it or describe the changes. Don't guess.
- If you spot a bug, mention it in one line. Don't fix it.

## Format

```
type(scope): title

- what changed and why (optional body)
```

- Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`
- Title: imperative mood ("add", not "added"), lowercase, no period, max 50 chars
- Scope is optional. Body is optional for small changes.
- Add `Closes #123` only if the user gave an issue number. Never invent one.

## Output

Give the message in one code block, then end with:
"Nothing was committed or changed. Just copy and paste."