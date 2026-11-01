# GitHub workflow

Use this routine when publishing changes to the `main` branch.

1. Start from the project folder and check the current branch and working tree with `git status`.
2. Bring in the latest GitHub changes with `git pull --rebase origin main` before starting new work.
3. Make and review the intended changes. Keep machine-specific settings, credentials, build output, and generated caches out of the commit. The repository `.gitignore` covers the common Android and Python cases.
4. Review the exact files and changes with `git status` and `git diff`. Stage only the files intended for this change.
5. Build or run the relevant checks when appropriate, then commit with a short, descriptive message.
6. Push the commit with `git push origin main` and confirm the branch is up to date with `git status`.

If Git reports a conflict or a rejected push, stop and inspect the remote changes before continuing. Never force-push to `main` as a routine fix.
