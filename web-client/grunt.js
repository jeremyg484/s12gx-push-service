/*global module:false*/
module.exports = function(grunt) {

	var fs, json5;

	fs = require('fs');
	json5 = require('json5');

	// Project configuration.
	grunt.initConfig({
		lint: {
			files: ['grunt.js', 'client/app/**/*.js', 'server/**/*.js']
		},

		jshint: {
			options: json5.parse(''+fs.readFileSync('.jshintrc'))
		},

		csslint: {
			files: ['client/app/**/*.css']
		},

		// Unfortunately, it seems that the grunt-html plugin
		// is broken.  Leaving this in for now in hopes that it
		// gets fixed at some point.
		htmllint: {
			all: ['client/app/**/*.html']
		},

		server: {
			module: './server/main'
		},

		// it'd be nice if there were more magic here:
		"config-amd": {
			// appDir tells us where run.js is on the file system
			// so we can update it.
			appDir: 'client/app',
			// libDir is where the third-party libs reside on the file system.
			// leave this blank to get this from volo.baseUrl:
			libDir: 'client/lib',
			// webRoot is where index.html resides on the file system.
			// this is used to map file system folders to web paths.
			webRoot: 'client'
		},

		buster: {
			test: {
				config: 'test/buster.js'
			}
		},

		watch: {
			files: '<config:lint.files>',
			tasks: 'default'
		}

	});

	grunt.loadNpmTasks('grunt-buster');
	grunt.loadNpmTasks('grunt-css');
	grunt.loadNpmTasks('grunt-html');

	// Use buster for testing
	grunt.registerTask('test', 'buster');

	// htmllint appears to be broken, don't use it yet.
	// grunt.registerTask('lintall', 'lint csslint htmllint');
	grunt.registerTask('lintall', 'lint csslint');

	grunt.registerTask('default', 'lintall test');

};