function sleep(ms: number) { return new Promise(res => setTimeout(res, ms)); }
console.log('before await');
await sleep(5);
console.log('after await');

