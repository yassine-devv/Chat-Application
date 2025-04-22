var usernameForm = document.querySelector('#usernameForm');
var inputUsername = document.querySelector("#name");
var messageForm = document.querySelector('#sendMessageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#area-messages');
var areaUserConnected = document.querySelector('.area-user-connected');

var stompClient = null;
var username = null;

function connect(event) {
    username = document.querySelector('#name').value.trim();

    console.log(username);

    if(username) {
        var socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        stompClient.connect({}, onConnected, onError);
    }
    event.preventDefault();
}

function onConnected() {
    stompClient.subscribe('/topic/public', onMessageReceive);
    stompClient.subscribe('/topic/user.'+username, onMessageReceive);


    inputUsername.value = "";
    stompClient.send("/app/chat.addUser", {}, JSON.stringify({producer: username, content: 'JOIN', consumer: "CLIENT"}))
}

function onMessageReceive(payload){
    var message = JSON.parse(payload.body);
    console.log(message.content);

    if(message.typeMessage === "JOIN" || message.typeMessage === "LEFT"){
        let arrUserConnected = message.content.split(/[\[\],]+/).filter(Boolean);

        for(let i=0; i<arrUserConnected.length; i++){
            arrUserConnected[i] = arrUserConnected[i].trim();
        }
        let html = ``;

        if(arrUserConnected.length===1 && arrUserConnected[0]===username){
            html = `<span>Nessun utente è connesso</span>`;
        }

        if(arrUserConnected.length > 1){
            html = `<span><b>Utenti Connessi</b></span>`;
            html += `<ul>`;

            for(let i=0; i<arrUserConnected.length; i++){
                if(arrUserConnected[i] !== username){
                    html += `
                            <li>
                                <form method="post" action="/${arrUserConnected[i]}" style="display:inline;">
                                    <button type="submit" class="card-link">${arrUserConnected[i]}</button>
                                </form>
                            </li>`;
                }
            }

            html += `</ul>`;
        }

        areaUserConnected.innerHTML = html;
    }

    if(message.typeMessage === "CHAT"){
        console.log(message.content)
        console.log(`<span>${message.producer}: ${message.content}</span></br>`);
        document.getElementById("area-messages").innerHTML += `<span>${message.producer}: ${message.content}</span></br>`;
    }
}

function onError(error) {
    console.log("errore, riprova");
}

function sendMessage(event) {
    event.preventDefault();
    var messageContent = messageInput.value.trim();
    if(messageContent && stompClient) {
        var chatMessage = {
            producer: username,
            content: messageInput.value,
            consumer: document.getElementById("consumer").innerHTML,
            typeMessage: "CHAT"
        };
        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
}

usernameForm.addEventListener('submit', connect, true)
if(messageForm){
    messageForm.addEventListener('submit', sendMessage, true)
}
