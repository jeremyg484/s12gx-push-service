(function() {

	var config = {};

	config['client:node'] = {
		environment: 'node',
		rootPath: '../',
		tests: ['client/test/**/*-test.js']
	};

	config['server:node'] = {
		environment: 'node',
		rootPath: '../',
		tests: ['server/test/**/*-test.js']
	};

	if (typeof module !== 'undefined') {
		module.exports = config;
	}

})();