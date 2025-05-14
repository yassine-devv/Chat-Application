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

    getAllChats().then(html => {
        Turbo.renderStreamMessage(html);
    });

    getCountPendingInvitations().then(html => {
        Turbo.renderStreamMessage(html);
    });

}

function onMessageReceive(payload){
    var message = JSON.parse(payload.body);

    if(message.typeMessage === "JOIN" || message.typeMessage === "LEFT"){
        //todo: passare con il nome degli user connessi anche i loro id
        getUsersConnected().then(html => {
            Turbo.renderStreamMessage(html);
        })
    }

    if(message.typeMessage === "INVITATION"){
        console.log(message.content);

        getCountPendingInvitations().then(html => {
            Turbo.renderStreamMessage(html);
        });

        getAllInvitations().then(html => {
            Turbo.renderStreamMessage(html);
        })

        let alert = document.getElementById("invitation-notification");
        let lastChild = alert.lastElementChild;

        while (lastChild) {
            alert.removeChild(lastChild);
            lastChild = alert.lastElementChild;
        }

        alert.innerHTML = `<span>${message.content}</span>`;
        alert.innerHTML += `<div class="sec-buttons-action-invitation" id="${message.chatId}">
                            <button type="button" class="btn-action-invite" onclick="hideAlert(); actionOnInvitation('${message.chatId}', true, '${message.producerId}')">
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="green" class="bi bi-check-lg" viewBox="0 0 16 16">
                                  <path d="M12.736 3.97a.733.733 0 0 1 1.047 0c.286.289.29.756.01 1.05L7.88 12.01a.733.733 0 0 1-1.065.02L3.217 8.384a.757.757 0 0 1 0-1.06.733.733 0 0 1 1.047 0l3.052 3.093 5.4-6.425z"/>
                                </svg>
                            </button>
                            <button type="button" class="btn-action-invite" onclick="hideAlert(); actionOnInvitation('${message.chatId}', false, '${message.producerId}')">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="red" class="bi bi-x-lg" viewBox="0 0 16 16">
                                  <path d="M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8z"/>
                                </svg>
                            </button>
                        </div>`
        alert.style.display = "flex";

        setTimeout(hideAlert, 4000);

        /*
        mandare modal box della notifica dell'invito
        incrementare il count degli inviti
        */
    }

    if(message.typeMessage === "ACCEPTED"){
        console.log(message.content);

        getAllChats().then(html => {
            Turbo.renderStreamMessage(html);
        });

        let alert = document.getElementById("invitation-notification");

        let lastChild = alert.lastElementChild;

        while (lastChild) {
            alert.removeChild(lastChild);
            lastChild = alert.lastElementChild;
        }

        alert.innerHTML = `<span>${message.content}</span>`;
        alert.style.display = "block";

        setTimeout(hideAlert, 4000);

        /*
        mandare modal box della notifica dell'invito
        incrementare il count degli inviti
        */
    }

    if(message.typeMessage === "CHAT"){
        console.log(message.content);

        getUsernameProducerById(message.producerId).then(data => {
            const usernameProducer = data[0];

            if(document.getElementById("area-messages")){
                if(usernameProducer==document.getElementById('consumer').innerText){
                    let areaMessages = document.getElementById("area-messages");

                    areaMessages.innerHTML += `<div class="consumer-message"><span>${message.content}</span><br></div>`;
                    areaMessages.scrollTo(0, areaMessages.scrollHeight);
                }else{
                    showDialog(usernameProducer, message.content)

                    setTimeout(hideDialog, 3500);
                }
            }else{
                showDialog(usernameProducer, message.content)

                setTimeout(hideDialog, 3500);
            }
        })

    }
}

function showDialog(producer, content){
    let generatedAvatar = generaAvatar(producer);
    //notifica messaggio
    document.querySelector(".notification-header").style.display = "flex";
    document.querySelector(".notification-header").innerHTML = `<div class="image-notification-profile" style="background-color: #${generatedAvatar.backgroundColor};"><span>${generatedAvatar.firstChar}</span></div>`;
    document.querySelector(".notification-header").innerHTML += `<span>${producer}</span>`;
    document.querySelector(".notification-content").innerHTML = `<p>${content}</p>`;

    document.querySelector("dialog").style.opacity = "1";
    document.querySelector("dialog").style.transform = "translate(-50%, 0)";
    document.querySelector("dialog").style.pointerEvents = "auto";

    document.querySelector("dialog").showModal();
}


function hideDialog(){
    document.querySelector("dialog").close();
}

function hideAlert(){
    document.getElementById("invitation-notification").style.display = "none";
}

async function getAllChats(){
    try{
        const response = await fetch('/api/getAllChats', {
            method: 'GET',
            headers: {
                'Accept': 'text/vnd.turbo-stream.html'
            },
        });

        const data = await response.text();
        return data;
    } catch (error) {
        console.error('Errore nella richiesta:', error);
        throw error;
    }
}

async function getUsersConnected(){
    try{
        const response = await fetch('/api/getUsersConnected', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
        });

        const data = await response.text();
        return data;
    } catch (error) {
        console.error('Errore nella richiesta:', error);
        throw error;
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
        getUsersConnected().then(html => {
            Turbo.renderStreamMessage(html);
        })
    }

    if(tabName=="Invitations"){
        getAllInvitations().then(html => {
            Turbo.renderStreamMessage(html);
        })
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

function sendInvitation(idUserInvitee){
    fetch('/api/sendInvitation', {
        method: "POST",
        headers: {
            "Content-Type": "text/vnd.turbo-stream.html"
        },

        body: JSON.stringify(idUserInvitee)
    })
        .then(response => response.text())
        .then(html => {
            Turbo.renderStreamMessage(html);
    })
        .catch(err => console.log(err));

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

async function getUsersWithChat(){
    try{
        const response = await fetch('/api/getUsersWithChat', {
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

function actionOnInvitation(idInvitation, result, idCurrentUser){
    fetch('/api/actionOnInvitation', {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ id_invitation: idInvitation, result: result, id_current_user: idCurrentUser })
    })
        .then(response => response.text())
        .then(html => {
        if (html.trim().startsWith("<turbo-stream")) {
            Turbo.renderStreamMessage(html);
        }

        return getAllChats();
    })
        .then(html => {
        Turbo.renderStreamMessage(html);
    })
        .catch(err => console.error(err));
}

async function getCountPendingInvitations(){
    try{
        const response = await fetch('/api/getCountPendingInvitations', {
            method: 'GET',
            headers: {
                'Accept': 'text/vnd.turbo-stream.html'
            },
        });

        const data = await response.text();
        return data;
    } catch (error) {
        console.error('Errore nella richiesta:', error);
        throw error;
    }

}

async function getAllInvitations(){
    try{
        const response = await fetch('/api/getAllInvitations', {
            method: 'GET',
            headers: {
                'Accept': 'text/vnd.turbo-stream.html'
            },
        });

        const data = await response.text();
        return data;
    } catch (error) {
        console.error('Errore nella richiesta:', error);
        throw error;
    }
}
