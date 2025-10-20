import { readFileSync, existsSync } from 'node:fs';
import path from 'node:path';
const p = path.join('.play', 'hello.ts');
console.log('fs existsSync?', existsSync(p));
console.log('fs readFileSync length:', readFileSync(p).length);

