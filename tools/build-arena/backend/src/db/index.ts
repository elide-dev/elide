import { drizzle } from 'drizzle-orm/libsql';
import { createClient } from '@libsql/client';
import * as schema from './schema.js';
import { join } from 'path';

// Database file location - store in backend directory
// Use process.cwd() to ensure consistency with drizzle.config.ts
const DB_PATH = process.env.DATABASE_PATH || join(process.cwd(), 'build-arena.db');

// Create libSQL client
const client = createClient({
  url: `file:${DB_PATH}`,
});

// Create Drizzle instance
export const db = drizzle(client, { schema });

// Export schema for use in queries
export * from './schema.js';

console.log(`Database initialized at: ${DB_PATH}`);
