import { createServer } from "node:http";


const server = createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/plain' });
  res.end('Hello World! 🚀\n');
}); 
const PORT = 3000;

server.listen(PORT, () => {
    console.log(`Server running at http://localhost:${PORT}/`);
  });