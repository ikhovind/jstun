window.onload = () => {
    document.getElementById("leftInputBox").readOnly = true;

    if(localStorage.getItem("joinGenerate") === "join"){
        document.getElementById("chatButton").addEventListener("click", ()=> acceptOffer());
    }
    else{
        createOffer();
        document.getElementById("chatButton").addEventListener("click", ()=> openConnecion());
      }
}

let chatChannel;

const WebRTCConnection = new RTCPeerConnection({
    iceServers: [
        {
            urls: 'stun:13.48.195.80:3478',
        },
    ],
});

function createOffer(){
    chatChannel = WebRTCConnection.createDataChannel('chat');
    chatChannel.onmessage = (event) => console.log('onmessage:', event.data);
    chatChannel.onopen = () => console.log('onopen');
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

function acceptOffer(){
    const remoteDescription = document.getElementById("rightInputBox").value;


    WebRTCConnection.ondatachannel = (event) => {
        if (event.channel.label == 'chat') {
            chatChannel = event.channel;
            chatChannel.onmessage = (event) => console.log('onmessage:', event.data);
            chatChannel.onopen = () => console.log('onopen');
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

function openConnecion(){
    const remoteDescription = document.getElementById("rightInputBox").value;
    WebRTCConnection.setRemoteDescription(JSON.parse(remoteDescription));
}