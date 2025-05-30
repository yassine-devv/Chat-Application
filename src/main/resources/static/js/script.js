document.addEventListener("DOMContentLoaded", connect);

var username;
var usersConnected = [];

function connect(event) {
    username = document.getElementById("username-field").innerText;

    console.log(username);

    if(username != null) {
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

    openTab("UsersConnected")

    //todo: caricare tutte le chat, nella sezione left e right i connessi e gli inviti
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

        setHtmlForUserConnected();
    }

    if(message.typeMessage === "CHAT"){
        console.log(message.content);

        if(document.getElementById("area-messages")){
            let areaMessages = document.getElementById("area-messages");

            areaMessages.innerHTML += `<div class="consumer-message"><span>${message.content}</span><br></div>`;
            areaMessages.scrollTo(0, areaMessages.scrollHeight);
        }


        /*
        getUsernameProducerById(message.producerId)
            .then(data => {

            const usernameProducer = data[0];

            if(document.getElementById("area-messages") && document.getElementById("consumer").innerText == usernameProducer){
                let areaMessages = document.getElementById("area-messages");

                areaMessages.innerHTML += `<div class="consumer-message"><span>${message.content}</span><br></div>`;
                areaMessages.scrollTo(0, areaMessages.scrollHeight);
            }else{
                let generatedAvatar = generaAvatar(usernameProducer);

                document.querySelector(".alert-heading").innerHTML = `<div class="image-profile" style="margin-right: 2%; background-color: #${generatedAvatar.backgroundColor};"><span>${generatedAvatar.firstChar}</span></div>`;
                document.querySelector(".alert-heading").innerHTML += `<span>${usernameProducer}</span>`;
                document.getElementById("message-content").innerHTML = message.content;
                /*document.querySelector(".btn-open-chat-modal").id = message.chatId;
                document.querySelector(".modal").style.display = "block";

                setTimeout(hideModal, 3000);
            }
        });*/

        /*
        if(document.getElementById("area-messages") && document.getElementById("consumer").innerText == message){
            let areaMessages = document.getElementById("area-messages");

            areaMessages.innerHTML += `<div class="consumer-message"><span>${message.content}</span><br></div>`;
            areaMessages.scrollTo(0, areaMessages.scrollHeight);
        }else{
            //fare in modo di prendere username dall'id del producer dal messaggio, e fare l'alert
            getUsernameProducerById(message.producerId)
                .then(data => {

                const usernameProducer = data[0];

                let generatedAvatar = generaAvatar(usernameProducer);

                document.querySelector(".alert-heading").innerHTML = `<div class="image-profile" style="margin-right: 2%; background-color: #${generatedAvatar.backgroundColor};"><span>${generatedAvatar.firstChar}</span></div>`;
                document.querySelector(".alert-heading").innerHTML += `<span>${usernameProducer}</span>`;
                document.getElementById("message-content").innerHTML = message.content;
                document.querySelector(".btn-open-chat-modal").id = message.chatId;
                document.querySelector(".modal").style.display = "block";

                setTimeout(hideModal, 3000);
            });
        }*/


        /*
        if(document.getElementById("area-messages")){
            let areaMessages = document.getElementById("area-messages");

            areaMessages.innerHTML += `<div class="consumer-message"><span>${message.content}</span><br></div>`;
            areaMessages.scrollTo(0, areaMessages.scrollHeight);

        }/*else{

            //fare in modo di prendere username dall'id del producer dal messaggio, e fare l'alert
            getUsernameProducerById(message.producerId)
                .then(data => {

                const usernameProducer = data[0];

                let generatedAvatar = generaAvatar(usernameProducer);

                document.querySelector(".title-modal").innerHTML = `<div class="image-profile" style="margin-right: 2%; background-color: #${generatedAvatar.backgroundColor};"><span>${generatedAvatar.firstChar}</span></div>`;
                document.querySelector(".title-modal").innerHTML += `<span>${usernameProducer}</span>`;
                document.getElementById("modal-body").innerHTML = message.content;
                document.querySelector(".btn-open-chat-modal").id = message.chatId;
                document.querySelector(".modal").style.display = "block";

                setTimeout(hideModal, 3000);
            });

        }*/
    }
}

function hideModal(){
    document.querySelector(".alert").style.display = "none";
}


function onError(error) {
    console.log("errore, riprova");
}

function openTab(tabName) {
    var i, tabcontent, tablinks;

    if(tabName=="UsersConnected"){
        setHtmlForUserConnected();
    }

    if(tabName=="Invitations"){
        console.log("inviti");
    }

    tabcontent = document.getElementsByClassName("tabcontent");
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = "none";
    }

    tablinks = document.getElementsByClassName("tablinks");
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace(" active", "");
    }

    document.getElementById(tabName).style.display = "block";
    if(tabName=="UsersConnected"){
        document.getElementsByClassName("users-connected")[0].classList.add("active");
    }else{
        document.getElementsByClassName("invitations")[0].classList.add("active");
    }
}

function setHtmlForUserConnected(){
    let arrColors = ['00CCCC', '0066CC', '80FF00', '00FF00', 'FF90CC', 'CC6600'];

    let html = ``;

    if(usersConnected.length===1 && usersConnected[0]===username){
        document.querySelector(".list-users-connected").style.textAlign = "center";
        document.querySelector(".list-users-connected").innerHTML = `<span id="span-no-user-connected" >Nessun utente è connesso</span>`;
    }

    if(usersConnected.length > 1){
        let listUsersConnected = document.querySelector(".list-users-connected");

        if(listUsersConnected.style.textAlign == "center"){
            listUsersConnected.style.removeProperty("text-align");
        }
        //document.querySelector(".list-users-connected").style.textAlign = "center";
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



            const labelChats = document.getElementsByClassName("label-chat");
            for (let i = 0; i < labelChats.length; i++) {
                labelChats[i].classList.remove("active");
            }

            document.getElementById(idChat).classList.add("active");

            console.log(document.getElementsByClassName("label-chat"));
            /*
            if(document.querySelector(".header-username #consumer")){
                let generatedAvatar = generaAvatar(document.getElementById("consumer").innerText);

                var htmlAvatar = `
                    <turbo-stream action="prepend" target="messages">
                        <template>
                          <div id="consumer">
                                <div class="image-profile" style="background-color: #${generatedAvatar.backgroundColor};"><span>${generatedAvatar.firstChar}</span></div>
                          </div>
                        </template>
                    </turbo-stream>
                `;

                Turbo.renderStreamMessage(htmlAvatar);
            }*/

        })
            .catch(err => console.log(err));
    }


}

function generaAvatar(usernameProducer){
    let arrColors = ['00CCCC', '0066CC', '80FF00', '00FF00', 'FF90CC', 'CC6600'];

    let firstChar = usernameProducer.charAt(0).toUpperCase();
    let backgroundColor = arrColors[Math.floor(Math.random() * 6)];

    return {"firstChar": firstChar, "backgroundColor": backgroundColor};
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
