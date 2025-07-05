const http = require('http');

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/plain' });
  res.end('OK');
});

server.listen(3001, '127.0.0.1', () => {
  console.log('Minimal server running on http://127.0.0.1:3001');
  console.log('Server listening:', server.listening);
  console.log('Address:', server.address());
});