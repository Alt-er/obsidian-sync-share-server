package com.alter.obsyncshare.controller;


import com.alter.obsyncshare.dto.FileHistoryDTO;
import com.alter.obsyncshare.dto.LoginDTO;
import com.alter.obsyncshare.session.UserSession;
import com.alter.obsyncshare.utils.GitUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin
public class HistoryController {

    private static final Logger logger =
            LoggerFactory.getLogger(HistoryController.class);


    @Autowired
    private UserSession userSession;

    @GetMapping("/fileHistory")
    public List<FileHistoryDTO> fileHistory(@RequestParam(value = "file", required = true) String file
                                            ,@RequestParam(value = "page", required = true) int page
            , HttpServletRequest request, HttpServletResponse response) throws IOException, GitAPIException {
        String username = request.getHeader("username");
        String token = request.getHeader("token");

        userSession.checkUsernameAndToken(username, token);

        File userVaultDir = userSession.getUserVaultDir(username);
        List<FileHistoryDTO> fileHistoryDTOS = GitUtil.fileHistory(userVaultDir.toPath(), file, page, 10);

        return fileHistoryDTOS;
    }

    @GetMapping("/fileContent")
    public void fileContent(@RequestParam(value = "file", required = true) String file,
                                            @RequestParam(value = "commitId", required = true) String commitId
            , HttpServletRequest request, HttpServletResponse response) throws IOException, GitAPIException {
        String username = request.getHeader("username");
        String token = request.getHeader("token");

        userSession.checkUsernameAndToken(username, token);

        File userVaultDir = userSession.getUserVaultDir(username);
        byte[] bytes = GitUtil.fileContent(userVaultDir.toPath(), commitId, file);

        // 设置响应头信息
        String type = "application/octet-stream";
        response.setContentType(type); // 替换为实际的内容类型
        response.setHeader("Content-Disposition", "attachment; filename=" + UriUtils.encode(new Date().getTime()+"", "UTF-8")); // 替换为实际的文件名
        response.getOutputStream().write(bytes);
    }


}
