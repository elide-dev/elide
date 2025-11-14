# Build Arena Testing Guide

This document describes the automated testing infrastructure for Build Arena, including closed-loop integration tests for race functionality.

## Test Types

### 1. Playwright Browser Tests

Full end-to-end tests that simulate user interactions in a browser.

**Location**: `tests/race-closed-loop.spec.ts`

**Run**:
```bash
npx playwright test race-closed-loop.spec.ts
```

### 2. API-Based Headless Tests

Lightweight tests that interact directly with the API and WebSocket servers.

**Location**: `scripts/api-race-test.ts`

**Run**:
```bash
tsx scripts/api-race-test.ts [repository-url]
```

### 3. Test Runner Script

**Location**: `scripts/run-race-tests.sh`

**Run**:
```bash
./scripts/run-race-tests.sh
```

## Prerequisites

1. Services running: `pnpm dev`
2. Docker images built: `cd docker && ./build-images.sh`
3. Playwright installed: `npx playwright install`

See full documentation in the file for more details.
