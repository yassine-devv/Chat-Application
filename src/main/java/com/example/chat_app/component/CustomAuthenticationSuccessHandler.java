package com.example.chat_app.component;

import com.example.chat_app.entities.User;
import com.example.chat_app.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // Recupera il nome utente
        String username = authentication.getName();

        Optional<User> user = userRepository.findByUsername(username);

        if(user.isPresent()){
            var userObj = user.get();
            request.getSession().setAttribute("id", userObj.getId());
        }
        // Salva in sessione
        request.getSession().setAttribute("username", username);

        // Reindirizza dopo il login (puoi personalizzare)
        response.sendRedirect("/");
    }
}
