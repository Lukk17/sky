# This is the only CLAUDE.md in the entire monorepo.
# All other agents (Kilo Code, OpenCode, Copilot, etc.) read AGENTS.md files directly.
# Claude Code reads this file and pulls in all AGENTS.md content via @ imports below.

@../AGENTS.md

# Per-module AGENTS.md — one shared instruction file per Gradle module.
@../sky-common/AGENTS.md
@../sky-booking/AGENTS.md
@../sky-offer/AGENTS.md
@../sky-message/AGENTS.md
@../sky-notify/AGENTS.md