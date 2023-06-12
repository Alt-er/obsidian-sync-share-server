package com.alter.obsyncshare.controller;

import com.alter.obsyncshare.dto.LoginDTO;
import com.alter.obsyncshare.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Random;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "app://obsidian.md")
public class UserController {


    private static final Logger logger =
            LoggerFactory.getLogger(UserController.class);


    @Autowired
    private UserSession userSession;


    @PostMapping("/login")
    public String login(
            @RequestBody LoginDTO loginDTO
    ) throws IOException {

        logger.debug("用户:{}正在尝试登录", loginDTO.getUsername());

        boolean validUsernameAndPassword = userSession.isValidUsernameAndPassword(loginDTO.getUsername(), loginDTO.getPassword());
        if (validUsernameAndPassword) {
            logger.debug("用户:{}登录成功", loginDTO.getUsername());
            return userSession.createToken(loginDTO.getUsername());
        } else {
            throw new RuntimeException("Wrong account or password.");
        }
    }

    @PostMapping("/test")
    public String test(
    ) throws IOException, InterruptedException {

        Thread.sleep(new Random().nextInt(new Random().nextInt(5) + 1)*1000);
        return "111";
    }

}
