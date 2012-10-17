/*global SockJS:true*/
define(['rest/interceptor/mime', 'curl/src/curl/domReady'], function(mime, domReady) {
    var windows = [];
    var name = window.location.hash.substr(1);
    var client = mime();
    if (name) {
        //TODO - communicate with server
        var socket = new WebSocket("ws://localhost:8080");
        socket.onmessage = function(event) {
            var text = event.data;
            speak(text);
        }
        socket.onopen = function() {
            socket.send("Hello WebSocket!");
        }

        domReady(function(){
            var ctx = document.getElementById("character").getContext('2d');
            var img = new Image();
            img.onload = function(){
                ctx.drawImage(img,0,0);
            };
            img.src = 'img/'+name+'.png';
        });
    } else {
        var features = "menuBar=no,width=400,height=300";
        windows.push(window.open("index.html#juergen", "Juergen", features));
        windows.push(window.open("index.html#chris", "Chris", features+",left=410"));
        windows.push(window.open("index.html#mark", "Mark", features+",top=360"));
        windows.push(window.open("index.html#rossen", "Rossen", features+",top=360,left=410"));

        window.addEventListener("focus", focusListener, false);
    }

    function handleMessage(topic, msg) {
        console.log('message received on topic '+topic+': '+JSON.stringify(msg));
        handleCommand(msg);
    }

    function handleCommand(command) {
        if (command.type === "speak") {
            speak(command.text);
        } else if (command.type === "explode") {
            explode();
        } else if (command.status && command.status === "dead") {
            speak("You killed "+capitalize(command.name)+"!! You bastards!!");
        } else {
            speak("WTF!?!?");
        }
    }

    function capitalize(str) {
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    function speak(text) {
        var ctx = document.getElementById("animation").getContext('2d');
        ctx.clearRect(0,0,400,300);
        ctx.fillStyle="white";
        ctx.beginPath();
        ctx.moveTo(305,5);
        ctx.quadraticCurveTo(255,5,255,42.5);
        ctx.quadraticCurveTo(255,100,280,100);
        ctx.quadraticCurveTo(280,120,260,125);
        ctx.quadraticCurveTo(290,120,295,100);
        ctx.quadraticCurveTo(400,100,395,62.5);
        ctx.quadraticCurveTo(400,0,305,5);
        ctx.stroke();
        ctx.fill();
        writeText(ctx, text);
    }

    function writeText(ctx, text) {
        var lines = [];
        var words = text.split(" ");
        var lineNum = 0;
        words.forEach(function(word){
            if (!lines[lineNum]) {
                lines.push(word);
            } else if (lines[lineNum].length + word.length <= 11 || lineNum == 4) {
                lines[lineNum] = lines[lineNum] + " " + word;
            } else {
                lineNum++;
                lines.push(word);
            }
        });

        var y = 35;
        ctx.font = "14pt Arial";
        ctx.fillStyle="black";
        lines.forEach(function(line){
            ctx.fillText(line,265,y);
            y+=18;
        });
    }

    function explode() {
        var ctx = document.getElementById("animation").getContext('2d');
        ctx.clearRect(0,0,400,300);
        var img = new Image();
        img.onload = function(){
            ctx.drawImage(img,75,0);
        };
        img.src = 'img/explosion.png';
        service.onmessage = function() {};
        client({path: 'http://localhost:8080/messages/broadcast/',
                    headers: { 'Content-Type': 'application/json'},
                    entity: {name: name, status: "dead"}});
        window.setTimeout(function(){
            window.close();
        }, 2000);
    }

    function focusListener() {
        windows.forEach(function(win) {
            win.close();
        });
        window.removeEventListener("focus", focusListener, false);
    }
});