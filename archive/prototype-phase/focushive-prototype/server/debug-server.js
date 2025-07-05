const net = require('net');

const server = net.createServer((socket) => {
  socket.write('HTTP/1.1 200 OK\r\n');
  socket.write('Content-Type: text/plain\r\n');
  socket.write('\r\n');
  socket.write('Hello World');
  socket.end();
});

server.on('error', (err) => {
  console.error('Server error:', err);
});

server.on('listening', () => {
  console.log('TCP server listening:', server.address());
});

server.listen(3000, '127.0.0.1', () => {
  console.log('Server started');
});