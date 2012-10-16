/*jshint es5:true, node:true*/
var http, express, path, host, port, app;

http = require('http');
express = require('express');

path = process.env.PWD + '/client';
host = '127.0.0.1';
port = 8000;

app = express();

app.configure(function() {
	app.use(app.router);
	app.use(express.static(path));
	app.use(express.directory(path));
	app.use(express.errorHandler({
		dumpExceptions: true,
		showStack: true
	}));
});

console.log("Serving @ " + host + ":" + port + " (" + path + ")");
app.listen(port, host);