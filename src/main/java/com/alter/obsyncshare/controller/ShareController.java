package com.alter.obsyncshare.controller;


import com.alter.obsyncshare.bo.FileInfo;
import com.alter.obsyncshare.dto.ShareHistoryDTO;
import com.alter.obsyncshare.dto.ShareNoteDTO;
import com.alter.obsyncshare.session.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.alter.obsyncshare.session.UserSession.userShareNoteMainPathFileName;

@RestController
@RequestMapping("/api/share")
@CrossOrigin
public class ShareController {


    private static final Logger logger =
            LoggerFactory.getLogger(UserController.class);


    @Autowired
    private UserSession userSession;

    private static String getFileExtension(File file) {
        String extension = "";

        // 获取文件名
        String fileName = file.getName();

        // 获取最后一个点（.）的索引
        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            // 提取后缀
            extension = fileName.substring(dotIndex + 1);
        }

        return extension;
    }

    public static String getFileNameWithoutExtension(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            fileName = fileName.substring(0, dotIndex);
        }
        return fileName;
    }


    public static File findAttachmentByLink(Path userShareNoteDirPath, Path userShareNoteMainPath, String link) throws IOException {
        // 如果是一个远程链接,则直接跳过
        if (link.startsWith("http://") || link.startsWith("https://")) {
            return null;
        }


        // 1. 先绝对路径取一次
        // 2. 取不到当相对路径取一次
        // 3. 取不到按照文件名取一次 广度优先 , 需要先判断是不是一个文件名
        if (!(link.startsWith("./") || link.startsWith("../"))) {
            Path abs = userShareNoteDirPath.resolve(link).normalize();
            // 安全检查 ,不能去检查其他目录
            if (abs.startsWith(userShareNoteDirPath)) {
                File file = abs.toFile();
                if (file.exists() && file.isFile()) {
                    return file;
                }
            }
        }

        // 相对路径查找
        Path rel = userShareNoteMainPath.getParent().resolve(link).normalize();
        // 安全检查 ,不能去检查其他目录
        if (rel.startsWith(userShareNoteDirPath)) {
            File file = rel.toFile();
            if (file.exists() && file.isFile()) {
                return file;
            }
        }

        if (!link.contains("/")) {

            File file = breadthFirstSearch(userShareNoteDirPath, link);
            if (file != null) {
                return file;
            }
        }
        return null;


        // const abs = file.vault.getAbstractFileByPath(link);

    }

    public static File breadthFirstSearch(Path dir, String fileName) throws IOException {
        if (Files.isDirectory(dir)) {
            File file = Files.list(dir).filter(path -> path.toFile().isFile() && path.getFileName().toString().equals(fileName))
                    .findFirst()
                    .map(path -> path.toFile())
                    .orElse(null);
            if (file != null) {
                return file;
            } else {
                List<Path> list = Files.list(dir).filter(path -> path.toFile().isDirectory()).collect(Collectors.toList());
                for (Path path : list) {
                    File find = breadthFirstSearch(path, fileName);
                    if (find != null) {
                        return find;
                    }
                }
            }
        }
        return null;
    }


    @GetMapping("/content/{username}/{shareLinkId}")
    public String content(HttpServletRequest request, HttpServletResponse response,
                          @PathVariable String username, @PathVariable String shareLinkId,
                          @RequestParam(value = "link", required = false) String link) throws IOException {

//        userSession.checkUsernameAndToken(username, "___");
        if (userSession.containsIllegalCharacters(username) || userSession.containsIllegalCharacters(shareLinkId)) {
            return "";
        }

        Path userShareNoteMainPath = userSession.getUserShareNoteMainPath(username, shareLinkId);
        File userShareNoteMainPathFile = userShareNoteMainPath.toFile();
        if (!userShareNoteMainPathFile.exists()) {
            return "Notes do not exist";
        }

        String mainPath = Files.readString(userShareNoteMainPathFile.toPath());
        if (mainPath == null || mainPath.isBlank()) {
            return "Notes do not exist";
        }

        String[] split = mainPath.split("\t");
        mainPath = split[0];

        if (split.length > 1) {
            long expirationDate = Long.parseLong(split[1]);
            if (expirationDate > 0 && System.currentTimeMillis() > expirationDate) {
                // 过期了 删除目录
                deleteFolder(userShareNoteMainPathFile.getParentFile());
                return "Notes do not exist";
            }
        }


        Path userShareDirPath = userSession.getUserShareDir(username).toPath();
        // 分享的笔记放到这个下面了
        Path userShareNoteDirPath = userShareDirPath.resolve(shareLinkId);

        // 绝对路径下的主文件path
        Path absMainPath = userShareNoteDirPath.resolve(mainPath);

        // 如果link不是空的
        if (link != null && !link.isBlank()) {
            File attachmentByLink = findAttachmentByLink(userShareNoteDirPath, absMainPath, link);
            if (attachmentByLink != null && attachmentByLink.exists()) {
                // 这里要判断类型 如果是图片就直接展示,如果是文件就下载,如果是md txt 则返回字符串
                File file = attachmentByLink;
                Path linkFilePath = attachmentByLink.toPath();
                String fileExtension = getFileExtension(file);
                String fileName = getFileNameWithoutExtension(file);
                response.setHeader("title", UriUtils.encode(fileName, "UTF-8"));
                response.setHeader("Access-Control-Expose-Headers", "title");
                switch (fileExtension) {
                    case "txt":
                    case "md":
                        // 执行文本文件的逻辑
                        logger.debug("这是一个文本文件");
                        return Files.readString(linkFilePath);
                    case "jpg":
                    case "png":
                    case "gif":
                    case "webp":
                    case "bmp":
                    case "mp4":
//                        System.out.println("这是一个媒体文件");
                        // 执行图片文件的逻辑
                        // 设置响应头信息
                        // "application/octet-stream"
                        String type = Files.probeContentType(linkFilePath);
                        if (type == null || type.isBlank()) {
                            type = "application/octet-stream";
                        }
                        response.setContentType(type); // 替换为实际的内容类型
                        response.setHeader("Content-Disposition", "inline; filename=" + UriUtils.encode(file.getName(), "UTF-8")); // 替换为实际的文件名
                        byte[] bytes = Files.readAllBytes(linkFilePath);
                        // 将 Blob 数据写入响应流
                        response.getOutputStream().write(bytes);
                        logger.debug("文件写入客户端完成");
                        return null;
                    default:
//                        System.out.println("这是一个未知文件");
                        type = Files.probeContentType(linkFilePath);
                        if (type == null || type.isBlank()) {
                            type = "application/octet-stream";
                        }
                        response.setContentType(type); // 替换为实际的内容类型
                        response.setHeader("Content-Disposition", "inline; filename=" + UriUtils.encode(file.getName(), "UTF-8")); // 替换为实际的文件名
                        bytes = Files.readAllBytes(linkFilePath);
                        // 将 Blob 数据写入响应流
                        response.getOutputStream().write(bytes);
                        logger.debug("文件写入客户端完成");
                        return null;
                }
            }
        } else {
            String mdContent = Files.readString(absMainPath);
            String fileName = getFileNameWithoutExtension(absMainPath.toFile());
            response.setHeader("title", UriUtils.encode(fileName, "UTF-8"));
            response.setHeader("Access-Control-Expose-Headers", "title");
            return mdContent;
        }


        return "";
    }

    @PostMapping("/shareNote")
    public String shareNote(
            @RequestParam Map<String, MultipartFile> fileMap,
            @ModelAttribute ShareNoteDTO otherInfo,
            HttpServletRequest request, HttpServletResponse response
    ) throws IOException {
        String username = request.getHeader("username");
        String token = request.getHeader("token");

        userSession.checkUsernameAndToken(username, token);

        logger.debug("用户:{} 分享信息: {}", username, otherInfo);
        logger.debug("用户:{} 分享的文件: {}", username, fileMap);


        Path userShareDirPath = userSession.getUserShareDir(username).toPath();

        String shareLinkId = (otherInfo.getShareLinkId() == null ||
                otherInfo.getShareLinkId().isBlank()) ?
                UUID.randomUUID().toString() : otherInfo.getShareLinkId();

        if (userSession.containsIllegalCharacters(shareLinkId)) {
            throw new RuntimeException("Invalid input");
        }

        // 这个分享的笔记放到这个下面
        Path userShareNoteDirPath = userShareDirPath.resolve(shareLinkId);

        fileMap.forEach((path, file) -> {
            Path filePath = userShareNoteDirPath.resolve(new FileInfo(path, 0).getPath());
            // 父文件是否存在
            File parentDir = filePath.getParent().toFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            // 写入文件
            try {
                Files.write(filePath, file.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Path userShareNoteMainPath = userSession.getUserShareNoteMainPath(username, shareLinkId);
        if (!Files.exists(userShareNoteMainPath.getParent())) {
            Files.createDirectories(userShareNoteMainPath.getParent());
        }

        Files.writeString(userShareNoteMainPath, otherInfo.getMainPath() + "\t" + otherInfo.getExpirationDate()+ "\t" + otherInfo.getHeaderPosition());

        // 存入share记录
//        File userShareLinksFile = userSession.getUserShareLinksFile(username);
//        List<String> strings = Files.readAllLines(userShareLinksFile.toPath());
//        strings.add(shareLinkId + "\t" + new FileInfo(otherInfo.getMainPath(), 0).getPath());
//        Files.write(userShareLinksFile.toPath(), strings);

        return "/share/" + username + "/" + shareLinkId+"?headerPosition="+otherInfo.getHeaderPosition();
    }

    @PostMapping("/delete")
    public String delete(
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam("shareLinkId") String shareLinkId
    ) throws IOException {
        String username = request.getHeader("username");
        String token = request.getHeader("token");

        userSession.checkUsernameAndToken(username, token);

        if (userSession.containsIllegalCharacters(shareLinkId)) {
            throw new RuntimeException("Invalid input");
        }

        Path userShareNoteDir = userSession.getUserShareDir(username).toPath().resolve(shareLinkId);
        if (Files.exists(userShareNoteDir)) {
            // 删除文件比较重要 多加一层校验 避免有其他注入漏洞
            Path userDirPath = userSession.getUserDir(username).toPath().normalize();
            Path userDirParentPath = userSession.getUserDir(username).toPath().getParent().normalize();
            Path normalize = userShareNoteDir.normalize();
            // 确定是在用户目录下  这样风险降低到最小
            if (normalize.startsWith(userDirParentPath) && normalize.startsWith(userDirPath)) {
                deleteFolder(userShareNoteDir.toFile());
                logger.debug("delete share notes, path:{} ", userShareNoteDir.toString());
            }
        }
        return "delete successfully";
    }


    @GetMapping("/shareHistory")
    public List<ShareHistoryDTO> shareHistory(
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam("path") String path
    ) throws IOException {
        String username = request.getHeader("username");
        String token = request.getHeader("token");

        userSession.checkUsernameAndToken(username, token);

        File userShareDir = userSession.getUserShareDir(username);


        List<ShareHistoryDTO> res = new ArrayList<>();

        File[] files = userShareDir.listFiles(); // 获取文件夹下的所有文件
        // 按照创建时间倒序排序
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        // 打印文件列表
        for (File file : files) {
            if (file.isDirectory()) {
                Path mainPathFileNamePath = file.toPath().resolve(userShareNoteMainPathFileName);
                File mainPathFileNameFile = mainPathFileNamePath.toFile();

                if (mainPathFileNameFile.exists() && mainPathFileNameFile.isFile()) {
                    try {
                        String s = Files.readString(mainPathFileNamePath);
                        String[] split = s.split("\t");
                        String tempPath = split[0];
                        String expirationDate = split.length > 1 ? split[1] : "0"; // 2099年
                        String headerPosition = split.length > 2 ? split[2] : "";
                        if ("0".equals(expirationDate)) {
                            expirationDate = "4070880000151";
                        }

                        if (tempPath.equals(path)) {
                            res.add(new ShareHistoryDTO("/share/" + username + "/" + file.getName()+"?headerPosition="+headerPosition, new Date(Long.parseLong(expirationDate))));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return res;
    }


    public static void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }

        // 删除空文件夹或文件
        folder.delete();
    }
}
