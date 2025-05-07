package com.example.chat_app.service;

import java.util.Optional;

import com.example.chat_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService{

    @Autowired
    private UserRepository userRepository;

    public com.example.chat_app.entities.User save(com.example.chat_app.entities.User user){
        return userRepository.save(user); // controllo se l'username è disponibile o meno
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<com.example.chat_app.entities.User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            var userObj = user.get();
            return User.builder()
                    .username(userObj.getUsername())
                    .password(userObj.getPassword())
                    .build();
        }else{
            throw new UsernameNotFoundException(username);
        }
    }

    public Optional<com.example.chat_app.entities.User> findByUsername(String username){
        Optional<com.example.chat_app.entities.User> user = userRepository.findByUsername(username);
        return user;
    }

    public Optional<com.example.chat_app.entities.User> findById(Long id){
        return userRepository.findById(id);
    }

}
