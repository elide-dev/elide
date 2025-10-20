const pySum = Polyglot.eval('python', 'sum([1,2,3,4])');
let Instant;
try {
  Instant = Java.type('java.time.Instant');
} catch (e) {
  console.log('Java.type not available:', String(e));
}
console.log('python sum =', pySum);
if (Instant) console.log('java instant =', Instant.now().toString());

