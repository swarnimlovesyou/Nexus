# Nexus Documentation Site

This folder contains the Vite + React documentation site for Nexus.

## Purpose

- Explain implemented Nexus features and workflows
- Mirror real CLI behavior and supported commands
- Provide usage guidance for routing, memory, API keys, sessions, and finance analytics

## Local Development

Run from the site folder:

```bash
npm install
npm run dev
```

## Production Build

```bash
npm run build
```

Build output is generated in the dist folder.

## Content Rule

Documentation copy should stay aligned with the implementation in the Java CLI.
Avoid describing commands or platform behaviors that are not currently supported.
