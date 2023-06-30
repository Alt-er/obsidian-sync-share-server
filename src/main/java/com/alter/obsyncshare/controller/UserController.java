package com.alter.obsyncshare.controller;

import com.alter.obsyncshare.dto.GitConfigDTO;
import com.alter.obsyncshare.dto.LoginDTO;
import com.alter.obsyncshare.session.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {


    private static final Logger logger = LoggerFactory.getLogger(UserController.class);


    @Autowired
    private UserSession userSession;


    @PostMapping("/login")
    public String login(@RequestBody LoginDTO loginDTO) throws IOException {

        logger.debug("用户:{}正在尝试登录", loginDTO.getUsername());

        boolean validUsernameAndPassword = userSession.isValidUsernameAndPassword(loginDTO.getUsername(), loginDTO.getPassword());
        if (validUsernameAndPassword) {
            logger.debug("用户:{}登录成功", loginDTO.getUsername());
            return userSession.createToken(loginDTO.getUsername());
        } else {
            throw new RuntimeException("Wrong account or password.");
        }
    }

    @PostMapping("/updateGitConfig")
    public String updateGitConfig(HttpServletRequest request, HttpServletResponse response, @RequestBody GitConfigDTO gitConfigDTO) throws IOException, InterruptedException {
        String username = request.getHeader("username");
        String token = request.getHeader("token");
        checkUsernameAndToken(username, token);

        userSession.setGitConfig(username, gitConfigDTO);

        GitConfigDTO gitConfig = userSession.getGitConfig(username);

        logger.debug("gitConfig:" + gitConfig);

        return "Git configuration update successful";
    }

    @PostMapping("/getGitConfig")
    public GitConfigDTO getGitConfig(HttpServletRequest request, HttpServletResponse response) throws IOException, InterruptedException {
        String username = request.getHeader("username");
        String token = request.getHeader("token");
        checkUsernameAndToken(username, token);

        GitConfigDTO gitConfig = userSession.getGitConfig(username);

        logger.debug("gitConfig:" + gitConfig);

        return gitConfig;
    }

    @PostMapping("/test")
    public String test() throws IOException, InterruptedException {

        return "111";
    }


    private void checkUsernameAndToken(String username, String token) throws IOException {
        userSession.checkUsernameAndToken(username, token);
    }
}
