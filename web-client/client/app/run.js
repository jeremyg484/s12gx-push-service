/*global curl:true*/
(function(curl) {

	var config = {
		baseUrl: '',
		pluginPath: 'lib/curl/src/curl/plugin',
		packages: [{"name":"curl","location":"lib/curl","main":"./src/curl"},{"name":"rest","location":"lib/rest","main":"./rest"},{"name":"when","location":"lib/when","main":"when"}]
	};

	curl(config, ['app/main']);

})(curl);