let htmlTest = "<!DOCTYPE html>\n" +
    "<html lang=\"en\">\n" +
    "<head>\n" +
    "    <meta charset=\"UTF-8\">\n" +
    "    <title>Title</title>\n" +
    "</head>\n" +
    "<body>\n" +
    "    <div id=\"chat\" >\n" +
    "        <div class=\"container\">\n" +
    "            <h1>Chat with your connection</h1>\n" +
    "            <div id=\"chatDiv\" class=\"chatBox\">\n" +
    "\n" +
    "            </div>\n" +
    "            <div class=\"chatFooter\">\n" +
    "                <textarea id=\"inputText\" name=\"message\" placeholder=\"Write something..\"></textarea>\n" +
    "                <input id=\"sendButton\" onclick='sendMessage()' \" type=\"submit\"/>\n" +
    "            </div>\n" +
    "        </div>\n" +
    "        <script src=\"../WebScript.js\"></script>\n" +
    "    </div>\n" +
    "</body>\n" +
    "<style>\n" +
    "    /* Style inputs with type=\"text\", select elements and textareas */\n" +
    "    .chatBox {\n" +
    "        overflow: auto;\n" +
    "        width: 100%; /* Full width */\n" +
    "        height: 30vw;\n" +
    "        padding: 12px; /* Some padding */\n" +
    "        border: 1px solid #ccc; /* Gray border */\n" +
    "        border-radius: 4px; /* Rounded borders */\n" +
    "        box-sizing: border-box; /* Make sure that padding and width stays in place */\n" +
    "        margin-top: 6px; /* Add a top margin */\n" +
    "        margin-bottom: 16px; /* Bottom margin */\n" +
    "        resize: vertical; /* Allow the user to vertically resize the textarea (not horizontally) */\n" +
    "        background-color: #ffffff;\n" +
    "    }\n" +
    "\n" +
    "    #inputText{\n" +
    "        padding: 12px; /* Some padding */\n" +
    "        border: 1px solid #ccc; /* Gray border */\n" +
    "        border-radius: 4px; /* Rounded borders */\n" +
    "        box-sizing: border-box; /* Make sure that padding and width stays in place */\n" +
    "        margin-top: 6px; /* Add a top margin */\n" +
    "        margin-bottom: 16px; /* Bottom margin */\n" +
    "        resize: vertical;\n" +
    "        width: 100%\n" +
    "    }\n" +
    "    #sendButton{\n" +
    "        width: 20%;\n" +
    "        background-color: #4CAF50;\n" +
    "        color: white;\n" +
    "        padding: 12px 12px;\n" +
    "        border: none;\n" +
    "        border-radius: 4px;\n" +
    "        cursor: pointer;\n" +
    "    }\n" +
    "\n" +
    "    /* When moving the mouse over the submit button, add a darker green color */\n" +
    "    input[type=submit]:hover {\n" +
    "        background-color: #45a049;\n" +
    "    }\n" +
    "\n" +
    "    /* Add a background color and some padding around the form */\n" +
    "    .container {\n" +
    "        border-radius: 5px;\n" +
    "        padding: 20px;\n" +
    "    }\n" +
    "    body{\n" +
    "        background-color: #f2f2f2;\n" +
    "    }\n" +
    "</style>\n" +
    "</html>"

const WebRTCConnection = new RTCPeerConnection({
    iceServers: [
        {
            urls: 'stun:13.48.195.80:3478',
        },
    ],
});
chatChannel = WebRTCConnection.createDataChannel('chat');

    document.getElementById("leftInputBox").readOnly = true;

    if (localStorage.getItem("joinGenerate") === "join") {
        document.getElementById("chatButton").addEventListener("click", () => acceptOffer());
    } else {
        createOffer();
        document.getElementById("chatButton").addEventListener("click", () => openConnecion());
    }


    function createOffer() {
        chatChannel.onmessage = (event) => recieveMessage(event.data);
        chatChannel.onopen = () => {
            //document.location = "chatWindow.html";
            document.documentElement.innerHTML = htmlTest;
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
                chatChannel.onmessage = (event) => recieveMessage(event.data);
                chatChannel.onopen = () => {
                    //document.location = "chatWindow.html";
                    document.documentElement.innerHTML = htmlTest;

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

function sendMessage(){
    console.log("sent");
    let message = document.getElementById("inputText").value;
    document.getElementById("inputText").value = "";
    document.getElementById("chatDiv").innerHTML += ("<p style='text-align: right'>" + message + "</p>");
    chatChannel.send("<p style='text-align: right'>" + message + "</p>")
    //do something with webrtc here
}

function recieveMessage(message){
    //called from eventListener on webRTC :)
    document.getElementById("chatDiv").innerHTML += ("<p style='text-align: left'>" + message + "</p>");
    console.log(message)
}



