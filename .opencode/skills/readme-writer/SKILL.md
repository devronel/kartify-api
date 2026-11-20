---
name: readme-writer
description: Write or improve a README for any project, as text for the user to copy and paste. Use when the user asks for a README, project docs, or help improving one.
---

# README Writer

## Rules

- Only write text. **Never** edit, create, or delete files, and never run git commands that change state.
- Read-only inspection is OK (list files, read `package.json`, config files, existing README) to understand the project.
- Only describe what you can see in the project. Never invent features, commands, or badges. If unsure, ask.

## Structure

Use only the sections that apply:

1. **Title + one-line description**
2. **Features** (short bullets)
3. **Installation**
4. **Usage** (with a short example)
5. **Configuration** (env vars, options)
6. **Contributing / License** (only if relevant)

Keep it short, clear, and use code blocks for commands.

## Output

Give the README in one markdown code block, then end with:
"Nothing was changed. Just copy and paste."