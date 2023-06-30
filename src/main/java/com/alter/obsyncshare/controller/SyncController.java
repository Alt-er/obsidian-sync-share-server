package com.alter.obsyncshare.controller;

import com.alter.obsyncshare.bo.*;
import com.alter.obsyncshare.dto.GitConfigDTO;
import com.alter.obsyncshare.session.UserSession;
import com.alter.obsyncshare.utils.GitUtil;
import com.alter.obsyncshare.utils.LockUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;


@RestController
@RequestMapping("/api/sync")
//@CrossOrigin(origins = "app://obsidian.md")
@CrossOrigin
public class SyncController {

    private static final Logger logger =
            LoggerFactory.getLogger(SyncController.class);


    @Autowired
    private UserSession userSession;

    private void checkAndCreateFileOrFolder(File file, boolean isDir) throws IOException {
        Lock lock = LockUtil.lock("__checkAndCreateFileOrFolder__");
        lock.lock();
        try {
            if (isDir) {
                if (!file.exists()) {
                    file.mkdirs();
                }
            } else {
                File parentFile = file.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                if (!file.exists()) {
                    file.createNewFile();
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            lock.unlock();
        }

    }

    /**
     * 同步完成后调用此方法解锁
     * 可选: 同步git
     *
     * @param request
     * @param response
     */
    @PostMapping("/syncCompleted")
    public String syncCompleted(HttpServletRequest request, HttpServletResponse response) throws IOException, GitAPIException, URISyntaxException {

        String username = request.getHeader("username");
        String token = request.getHeader("token");

        checkUsernameAndToken(username, token);

        GitConfigDTO gitConfig = userSession.getGitConfig(username);
        if (gitConfig.isSyncToLocalGit()) {
            String userAgent = request.getHeader("User-Agent");

            // git commit
            File userVaultDir = userSession.getUserVaultDir(username);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = sdf.format(new Date());
            GitUtil.commitAll(userVaultDir.toPath(), formattedDate, userAgent == null ? "" : userAgent , gitConfig.getMaximumCommitFileSize());

            logger.debug("git commit executed successfully");

            if (!gitConfig.getRemoteGitAddress().isBlank()) {
                GitUtil.push(userVaultDir.toPath(), gitConfig.getRemoteGitAddress(), gitConfig.getRemoteGitUsername(), gitConfig.getRemoteGitAccessToken());
                logger.debug("git push executed successfully");
            }

        }
        boolean unlock = userSession.unlock(username, token);
        if (unlock) {
            return "Unlocked successfully";
        } else {
            logger.error("Unlock failure");
            return "Unlock failure";
        }

    }

    @PostMapping("/downloadFile")
    public void downloadFile(HttpServletRequest request, HttpServletResponse response
            , @RequestParam("path") String path
    ) throws IOException {

        String username = request.getHeader("username");
        String token = request.getHeader("token");

        checkUsernameAndToken(username, token);

        FileInfo fileInfo = new FileInfo(path, 0);

        Path userVaultPath = userSession.getUserVaultDir(username).toPath();
        Path filePath = userVaultPath.resolve(fileInfo.getPath());

        File file = filePath.toFile();
        if (!file.exists()) {
            throw new RuntimeException("File does not exist");
        }
        boolean isDir = file.isDirectory();

        // 设置响应头信息
        String type = Files.probeContentType(filePath);
        if (type == null || type.isBlank()) {
            type = "application/octet-stream";
        }
        response.setContentType(type); // 替换为实际的内容类型
        response.setHeader("Content-Disposition", "attachment; filename=" + UriUtils.encode(file.getName(), "UTF-8")); // 替换为实际的文件名
        response.setHeader("mtime", file.lastModified() + "");
        response.setHeader("isDir", String.valueOf(isDir));
        response.setHeader("Access-Control-Expose-Headers", "mtime, isDir");
        if (!isDir) {
            byte[] bytes = Files.readAllBytes(filePath);
            // 将 Blob 数据写入响应流
            response.getOutputStream().write(bytes);
            logger.debug("文件写入客户端完成");
        }
    }

    @PostMapping("/uploadFile")
    public String uploadFile(HttpServletRequest request, HttpServletResponse response
            , @RequestParam(value = "file", required = false) MultipartFile file
            , @RequestParam("path") String path
            , @RequestParam("mtime") long mtime
            , @RequestParam("isDir") boolean isDir) throws IOException {

        String username = request.getHeader("username");
        String token = request.getHeader("token");

        checkUsernameAndToken(username, token);

        FileInfo fileInfo = new FileInfo(path, mtime);
        Path userVaultPath = userSession.getUserVaultDir(username).toPath();
        Path filePath = userVaultPath.resolve(fileInfo.getPath());

        File targetFile = filePath.toFile();
        if (isDir) {
            checkAndCreateFileOrFolder(targetFile, true);
        } else {
            checkAndCreateFileOrFolder(targetFile, false);
            // 如果发现本该写入的文件是一个文件夹且是空的则删除
            if (targetFile.isDirectory() && targetFile.list().length == 0) {
                targetFile.delete();
                checkAndCreateFileOrFolder(targetFile, false);
            }
            Files.write(filePath, file.getBytes());
            targetFile.setLastModified(fileInfo.getMtime());
        }
        logger.debug("file:{} , path:{} , mtime:{}", file, path, mtime);
        return "file upload successfully";
    }

    @PostMapping("/syncDeleteHistoryAndFiles")
    public String syncDeleteHistoryAndFiles(HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, List<String>> requestBody) throws IOException {
        String username = request.getHeader("username");
        String token = request.getHeader("token");

        checkUsernameAndToken(username, token);

        List<String> deleteDeleteHistorys = requestBody.get("deleteDeleteHistorys");
        List<String> deleteFiles = requestBody.get("deleteFiles");
        List<String> uploadDeleteHistorys = requestBody.get("uploadDeleteHistorys");


        // 删除DeleteHistory
        Path path = userSession.getServerDeleteHistory(username).toPath();
        List<String> serverDeleteHistoryTxt = Files.readAllLines(path);
        serverDeleteHistoryTxt.removeIf(s ->
                deleteDeleteHistorys.contains(s.split("\t")[0]));


        // 删除文件
        Path userVaultPath = userSession.getUserVaultDir(username).toPath();
        deleteFiles.stream().sorted(Comparator.comparingInt(String::length).reversed()).map(df -> new FileInfo(df, 0)).forEach(fi -> {
            File file = userVaultPath.resolve(fi.getPath()).toFile();
            if (file.exists()) {
                if (file.isFile() || (file.isDirectory() && file.list().length == 0)) {
                    logger.debug("删除文件:{}", file.getAbsolutePath());
                    file.delete();
                }
            }
        });


        // 更新删除记录
        uploadDeleteHistorys.stream()
                .map(dh -> dh.split("\t"))
                .map(arr -> new DeleteHistory(arr[0], Long.parseLong(arr[1])))
                .forEach(fi -> {
                    // 清除同名的
                    serverDeleteHistoryTxt.removeIf(s ->
                            fi.getPath().equals(s.split("\t")[0]));
                    // 加入这条删除记录
                    serverDeleteHistoryTxt.add(fi.getPath() + "\t" + fi.getDeleteTime());
                });
        try {
            Files.write(path, serverDeleteHistoryTxt);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "sync successfully";
    }

    @PostMapping("/diff")
    public Diff diff(HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, String[][]> requestBody) throws IOException {

        String username = request.getHeader("username");
        String token = request.getHeader("token");

        checkUsernameAndToken(username, token);

        Lock lock = LockUtil.lock(username);
        if (!lock.tryLock()) {
            //其他客户端正在同步中,请稍后再试
            throw new RuntimeException("Other clients are in the process of synchronization, please try again later");
        }
        try {
            boolean localLock = userSession.lock(username, token);
            if (localLock) {
                String[][] fileInfos = requestBody.get("fileInfos");
                String[][] deleteHistory = requestBody.get("deleteHistory");


                List<FileInfo> clientFileInfoList = new ArrayList<>();
                List<DeleteHistory> clientDeleteHistoryList = new ArrayList<>();

                // 转换为FileInfo对象
                for (String[] fileInfo : fileInfos) {
                    clientFileInfoList.add(new FileInfo(fileInfo[0], Long.parseLong(fileInfo[1])));
                }

                // 转换为DeleteHistory对象
                for (String[] d : deleteHistory) {
                    clientDeleteHistoryList.add(new DeleteHistory(d[0], Long.parseLong(d[1])));
                }


                // 将服务端文件转换为FileInfo对象
                // 此处要考虑填相对路径的情况
//                String directoryPath =
//                        Paths.get("/Users/guoxiaomin/obsidian-plugin-test-server").toAbsolutePath().toString();
                String directoryPath = userSession.getUserVaultDir(username).getCanonicalPath();
                List<FileInfo> serverFileInfoList = new ArrayList<>();
                listFilesAndDirectories(directoryPath, directoryPath, serverFileInfoList);


                // 将服务端删除记录转换为DeleteHistory对象
                Path path = userSession.getServerDeleteHistory(username).toPath();
                List<String> serverDeleteHistoryTxt = Files.readAllLines(path);
                List<DeleteHistory> serverDeleteHistoryList = new ArrayList<>();
                serverDeleteHistoryTxt.forEach(dh -> {
                    String[] s = dh.split("\t");
                    // path delete_time
                    serverDeleteHistoryList.add(new DeleteHistory(s[0], Long.parseLong(s[1])));
                });

                Set<String> clientDeleteHistorySet = new HashSet<>();
                Set<String> serverDeleteHistorySet = new HashSet<>();
                clientDeleteHistoryList.forEach(dh -> {
                    clientDeleteHistorySet.add(dh.getPath());
                });
                serverDeleteHistoryList.forEach(dh -> {
                    serverDeleteHistorySet.add(dh.getPath());
                });
                List<FullOuterJoinWarpper> fullOuterJoinWarppers = fullOuterJoin(clientFileInfoList, serverFileInfoList, clientDeleteHistoryList, serverDeleteHistoryList);
                Diff diff = generateDiffAction(fullOuterJoinWarppers, clientDeleteHistorySet, serverDeleteHistorySet);

//                printFullOuterJoinTable(fullOuterJoinWarppers);

                return diff;

            } else {
                // 上锁失败 可能是其他用户正在同步中
                throw new RuntimeException("Other clients are in the process of synchronization, please try again later");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            lock.unlock();
        }


//        printFullOuterJoinTable(fullOuterJoinTable);
//        logger.debug(clientFileInfoList.toString());
//        logger.debug(clientDeleteHistoryList.toString());

//        logger.debug(serverFileInfoList.toString());
//        logger.debug(clientDeleteHistoryList.toString());


    }


    private Diff generateDiffAction(List<FullOuterJoinWarpper> fullOuterJoinWarppers, Set<String> clientDeleteHistorySet, Set<String> serverDeleteHistorySet) {

        DiffActionInfo clientDiffActionInfo = new DiffActionInfo();
        DiffActionInfo serverDiffActionInfo = new DiffActionInfo();

        List<FullOuterJoinWarpper> fileAndFolderConflictList = new ArrayList<>();

        fullOuterJoinWarppers.forEach(j -> {
            FileInfo clientFileInfo = j.getClientFileInfo();
            FileInfo serverFileInfo = j.getServerFileInfo();
            DeleteHistory clientDeleteHistory = j.getClientDeleteHistory();
            DeleteHistory serverDeleteHistory = j.getServerDeleteHistory();

            long clientFileOpTime = clientFileInfo == null ? 0 : clientFileInfo.getMtime();
            long serverFileOpTime = serverFileInfo == null ? 0 : serverFileInfo.getMtime();
            long clientDeleteOpTime = clientDeleteHistory == null ? 0 : clientDeleteHistory.getDeleteTime();
            long serverDeleteOpTime = serverDeleteHistory == null ? 0 : serverDeleteHistory.getDeleteTime();

            long max = findMax(clientFileOpTime, serverFileOpTime, clientDeleteOpTime, serverDeleteOpTime);

            // 客户端删除记录操作时间最新
            if (clientDeleteHistory != null && clientDeleteHistory.getDeleteTime() == max) {
                // 删除客户端和服务端文件 , 加一个if判断,如果有才删除
                if (clientFileInfo != null)
                    clientDiffActionInfo.getDeleteFiles().add(clientDeleteHistory.getPath());
                if (serverFileInfo != null)
                    serverDiffActionInfo.getDeleteFiles().add(clientDeleteHistory.getPath());
                // 上传客户端删除记录
                clientDiffActionInfo.getUploadDeleteHistorys().add(clientDeleteHistory.getPath());
                // 删除客户端删除记录
                clientDiffActionInfo.getDeleteDeleteHistorys().add(clientDeleteHistory.getPath());
            } else if (serverDeleteHistory != null && serverDeleteHistory.getDeleteTime() == max) {
                // 服务端删除记录操作时间最新

                // 删除客户端和服务端文件
                if (clientFileInfo != null)
                    clientDiffActionInfo.getDeleteFiles().add(serverDeleteHistory.getPath());
                if (serverFileInfo != null)
                    serverDiffActionInfo.getDeleteFiles().add(serverDeleteHistory.getPath());

                // 删除客户端删除记录
                if (clientDeleteHistorySet.contains(serverDeleteHistory.getPath()))
                    clientDiffActionInfo.getDeleteDeleteHistorys().add(serverDeleteHistory.getPath());
            } else if (clientFileInfo != null && clientFileInfo.getMtime() == max) {

                // 删除客户端删除记录
                if (clientDeleteHistorySet.contains(clientFileInfo.getPath()))
                    clientDiffActionInfo.getDeleteDeleteHistorys().add(clientFileInfo.getPath());
                // 删除服务端删除记录
                if (serverDeleteHistorySet.contains(clientFileInfo.getPath()))
                    serverDiffActionInfo.getDeleteDeleteHistorys().add(clientFileInfo.getPath());

                // 服务端 和 客户端都存在文件
                if (clientFileInfo != null && serverFileInfo != null) {
                    // 如果服务端和客户端时间一样则跳过
                    if (clientFileInfo.getMtime() == serverFileInfo.getMtime()) {
                        return;
                    }
                    // 如果客户端是文件夹 服务端是文件
                    if (clientFileInfo.getMtime() == 0 && serverFileInfo.getMtime() > 0) {
                        fileAndFolderConflictList.add(j);
                        return;
                    }

                    // 如果客户端是文件 服务端是文件夹
                    if (clientFileInfo.getMtime() > 0 && serverFileInfo.getMtime() == 0) {
                        fileAndFolderConflictList.add(j);
                        return;
                    }

                }
                // 客户端文件操作时间最新

                // 上传客户端文件
                clientDiffActionInfo.getUploadFiles().add(clientFileInfo.getPath());

            } else if (serverFileInfo != null && serverFileInfo.getMtime() == max) {

                // 删除客户端删除记录
                if (clientDeleteHistorySet.contains(serverFileInfo.getPath()))
                    clientDiffActionInfo.getDeleteDeleteHistorys().add(serverFileInfo.getPath());
                // 删除服务端删除记录
                if (serverDeleteHistorySet.contains(serverFileInfo.getPath()))
                    serverDiffActionInfo.getDeleteDeleteHistorys().add(serverFileInfo.getPath());

                // 服务端 和 客户端都存在文件
                if (clientFileInfo != null && serverFileInfo != null) {
                    // 如果服务端和客户端时间一样则跳过
                    if (clientFileInfo.getMtime() == serverFileInfo.getMtime()) {
                        return;
                    }
                    // 如果客户端是文件夹 服务端是文件
                    if (clientFileInfo.getMtime() == 0 && serverFileInfo.getMtime() > 0) {
                        fileAndFolderConflictList.add(j);
                        return;
                    }

                    // 如果客户端是文件 服务端是文件夹
                    if (clientFileInfo.getMtime() > 0 && serverFileInfo.getMtime() == 0) {
                        fileAndFolderConflictList.add(j);
                        return;
                    }

                }

                // 服务端文件操作时间最新

                // 客户端下载服务端文件
                clientDiffActionInfo.getDownloadFiles().add(serverFileInfo.getPath());

            } else {
                logger.error("出现未知情况...");
            }


        });


        // 最后处理 fileAndFolderConflictList , 这里面都是一方是文件一方是文件夹的数据
        fileAndFolderConflictList.forEach(c -> {
            FileInfo clientFileInfo = c.getClientFileInfo();
            FileInfo serverFileInfo = c.getServerFileInfo();

            // 客户端是一个文件夹
            if (clientFileInfo.getMtime() == 0) {
                boolean find = false;
                // 遍历整个表
                for (FullOuterJoinWarpper f : fullOuterJoinWarppers) {
                    // 找出子目录和子文件
                    if (f.getClientFileInfo() != null
                            && f.getPath().length() > clientFileInfo.getPath().length()
                            && Paths.get(f.getPath()).startsWith(clientFileInfo.getPath())
                            // 并且不能是马上要被删除的文件
                            && !clientDiffActionInfo.getDeleteFiles().contains(f.getPath())
                    ) {
                        find = true;
                        // 发现存在子文件或子文件夹
                        // 1. 服务端删除这个文件
                        serverDiffActionInfo.getDeleteFiles().add(clientFileInfo.getPath());
                        // 2. 客户端上传这个文件夹
                        clientDiffActionInfo.getUploadFiles().add(clientFileInfo.getPath());
                        break;
                    }
                }

                // 如果没找到
                if (!find) {
                    //  客户端删除这个文件夹
                    clientDiffActionInfo.getDeleteFiles().add(clientFileInfo.getPath());
                    clientDiffActionInfo.getDownloadFiles().add(clientFileInfo.getPath());
                }


            } else if (serverFileInfo.getMtime() == 0) {
                // 服务端是一个文件夹
                boolean find = false;
                for (FullOuterJoinWarpper f : fullOuterJoinWarppers) {
                    // 找出子目录和子文件
                    if (f.getServerFileInfo() != null
                            && f.getPath().length() > serverFileInfo.getPath().length()
                            && Paths.get(f.getPath()).startsWith(serverFileInfo.getPath())
                            // 并且不能是马上要被删除的文件
                            && !serverDiffActionInfo.getDeleteFiles().contains(f.getPath())
                    ) {
                        find = true;
                        // 发现存在子文件或子文件夹
                        // 1. 客户端删除这个文件
                        clientDiffActionInfo.getDeleteFiles().add(serverFileInfo.getPath());
                        // 2. 客户端下载这个文件夹
                        clientDiffActionInfo.getDownloadFiles().add(serverFileInfo.getPath());
                        break;
                    }
                }
                // 如果没找到
                if (!find) {
                    //  服务端删除这个文件夹
                    serverDiffActionInfo.getDeleteFiles().add(serverFileInfo.getPath());
                    // 客户端上传这个文件
                    clientDiffActionInfo.getUploadFiles().add(serverFileInfo.getPath());
                }
            }

        });
        return new Diff(clientDiffActionInfo, serverDiffActionInfo);
    }

    public static long findMax(long... values) {
        long max = Long.MIN_VALUE;
        for (long value : values) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }


    private List<FullOuterJoinWarpper> fullOuterJoin(List<FileInfo> clientFileInfoList, List<FileInfo> serverFileInfoList, List<DeleteHistory> clientDeleteHistoryList, List<DeleteHistory> serverDeleteHistoryList) {
        List<FullOuterJoinWarpper> fullOuterJoinTable = new ArrayList<>(clientFileInfoList.size() * 2);
        clientFileInfoList.forEach(cfi -> {
            FileInfo serverFileInfo = listRemoveFirstAndGet(serverFileInfoList, info ->
                    info.getPath().equals(cfi.getPath())
            );

            DeleteHistory clientDeleteHistory = listRemoveFirstAndGet(clientDeleteHistoryList, info ->
                    info.getPath().equals(cfi.getPath())
            );

            DeleteHistory serverDeleteHistory = listRemoveFirstAndGet(serverDeleteHistoryList, info ->
                    info.getPath().equals(cfi.getPath())
            );

            fullOuterJoinTable.add(new FullOuterJoinWarpper(cfi.getPath(), cfi, serverFileInfo, clientDeleteHistory, serverDeleteHistory));
        });

        // 处理剩余的
        serverFileInfoList.forEach(sfi -> {
            DeleteHistory clientDeleteHistory = listRemoveFirstAndGet(clientDeleteHistoryList, info ->
                    info.getPath().equals(sfi.getPath())
            );

            DeleteHistory serverDeleteHistory = listRemoveFirstAndGet(serverDeleteHistoryList, info ->
                    info.getPath().equals(sfi.getPath())
            );

            fullOuterJoinTable.add(new FullOuterJoinWarpper(sfi.getPath(), null, sfi, clientDeleteHistory, serverDeleteHistory));
        });

        // 处理剩余的
        clientDeleteHistoryList.forEach(cdh -> {
            DeleteHistory serverDeleteHistory = listRemoveFirstAndGet(serverDeleteHistoryList, info ->
                    info.getPath().equals(cdh.getPath())
            );
            fullOuterJoinTable.add(new FullOuterJoinWarpper(cdh.getPath(), null, null, cdh, serverDeleteHistory));
        });

        // 处理剩余的
        serverDeleteHistoryList.forEach(sdh -> {
            fullOuterJoinTable.add(new FullOuterJoinWarpper(sdh.getPath(), null, null, null, sdh));
        });


        return fullOuterJoinTable;

    }

    private void printDiffs(List<Diff> diffs) {

    }

    private void checkUsernameAndToken(String username, String token) throws IOException {
        userSession.checkUsernameAndToken(username, token);
    }

    private void printFullOuterJoinTable(List<FullOuterJoinWarpper> fullOuterJoinTable) {
        System.out.println("| clientFileInfo | serverFileInfo | clientDeleteHistory | serverDeleteHistory |");
        System.out.println("|--|--|--|--|");
        for (FullOuterJoinWarpper fullOuterJoinWarpper : fullOuterJoinTable) {
            System.out.println(fullOuterJoinWarpper);
        }
    }

    private <E> E listRemoveFirstAndGet(List<E> list, Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        final Iterator<E> each = list.iterator();
        while (each.hasNext()) {
            E next = each.next();
            if (filter.test(next)) {
                each.remove();
                return next;
            }
        }
        return null;
    }


    public void listFilesAndDirectories(String directoryPath, String basePath, List<FileInfo> fileList) {
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                String relativePath = file.getAbsolutePath().substring(basePath.length());
                long modifiedTime = file.lastModified();

                // 不要隐藏文件夹
                if (!file.isHidden()) {
                    fileList.add(new FileInfo(relativePath, file.isFile() ? modifiedTime : 0));

                    if (file.isDirectory()) {
                        listFilesAndDirectories(file.getAbsolutePath(), basePath, fileList);
                    }
                }

            }
        }
    }


}
