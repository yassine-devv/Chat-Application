document.addEventListener("DOMContentLoaded", connect);

var username;
var usersConnected = [];

function connect(event) {
    username = document.getElementById("username-field").innerText;

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

    stompClient.send("/app/chat.addUser", {}, JSON.stringify({producer: username, typeMessage: 'JOIN', content: 'JOIN', consumer: "CLIENT"}))

    openTab('UsersConnected');
}

function onMessageReceive(payload){
    var message = JSON.parse(payload.body);
    console.log(message.content);

    if(message.typeMessage === "JOIN" || message.typeMessage === "LEFT"){
        //todo: passare con il nome degli user connessi anche i loro id

        let arrUserConnected = message.content.split(/[\[\],]+/).filter(Boolean);

        for(let i=0; i<arrUserConnected.length; i++){
            arrUserConnected[i] = arrUserConnected[i].trim();
        }

        usersConnected = [];
        usersConnected = [].concat(arrUserConnected);

        setHtmlForUserConnected("UsersConnected");
    }

    if(message.typeMessage === "CHAT"){
        if(document.getElementById("area-messages")){
            let areaMessages = document.getElementById("area-messages");

            areaMessages.innerHTML += `<div class="consumer-message"><span>${message.content}</span><br></div>`;
            areaMessages.scrollTo(0, areaMessages.scrollHeight);

        }else{
            //fare in modo di prendere username dall'id del producer dal messaggio, e fare l'alert
            getUsernameProducerById(message.producerId)
                .then(data => {
                    /*
                    modificare i campi del modal
                    settare l'id della chat del pulsante del modal prendendolo attraverso la classe
                    visualizzare il modal
                    */

                    const usernameProducer = data[0];

                    let generatedAvatar = generaAvatar(usernameProducer);

                    document.querySelector(".title-modal").innerHTML = `<div class="image-profile" style="margin-right: 2%; background-color: #${generatedAvatar.backgroundColor};"><span>${generatedAvatar.firstChar}</span></div>`;
                    document.querySelector(".title-modal").innerHTML += `<span>${usernameProducer}</span>`;
                    document.getElementById("modal-body").innerHTML = message.content;
                    document.querySelector(".btn-open-chat-modal").id = message.chatId;
                    document.querySelector(".modal").style.display = "block";

                    setTimeout(hideModal, 3000);
                });

        }
    }
}

function hideModal(){
    document.querySelector(".modal").style.display = "none";
}

function onError(error) {
    console.log("errore, riprova");
}

function openTab(tabName) {
    var i, tabcontent, tablinks;

    let tabNames = ['UsersConnected', 'Chat', 'Inviti']

    switch (tabName){
        case tabNames[0]:
            setHtmlForUserConnected(tabName);
            break;
        case tabNames[1]:
            fetch('/api/getAllChats', {
                method: "GET",
                headers: {
                    "Content-Type": "text/vnd.turbo-stream.html"
                }
            })
                .then(response => response.text())
                .then(html => {
                    Turbo.renderStreamMessage(html);
                })
                .catch(err => console.log(err));
            break;
        case tabNames[2]:
            console.log(tabName);
            break;
    }

    tabcontent = document.getElementsByClassName("tabcontent");
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = "none";
    }

    tablinks = document.getElementsByClassName("tablinks");
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace(" active", "");
    }

    if(tabName==="Chat"){
        document.getElementById(tabName).style.display = "flex";
    }else{
        document.getElementById(tabName).style.display = "block";
    }

    /*if(tabName !== "Chat"){
        let areaChat = document.getElementById("area-chat");
        let headerUsername = document.querySelector(".header-username");
        let areaMessages = document.querySelector(".header-username");
        if (parentDiv.contains(childDiv)) {
            alert("yes");
        }
    }*/

    document.getElementsByClassName("tabcontent")[0].classList.add("active");
}

/*
funzione che si occupa di generare l'html avendo un array con tutti gli utenti connessi
*/
function setHtmlForUserConnected(tabName){
    let arrColors = ['00CCCC', '0066CC', '80FF00', '00FF00', 'FF90CC', 'CC6600'];

    let html = ``;

    if(usersConnected.length===1 && usersConnected[0]===username){
        document.querySelector(".list-users-connected").innerHTML = `<span>Nessun utente è connesso</span>`;
    }
    if(usersConnected.length > 1){
        for(let i=0; i<usersConnected.length; i++){
            if(usersConnected[i] !== username){
                let generatedAvatar = generaAvatar(usersConnected[i]);
                html += `
                    <div class="single-user">
                        <div class="image-profile" style="background-color: #${generatedAvatar.backgroundColor};"><span>${generatedAvatar.firstChar}</span></div>
                        <span>${usersConnected[i]}</span>
                    </div>
                `;
            }
        }
        document.querySelector(".list-users-connected").innerHTML = html;
    }
}

//nel momento che clicchi per aprire la chat manda una chiamata api che torna tutti i messaggi in quella chat
function openChat(evt){
    var idChat = evt.target.id;

    if(idChat != null){
        fetch('/api/'+idChat, {
            method: "POST",
            headers: {
                "Content-Type": "text/vnd.turbo-stream.html"
            }
        })
            .then(response => response.text())
            .then(html => {
            Turbo.renderStreamMessage(html);
        })
            .catch(err => console.log(err));
    }
}

//funziona che fa una chiamata api che passando l'id di un utente torna il suo username
async function getUsernameProducerById(producerId){
    try{
        const response = await fetch('/api/getUsernameById/'+producerId, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
        });

        const data = await response.json();
        return data;
    } catch (error) {
        console.error('Errore nella richiesta:', error);
        throw error;
    }
}

function generaAvatar(usernameProducer){
    let arrColors = ['00CCCC', '0066CC', '80FF00', '00FF00', 'FF90CC', 'CC6600'];

    let firstChar = usernameProducer.charAt(0).toUpperCase();
    let backgroundColor = arrColors[Math.floor(Math.random() * 6)];

    return {"firstChar": firstChar, "backgroundColor": backgroundColor};
}

function redirectToChat(evt){
    document.querySelector(".modal").style.display = "none";
    openTab("Chat");
    openChat(evt);
}
