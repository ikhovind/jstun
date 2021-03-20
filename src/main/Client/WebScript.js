
const WebRTCConnection = new RTCPeerConnection({
    iceServers: [
        {
            urls: 'stun:13.48.195.80:3478',
        },
    ],
});
chatChannel = WebRTCConnection.createDataChannel('chat');

function loadForm() {
    document.getElementById("leftInputBox").readOnly = true;

    if (localStorage.getItem("joinGenerate") === "join") {
        document.getElementById("chatButton").addEventListener("click", () => acceptOffer());
    } else {
        createOffer();
        document.getElementById("chatButton").addEventListener("click", () => openConnecion());
    }


    function createOffer() {
        chatChannel.onmessage = (event) => console.log('onmessage:', event.data);
        chatChannel.onopen = () => {
            document.location = "chatWindow.html";
        }
        chatChannel.onclose = () => console.log('onclose');

        WebRTCConnection.onicecandidate = (event) => {
            if (event.candidate)
                document.getElementById("leftInputBox").value = JSON.stringify(WebRTCConnection.localDescription);
            document.getElementById("leftInputBox").readOnly = true;
        };

        WebRTCConnection.createOffer().then((localDescription) => {
            WebRTCConnection.setLocalDescription(localDescription);
        });
    }

    function acceptOffer() {
        const remoteDescription = document.getElementById("rightInputBox").value;


        WebRTCConnection.ondatachannel = (event) => {
            if (event.channel.label == 'chat') {
                chatChannel = event.channel;
                //?
                chatChannel.onmessage = (event) => recieveMessage(event.data);
                chatChannel.onopen = () => {
                    document.location = "chatWindow.html";
                }
                chatChannel.onclose = () => console.log('onclose');
            }
        };

        WebRTCConnection.onicecandidate = (event) => {
            if (event.candidate)
                document.getElementById("leftInputBox").value = JSON.stringify(WebRTCConnection.localDescription);
        };

        WebRTCConnection.setRemoteDescription(JSON.parse(remoteDescription));

        WebRTCConnection.createAnswer().then((localDescription) => {
            WebRTCConnection.setLocalDescription(localDescription);
        });
    }

    function openConnecion() {
        const remoteDescription = document.getElementById("rightInputBox").value;
        WebRTCConnection.setRemoteDescription(JSON.parse(remoteDescription));
        console.log("json: " + JSON.stringify(WebRTCConnection));

    }
}

function setChat(){
    console.log("ok dette funker");
    document.getElementById("sendButton").addEventListener("click", ()=>sendMessage());
/*
    chatChannel.onmessage = (event) => {
        console.log("heihva faen")
        recieveMessage(event.data);
    }

 */
    function sendMessage(){
        let message = document.getElementById("inputText").value;
        document.getElementById("inputText").value = "";
        document.getElementById("chatDiv").innerHTML += ("<p style='text-align: right'>" + message + "</p>");
        chatChannel.send("<p style='text-align: right'>" + message + "</p>")
        //do something with webrtc here
    }

    function recieveMessage(message){
        //called from eventListener on webRTC :)
        document.getElementById("chatDiv").innerHTML += ("<p style='text-align: left'>" + message + "</p>");

    }
}