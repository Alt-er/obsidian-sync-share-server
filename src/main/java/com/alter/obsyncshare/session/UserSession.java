package com.alter.obsyncshare.session;


import com.alter.obsyncshare.controller.UserController;
import com.alter.obsyncshare.utils.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class UserSession {
    private static final Logger logger =
            LoggerFactory.getLogger(UserController.class);

    private Path userDir;

    public static String tokenDirName = "tokens";
    public static String userinfoFileName = "userinfo";
    public static String syncLockFileName = "sync_lock";
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
