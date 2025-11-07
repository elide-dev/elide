import { drizzle } from 'drizzle-orm/libsql';
import { createClient } from '@libsql/client';
import * as schema from './schema.js';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

// Database file location - store in backend directory
const DB_PATH = process.env.DATABASE_PATH || join(__dirname, '..', '..', 'build-arena.db');

// Create libSQL client
const client = createClient({
  url: `file:${DB_PATH}`,
});

// Create Drizzle instance
export const db = drizzle(client, { schema });

// Export schema for use in queries
export * from './schema.js';

console.log(`Database initialized at: ${DB_PATH}`);
