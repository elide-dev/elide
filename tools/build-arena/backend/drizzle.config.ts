import type { Config } from 'drizzle-kit';
import { join } from 'path';

export default {
  schema: './src/db/schema.ts',
  out: './drizzle',
  dialect: 'sqlite',
  dbCredentials: {
    url: process.env.DATABASE_PATH || join(process.cwd(), 'backend', 'build-arena.db'),
  },
} satisfies Config;
