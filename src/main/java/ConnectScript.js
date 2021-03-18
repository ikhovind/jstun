let webRTC;

document.getElementById("generateRemote").addEventListener("click", generateRemote(document.getElementById("stunServer").value));

document.getElementById("connectToRemote").addEventListener("click", connectToRemote(document.getElementById("stunServer").value, document.getElementById("remoteDescription").value));

function generateRemote(stunServer){
    console.log(stunServer);
    webRTC = new RTCPeerConnection({
        iceServers: [
            {
                urls: "stun:13.48.195.80:3478",
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
    document.getElementById("connectToRemote").removeEventListener("click")
    document.getElementById("connectToRemote").addEventListener("click",
        clientAConnect(document.getElementById("remoteDescription").value));
}

function connectToRemote(ip, rmDescription){
    const remoteDescription = rmDescription;

    const WebRTCConnection = new RTCPeerConnection({
        iceServers: [
            {
                urls: "stun:13.48.195.80:3478",
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