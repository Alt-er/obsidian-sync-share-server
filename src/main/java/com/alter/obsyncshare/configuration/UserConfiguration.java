package com.alter.obsyncshare.configuration;


import com.alter.obsyncshare.session.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Configuration
public class UserConfiguration {


    @Autowired
    private Environment environment;

    @Bean("userSession")
    public UserSession createUserSession(){
        ///Users/guoxiaomin/obsidian-plugin-test-server
        String ob_server_user_dir = environment.getProperty("OB_SERVER_USER_DIR");
        if(ob_server_user_dir== null || ob_server_user_dir.isBlank()){
            ob_server_user_dir = "user_store";
        }
        Path path = Paths.get(ob_server_user_dir);
        if(!path.toFile().exists()){
            path.toFile().mkdirs();
        }
        UserSession userSession = new UserSession(path);
        return userSession;
    }

}
