package com.alter.obsyncshare.session;


import com.alter.obsyncshare.controller.UserController;
import com.alter.obsyncshare.dto.GitConfigDTO;
import com.alter.obsyncshare.utils.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UserSession {
    private static final Logger logger =
            LoggerFactory.getLogger(UserController.class);

    private Path userDir;

    public static String tokenDirName = "tokens";
    public static String userinfoFileName = "userinfo";
    public static String syncLockFileName = "sync_lock";
    public static String configFileName = "config";
    public static String vaultDirName = "vault";
    public static String shareDirName = "share";
    public static String serverDeleteHistoryFileName = "server_delete_history";
    public static String userShareNoteMainPathFileName = "__user_share_note_main_path__";

    public UserSession(Path userDir) {
        this.userDir = userDir;
    }


    private File getUserinfoFile() throws IOException {
        Path userinfo = userDir.resolve(userinfoFileName);
        File file = userinfo.toFile();
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    public File getUserDir(String username) {
        Path myUserDir = userDir.resolve(username);
        File file = myUserDir.toFile();
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public GitConfigDTO getGitConfig(String username) throws IOException {
        File myUserDir = getUserDir(username);
        File configFile = myUserDir.toPath().resolve(configFileName).toFile();
        if (!configFile.exists()) {
            configFile.createNewFile();
        }
        GitConfigDTO gitConfigDTO = new GitConfigDTO();
//        gitConfigDTO.setRemoteGitAddress();
        List<String> strings = Files.readAllLines(configFile.toPath());
        strings.forEach(s -> {
            if (!s.isBlank()) {
                String[] split = s.split("\t");
                if (split.length == 2) {
                    if (split[0].equals("syncToLocalGit")) {
                        gitConfigDTO.setSyncToLocalGit(Boolean.parseBoolean(split[1]));
                    } else if (split[0].equals("maximumCommitFileSize")) {
                        gitConfigDTO.setMaximumCommitFileSize(Integer.parseInt(split[1]));
                    } else if (split[0].equals("remoteGitAddress")) {
                        gitConfigDTO.setRemoteGitAddress(split[1]);
                    } else if (split[0].equals("remoteGitUsername")) {
                        gitConfigDTO.setRemoteGitUsername(split[1]);
                    } else if (split[0].equals("remoteGitAccessToken")) {
                        gitConfigDTO.setRemoteGitAccessToken(split[1]);
                    }
                }
            }
        });
        return gitConfigDTO;
    }

    public void setGitConfig(String username, GitConfigDTO gitConfigDTO) throws IOException {
        File myUserDir = getUserDir(username);
        File configFile = myUserDir.toPath().resolve(configFileName).toFile();
        if (!configFile.exists()) {
            configFile.createNewFile();
        }

        List<String> list = new ArrayList<>();
        list.add("syncToLocalGit\t" + Boolean.toString(gitConfigDTO.isSyncToLocalGit()));
        list.add("maximumCommitFileSize\t" + gitConfigDTO.getMaximumCommitFileSize());
        list.add("remoteGitAddress\t" + gitConfigDTO.getRemoteGitAddress());
        list.add("remoteGitUsername\t" + gitConfigDTO.getRemoteGitUsername());
        list.add("remoteGitAccessToken\t" + gitConfigDTO.getRemoteGitAccessToken());

        String collect = list.stream().collect(Collectors.joining("\n"));
        Files.writeString(configFile.toPath(), collect);
    }

    public File getUserVaultDir(String username) {
        File myUserDir = getUserDir(username);
        File vaultDir = myUserDir.toPath().resolve(vaultDirName).toFile();
        if (!vaultDir.exists()) {
            vaultDir.mkdirs();
        }
        return vaultDir;
    }

    public File getUserShareDir(String username) {
        File myUserDir = getUserDir(username);
        File shareDir = myUserDir.toPath().resolve(shareDirName).toFile();
        if (!shareDir.exists()) {
            shareDir.mkdirs();
        }
        return shareDir;
    }


    public Path getUserShareNoteMainPath(String username, String shareLinkId) throws IOException {
        return getUserShareDir(username).toPath().resolve(shareLinkId).resolve(userShareNoteMainPathFileName);
    }

    private File getUserTokenDir(String username) {
        Path myUserDir = userDir.resolve(username);
        Path myTokenDir = myUserDir.resolve(tokenDirName);
        File file = myTokenDir.toFile();
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }


    public File getServerDeleteHistory(String username) throws IOException {
        File userDir = getUserDir(username);
        File file = userDir.toPath().resolve(serverDeleteHistoryFileName).toFile();
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }


    /**
     * 创建锁
     *
     * @param username
     * @param token
     * @return
     * @throws IOException
     */
    private boolean createLock(String username, String token) throws IOException {
        File userDir = getUserDir(username);
        File lockFile = userDir.toPath().resolve(syncLockFileName).toFile();
        if (!lockFile.exists() && lockFile.createNewFile()) {
            Files.writeString(lockFile.toPath(), token);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 上锁
     * 如果其他端未释放返回false
     *
     * @param username
     * @param token
     * @return
     * @throws IOException
     */
    public boolean lock(String username, String token) throws IOException {
        File userDir = getUserDir(username);
        File lockFile = userDir.toPath().resolve(syncLockFileName).toFile();
        if (lockFile.exists()) {
            String s = Files.readString(lockFile.toPath());
            if (s.equals(token)) {
                return true;
            } else {
                // 如果时间太久也解锁
                long lastModified = lockFile.lastModified();
                long currentTime = System.currentTimeMillis();
                long timeDifference = currentTime - lastModified;
                long halfHourInMillis = 30 * 60 * 1000; // 半小时的毫秒数
                if (timeDifference > halfHourInMillis) {
                    // 删除锁 ,重新创建锁
                    lockFile.delete();
                    return createLock(username, token);
                }
            }
        } else {
            return createLock(username, token);
        }
        return false;
    }

    public boolean unlock(String username, String token) throws IOException {
        File userDir = getUserDir(username);
        File lockFile = userDir.toPath().resolve(syncLockFileName).toFile();
        if (lockFile.exists()) {
            String s = Files.readString(lockFile.toPath());
            if (s.equals(token)) {
                // 删除锁文件
                return lockFile.delete();
            }
        } else {
            return false;
        }
        return false;
    }


    public boolean isValidUsernameAndPassword(String username, String password) throws IOException {
        File userinfo = getUserinfoFile();
        List<String> userinfoList = Files.readAllLines(userinfo.toPath());
        // 如果存在直接返回
        if (userinfoList.contains(username + "\t" + MD5Util.getMD5Code(password))) {
            return true;
        }
        // 不存在看看账号是否存在
        for (String s : userinfoList) {
            // 账号存在 直接返回校验失败
            if (s.split("\t")[0].equals(username)) {
                return false;
            }
        }

        logger.debug("账号:{}/{}不存在,创建该账号", username, password);
        // 账号不存在 则创建账号
        userinfoList.add(username + "\t" + MD5Util.getMD5Code(password));

        Files.write(userinfo.toPath(), userinfoList);
        return true;
    }

    public String createToken(String username) throws IOException {

        File userTokenDir = getUserTokenDir(username);
        String token = UUID.randomUUID().toString();

        File tokenFile = userTokenDir.toPath().resolve(token).toFile();
        tokenFile.createNewFile();
        return token;
    }


    public boolean isValidToken(String username, String token) throws IOException {

        File userTokenDir = getUserTokenDir(username);

        File tokenFile = userTokenDir.toPath().resolve(token).toFile();
        return tokenFile.exists();
    }


    /**
     * 检查用户名或toekn是否合法
     *
     * @param username
     * @param token
     * @throws IOException
     */
    public void checkUsernameAndToken(String username, String token) throws IOException {
        // 避免被注入
        if (containsIllegalCharacters(username) || containsIllegalCharacters(token)) {
            throw new RuntimeException("Invalid input");
        }
        // 检查token是否有效
        boolean validToken = isValidToken(username, token);
        if (!validToken) {
            throw new RuntimeException("token is invalid, please try to log in again");
        }
    }

    public static boolean containsIllegalCharacters(String input) {
        String[] illegalCharacters = {".", "/", "\\"};

        for (String character : illegalCharacters) {
            if (input.contains(character)) {
                return true;
            }
        }

        return false;
    }
}
