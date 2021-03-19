
function createOffer(){
    const WebRTCConnection = new RTCPeerConnection({
        iceServers: [
            {
                urls: 'stun:13.48.195.80:3478',
            },
        ],
    });

    const chatChannel = WebRTCConnection.createDataChannel('chat');
    chatChannel.onmessage = (event) => console.log('onmessage:', event.data);
    chatChannel.onopen = () => {
        console.log('onopen');
        sessionStorage.setItem("someVarKey", WebRTCConnection);
        window.location.href = "chatGUI.html";
    }
    chatChannel.onclose = () => console.log('onclose');

    WebRTCConnection.onicecandidate = (event) => {
        if (event.candidate)
            console.log('localDescription:', JSON.stringify(WebRTCConnection.localDescription));
    };

    WebRTCConnection.createOffer().then((localDescription) => {
        WebRTCConnection.setLocalDescription(localDescription);
        document.getElementById("descriptor1").value = localDescription;
    });

    return false;
}

function acceptOffer(){
    const remoteDescription = document.getElementById("descriptor1").value;

    const WebRTCConnection = new RTCPeerConnection({
        iceServers: [
            {
                urls: 'stun:13.48.195.80:3478',
            },
        ],
    });

    let chatChannel;
    WebRTCConnection.ondatachannel = (event) => {
        if (event.channel.label == 'chat') {
            chatChannel = event.channel;
            chatChannel.onmessage = (event) => console.log('onmessage:', event.data);
            chatChannel.onopen = () => {
                console.log('onopen');
                sessionStorage.setItem("someVarKey", WebRTCConnection);
                window.location.href = "chatGUI.html";
            }
            chatChannel.onclose = () => console.log('onclose');
            sessionStorage.setItem("Connection", WebRTCConnection)
        }
    };

    WebRTCConnection.onicecandidate = (event) => {
        if (event.candidate)
            console.log('localDescription:', JSON.stringify(WebRTCConnection.localDescription));
    };

    WebRTCConnection.setRemoteDescription(remoteDescription);

    WebRTCConnection.createAnswer().then((localDescription) => {
        WebRTCConnection.setLocalDescription(localDescription);
        document.getElementById("descriptor2").value = localDescription;
    });

    window.sessionStorage.setItem("someVarKey", WebRTCConnection);

    return false;
}

function openConnecion(){
    const remoteDescription = document.getElementById("descriptor2").value;
    WebRTCConnection.setRemoteDescription(remoteDescription);
}