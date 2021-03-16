const WebRTCConnection = new RTCPeerConnection({
    iceServers: [
        {
            urls: 'stun:jstun-1615905577996.azurewebsites.net:4445',
        },
    ],
});

const chatChannel = WebRTCConnection.createDataChannel('chat');
chatChannel.onmessage = (event) => console.log('onmessage:', event.data);
chatChannel.onopen = () => console.log('onopen');
chatChannel.onclose = () => console.log('onclose');

WebRTCConnection.onicecandidate = (event) => {
    if (event.candidate)
        console.log('localDescription:', JSON.stringify(WebRTCConnection.localDescription));
};

WebRTCConnection.createOffer().then((localDescription) => {
    WebRTCConnection.setLocalDescription(localDescription);
});






const remoteDescription = {}/* Add a localDescription from client A here */;

const WebRTCConnection = new RTCPeerConnection({
    iceServers: [
        {
            urls: 'stun:jstun-1615905577996.azurewebsites.net:4445',
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
        console.log('localDescription:', JSON.stringify(WebRTCConnection.localDescription));
};

WebRTCConnection.setRemoteDescription(remoteDescription);

WebRTCConnection.createAnswer().then((localDescription) => {
    WebRTCConnection.setLocalDescription(localDescription);
});






const remoteDescription = {}/* Add localDescription from client B here */;
WebRTCConnection.setRemoteDescription(remoteDescription);