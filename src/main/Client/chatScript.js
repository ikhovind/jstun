
document.getElementById("sendButton").addEventListener("click", ()=>sendMessage());

function sendMessage(){
    let message = document.getElementById("inputText").value;
    document.getElementById("inputText").value = "";
    document.getElementById("chatDiv").innerHTML += ("<p style='text-align: right'>" + message + "</p>");

    //do something with webrtc here
}

function recieveMessage(message){
    //called from eventListener on webRTC :)
    document.getElementById("chatDiv").innerHTML += ("<p style='text-align: left'>" + message + "</p>");

}

