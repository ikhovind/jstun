let webRTC;

document.getElementById("generateRemote").addEventListener("click", generateRemote());

document.getElementById("connectToRemote").addEventListener("click", connectToRemote(document.getElementById("remoteDescription").value));

function generateRemote(){
    webRTC = new RTCPeerConnection({
        iceServers: [
            {
                urls: "stun:127.0.0.1:3478",
            },
        ],
    });

    const chatChannel = webRTC.createDataChannel('chat');
    chatChannel.onmessage = (event) => console.log('onmessage:', event.data);
    chatChannel.onopen = () => console.log('onopen');
    chatChannel.onclose = () => console.log('onclose');

    webRTC.onicecandidate = (event) => {
        if (event.candidate)
            document.getElementById("remoteDescription").value = JSON.stringify(webRTC.localDescription);
    };

    webRTC.createOffer().then((localDescription) => {
        webRTC.setLocalDescription(localDescription);
    });
    document.getElementById("connectToRemote").removeEventListener("click", generateRemote)
    document.getElementById("connectToRemote").addEventListener("click",
        clientAConnect(document.getElementById("remoteDescription").value));
}

function connectToRemote(rmDescription){
    const remoteDescription = rmDescription;

    const WebRTCConnection = new RTCPeerConnection({
        iceServers: [
            {
                urls: "stun:127.0.0.1:3478",
            },
        ],
    });

    let chatChannel;
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
            document.getElementById("remoteDescription").value = JSON.stringify(webRTC.localDescription);
    };

    WebRTCConnection.setRemoteDescription(remoteDescription);

    WebRTCConnection.createAnswer().then((localDescription) => {
        WebRTCConnection.setLocalDescription(localDescription);
    });
}

function clientAConnect(remoteDescription){
    webRTC.setRemoteDescription(remoteDescription);
}