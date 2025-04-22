package com.example.chat_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String showAllBooks() {
        return "index";
    }

    @PostMapping("/{username}")
    public ResponseEntity<String> chat(@PathVariable String username){
        return ResponseEntity.ok().header("Content-Type", "text/vnd.turbo-stream.html")
                .body("""
                    <turbo-stream action="remove" target="usernameForm"></turbo-stream>
                    <turbo-stream action="remove" targets=".area-user-connected"></turbo-stream>
                    <turbo-stream action='update' target="area-chat">
                        <template>
                            <span id="consumer"><b>%s</b></span></br></br>
                            <div id="area-messages"></div>
                            <form id="sendMessageForm" name="sendMessageForm">
                                <input type="text" id="message" placeholder="Scrivi un messaggio..." autocomplete="off" class="form-control"/>
                                <button type="submit" class="accent message-submit">Invia</button>
                            </form>
                            <script>
                                document.getElementById("sendMessageForm").onsubmit = function(e) {
                                    e.preventDefault();
                                    const message = document.getElementById("message").value.trim();
                                    const consumer = document.getElementById("consumer").textContent;
                                    if (message && window.stompClient) {
                                        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
                                            producer: window.username,
                                            consumer: consumer,
                                            content: message,
                                            typeMessage: "CHAT"
                                        }));
                                        document.getElementById("area-messages").innerHTML += `<span>Tu: ${message}</span></br>`
                                    }
                                    this.reset();
                                };
                            </script>
                        </template>
                    </turbo-stream>
                """.formatted(username));
    }
}
