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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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

    @GetMapping("/getNewVersion")
    public String getNewVersion() throws IOException, InterruptedException {
        //const res = await fetch("https://hub.docker.com/v2/repositories/alterzz/obsidian-sync-share-server/tags")
        try {
            // 创建URL对象
            URL url = new URL("https://hub.docker.com/v2/repositories/alterzz/obsidian-sync-share-server/tags");
            // 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 设置请求方法为GET
            connection.setRequestMethod("GET");
            // 获取响应代码
            int responseCode = connection.getResponseCode();
            // 判断请求是否成功
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取响应内容
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                // 打印响应内容
//              System.out.println(response.toString());
                return response.toString();
            } else {
                logger.error("请求失败，响应代码：" + responseCode);
            }

            // 关闭连接
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null ;
    }


    private void checkUsernameAndToken(String username, String token) throws IOException {
        userSession.checkUsernameAndToken(username, token);
    }
}
