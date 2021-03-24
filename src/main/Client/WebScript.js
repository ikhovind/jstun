let htmlTest = "<!DOCTYPE html>\n" +
    "<html lang=\"en\"><head>\n" +
    "<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">\n" +
    "    <meta charset=\"UTF-8\">\n" +
    "    <title>Title</title>\n" +
    "</head>\n" +
    "<body>\n" +
    "    <div id=\"chat\">\n" +
    "        <div class=\"container\">\n" +
    "            <h1 id=\"header\">Chat with your connection</h1>\n" +
    "            <div id=\"chatDiv\" class=\"chatBox\">\n" +
    "\n" +
    "            </div>\n" +
    "            <div class=\"chatFooter\">\n" +
    "                <textarea id=\"inputText\" name=\"message\" placeholder=\"Write something..\"></textarea>\n" +
    "                <input id=\"sendButton\" onclick=\"sendMessage()\" type=\"submit\" value=\"Send\">\n" +
    "            </div>\n" +
    "        </div>\n" +
    "        <script src=\"../WebScript.js\"></script>\n" +
    "    </div>\n" +
    "\n" +
    "<style>\n" +
    "    /* Style inputs with type=\"text\", select elements and textareas */\n" +
    "    .chatBox {\n" +
    "        overflow: auto;\n" +
    "        word-wrap: break-word;\n" +
    "        width: 100%; /* Full width */\n" +
    "        height: 30vw;\n" +
    "        padding: 12px; /* Some padding */\n" +
    "        border: 1px solid #ccc; /* Gray border */\n" +
    "        border-radius: 4px; /* Rounded borders */\n" +
    "        box-sizing: border-box; /* Make sure that padding and width stays in place */\n" +
    "        margin-top: 6px; /* Add a top margin */\n" +
    "        margin-bottom: 16px; /* Bottom margin */\n" +
    "        resize: vertical; /* Allow the user to vertically resize the textarea (not horizontally) */\n" +
    "        background-color: #40444b;\n" +
    "        color: #ffffff;\n" +
    "    }\n" +
    "\n" +
    "    #header {\n" +
    "        font-family: \"Trebuchet MS\", sans-serif;\n" +
    "        color: #f2f2f2;\n" +
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
    "        width: 100%;\n" +
    "        background-color: #40444b;\n" +
    "        color: #ffffff;\n" +
    "    }\n" +
    "\n" +
    "    #sendButton{\n" +
    "        width: 20%;\n" +
    "        background-color: #7289da;\n" +
    "        color: white;\n" +
    "        padding: 12px 12px;\n" +
    "        border: none;\n" +
    "        border-radius: 4px;\n" +
    "        cursor: pointer;\n" +
    "    }\n" +
    "\n" +
    "    #sendButton:hover {\n" +
    "        background-color: #99a9e2;\n" +
    "    }\n" +
    "\n" +
    "    /* When moving the mouse over the submit button, add a darker green color */\n" +
    "    input[type=submit]:hover {\n" +
    "        background-color: #99a9e2;\n" +
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
    "    html, body {\n" +
    "        max-height: 1070px;\n" +
    "        background-color: #2c2f33;\n" +
    "    }\n" +
    "   .message {\n" +
    "        width: 50%;\n" +
    "        margin: 0;\n" +
    "    }\n" +
    "\n" +
    "   .sent {\n" +
    "        margin-left: 50%;\n" +
    "    }\n" +
    "   hr {\n" +
    "        margin: 3px 0 0 0;\n" +
    "        padding: 0;\n" +
    "        border-top: 1px solid #23272A;\n" +
    "        border-bottom: 0;\n" +
    "    }\n" +
    "   .pfp {\n" +
    "        margin-top: 10px;\n" +
    "        border-radius: 50%;\n" +
    "        border: 1px solid #61C9CE;\n" +
    "    }\n" +
    "   .talr {\n" +
    "        text-align: right;\n" +
    "    }\n" +
    "</style>\n" +
    "</body></html>"

let pfp_src = "https://i.imgur.com/vcTN90E.png";
let remote_pfp = "https://i.imgur.com/Lij7ztF.png";
let changed = false;

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
        if(!(document.getElementById("pfp_src").value=="")){
            pfp_src = document.getElementById("pfp_src").value;
            chatChannel.send("$change " + pfp_src)
        }else{
            chatChannel.send("$change " + remote_pfp)
        }
        document.documentElement.innerHTML = htmlTest;
    }
    chatChannel.onclose = () => console.log('onclose');

    WebRTCConnection.onicecandidate = (event) => {
        if (event.candidate)
            console.log(event.candidate)
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
                if(!(document.getElementById("pfp_src").value=="")){
                    pfp_src = document.getElementById("pfp_src").value;
                    chatChannel.send("$change " + pfp_src)
                }else{
                    chatChannel.send("$change " + remote_pfp)
                }
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

function sendMessage() {
    console.log("sent");
    let message = document.getElementById("inputText").value;
    document.getElementById("inputText").value = "";
    document.getElementById("chatDiv").innerHTML += (
        "<div class='talr'>" +
        "   <img class='sent pfp' src=\"" + pfp_src + "\" alt='' width='32px'>" +
        "   <p class='message sent'>" +
            message.replace(/<\/?[^>]+(>|$)/g, "") + "</p>" +
        "   <hr>" +
        "</div>"
    );
    chatChannel.send(message)
    //do something with webrtc here
}

function recieveMessage(message) {
    if(!changed){
        console.log(message);
        if(message == "$change"){
            changed = true;
            return;
        }
        remote_pfp = message.substring(7);
        changed = true;
        return;
    }
    //called from eventListener on webRTC :)
    document.getElementById("chatDiv").innerHTML += ("<img class='pfp' src='" + remote_pfp + "' width='32px'><p class='message'>" + message.replace(/<\/?[^>]+(>|$)/g, "") + " <hr> ");
    console.log(message)
}

function toggleSettings(){
    var x = document.getElementById("pfp_src")
    if (x.style.display === 'none'){
        x.style.display = "block";
    }else{
        x.style.display = "none";
    }
}

