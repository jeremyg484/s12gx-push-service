/*global SockJS:true*/
define(['rest/interceptor/mime','./util'], function(mime, util){
	var client, Subscription,
		rest = mime(),
        urlSJSSuffix = 'socket';

	if (typeof SockJS === 'undefined') {
	    console.error('Please load SockJS first.');
	    return;
	}

	if (!Function.prototype.bind) {
	    Function.prototype.bind = function (oThis) {
	        if (typeof this !== "function") {
	            // closest thing possible to the ECMAScript 5 internal IsCallable function
	            throw new TypeError("Function.prototype.bind - what is trying to be bound is not callable");
	        }

	        var aArgs = Array.prototype.slice.call(arguments, 1),
	            fToBind = this,
	            NOP = function () {},
	            fBound = function () {
	                return fToBind.apply(this instanceof NOP
	                                     ? this
	                                     : oThis || window,
	                                     aArgs.concat(Array.prototype.slice.call(arguments)));
	            };

	        NOP.prototype = this.prototype;
	        fBound.prototype = new NOP();

	        return fBound;
	    };
	}

	client = function (url) {
	    if (url === undefined) {
	        url = location.protocol + '//' + location.host + location.pathname;
	    }

	    if (url.charAt(url.length - 1) !== '/') {
	        url += '/';
	    }

	    this.url = url;
	    this.prefix = url;
	    this.urlSJS = url + urlSJSSuffix;

	    this._sjsc();
	};

	client.prototype = {
	    _sjsc: function () {
	        if (undefined === this.sjs) {
	            this.sjs = new SockJS(this.urlSJS);
	            this.sjs.onopen = this._onopen.bind(this);
	            this.sjs.onmessage =
	                (function (msg) { this._onmessage(msg.data); }).bind(this);
	            this.sjs.onclose = this._onclose.bind(this);
	        }
	        return this.sjs;
	    },

	    _onopen: function () {
	        this.sjs.send('{}');
	    },

	    _emit: function () {
	        var name, args, fun;
	        args = Array.prototype.slice.call(arguments, 0);
	        name = 'on' + args[0];
	        fun = this[name];
	        if (undefined !== fun && typeof fun === "function") {
	            fun.apply(fun, args.slice(1));
	        }
	    },

	    _post: function (path, data, handler) {
	        var self = this, req;
	        req = {path: self.url + path,
	               method: 'POST',
	               headers: { 'Content-Type': 'application/json'},
	               entity: data};
	        rest(req).then(handler ? handler : function(){});
	    }
	};

	Subscription = function (client, topic) {
	    this.client = client;
	    this.topic = topic;
	    this._ready = false;
	    this.prefix = topic;
	    this._maybe_error = Subscription.prototype._maybe_error.bind(this);
	};

	Subscription.prototype = {
	    _emit: client.prototype._emit,

	    _maybe_error: function (response) {
	        if (response.status.code < 200 || response.status.code >= 300) {
	            this._emit('error');
	            this.errored = true;
	            delete this.client.topics[this.topic];
	            delete this.client._topics[this.topic];
	            this._emitclose();
	        }
	    }
	};

	client.prototype.isReady = function () { return this._ready; };
	client.prototype._ready = false;
	client.prototype.topics = {};
	client.prototype._topics = {};

	client.prototype._onmessage = function (msg) {
	    var obj = JSON.parse(msg),
	        topic, sub;
	    switch (obj.type) {
	    case 'message':
	        if (! this._ready) {
	            console.log('Received message when not ready!');
	            return;
	        }
	        topic = obj.topic;
	        sub = this._topics[topic];
	        if (sub === undefined) {
	            sub = this;
	        }
	        sub._emitmessage(obj);
	        break;
	    case 'subscribe-ok':
	        if (! this._ready) {
	            console.log('Received subscribe-ok when not ready!');
	            return;
	        }
	        topic = obj.topic;
	        sub = this._topics[topic];
	        if (sub !== undefined) {
	            this.topics[topic] = 'ready';
	            sub._emitready();
	        }
	        break;
	    case 'unsubscribe-ok':
	        if (! this._ready) {
	            console.log('Received unsubscribe-ok when not ready!');
	            return;
	        }
	        topic = obj.topic;
	        sub = this._topics[topic];
	        if (sub !== undefined) {
	            delete this.topics[topic];
	            delete this._topics[topic];
	            sub._emitclose();
	        }
	        break;
	    case 'session-ok':
	        if (! this._ready) {
	            this._session_id = obj.id;
	            this._ready = true;
	            this._emit('ready');
	        } else {
	            console.log('Received session-ok when already ready!');
	        }
	        break;
	    default:
	        console.log('Unexpected data from server: ' + msg);
	    }
	};

	client.prototype._onclose = function () {
	    var topics, i;

	    if (this._ready) {
	        this._ready = false;
	        delete this._session_id;

	        topics = Object.keys(this._topics);
	        for (i = 0; i < topics.length; i += 1) {
	            this._topics[topics[i]]._emit('close');
	        }
	        this._topics = {};
	        this.topics = {};

	        this._emit('close');
	    }
	};

	client.prototype._postSession = function (path, cont) {
	    var self = this;
	    return this._post(path, {id: self._session_id}, cont);
	};

	client.prototype.subscribe = function (topic) {
	    if (! this._ready) {
	        return undefined;
	    }
	    var sub = this._topics[topic];
	    if (sub !== undefined) {
	        return sub;
	    }
	    sub = new Subscription(this, topic);
	    sub._subscribe();
	    this._topics[topic] = sub;
	    this.topics[topic] = 'starting';
	    return sub;
	};

	client.prototype.unsubscribe = function (topic) {
	    if (! this._ready) {
	        return;
	    }
	    var sub = this._topics[topic];
	    if (sub === undefined) {
	        return;
	    }
	    sub._unsubscribe();
	    this.topics[topic] = 'stopping';
	    return sub;
	};

	client.prototype.publish = function (topic, message, cont) {
	    return this._post('messages/' + topic + '/', message, cont);
	};

	client.prototype._emitmessage = function (obj) {
	    if (this._ready) {
	        this._emit('message', obj.topic, obj.data);
	    } else {
	        console.log('Received message when not ready!');
	    }
	};

	Subscription.prototype.errored = false;
	Subscription.prototype.isReady = function () { return this._ready; };
	Subscription.prototype._emitmessage = client.prototype._emitmessage;

	Subscription.prototype._emitready = function () {
	    if (! this._ready) {
	        this._ready = true;
	        this._emit('ready', this.topic);
	    }
	};

	Subscription.prototype._emitclose = function () {
	    if (this._ready) {
	        this._ready = false;
	        this._emit('close', this.topic);
	    }
	};

	Subscription.prototype._subscribe = function () {
	    this.client._postSession('subscriptions/' + this.topic + '/',
	                             this._maybe_error);
	};

	Subscription.prototype._unsubscribe = function () {
		/*this should be a DELETE*/
	    /*this.client._postSession('subscriptions/' + this.topic + '/',
	                             this._maybe_error);*/
	};

	Subscription.prototype.unsubscribe = function () {
	    return this.client.unsubscribe(this.topic);
	};

	Subscription.prototype.publish = function (message, cont) {
	    return this.client.publish(this.topic, message, cont);
	};

	return client;
});