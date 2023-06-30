package com.alter.obsyncshare.utils;

import com.alter.obsyncshare.controller.SyncController;
import com.alter.obsyncshare.dto.FileHistoryDTO;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class GitUtil {
    private static final Logger logger =
            LoggerFactory.getLogger(GitUtil.class);


    private static final String GIT_DATA_DIR = ".git";

    private static Git getGit(Path path) throws IOException, GitAPIException {
        Git git = null;
        // 获取git对象
        if (Files.exists(path.resolve(GIT_DATA_DIR))) {
            git = Git.open(path.toFile());
        } else {
            git = Git.init().setDirectory(path.toFile()).call();
        }
        return git;
    }

    /**
     * 提交所有更改
     *
     * @param path
     * @param message
     * @throws GitAPIException
     * @throws IOException
     */
    public static synchronized void commitAll(Path path, String message, String UA, int maxFileSize) throws GitAPIException, IOException {
        try (Git git = getGit(path)) {
            // 获取当前仓库的状态
            Status status = git.status().call();

            // 遍历工作目录下的所有文件
            for (String filePath : status.getUntracked()) {
                File file = new File(git.getRepository().getWorkTree(), filePath);

                // 检查文件大小是否超过指定的最大大小
                if (file.isFile() && file.length() > maxFileSize * 1024 * 1024) {
                    logger.debug("Skipping file: " + filePath);
                    continue;
                }
                // 检查文件是否已经在暂存区中存在，如果存在则跳过
                if (status.getAdded().contains(filePath) || status.getChanged().contains(filePath)) {
                    logger.debug("Skipping file (already in index): " + filePath);
                    continue;
                }
                // 添加文件到暂存区
                git.add().addFilepattern(filePath).call();
            }
            for (String filePath : status.getModified()) {
                // 添加文件到暂存区
                git.add().addFilepattern(filePath).call();
            }
            for (String filePath : status.getRemoved()) {
                // 添加文件到暂存区
                git.add().addFilepattern(filePath).call();
            }

            // 提交所有文件
            git.commit().setMessage(message).setAuthor(UsernameGeneratorUtil.generateUsername(UA), "").call();
        }
    }

    public static synchronized void push(Path path, String remoteGitAddress, String username, String accessToken) throws GitAPIException, IOException, URISyntaxException {
        try (Git git = getGit(path)) {
            git.remoteSetUrl().setRemoteName("origin").setRemoteUri(new URIish(remoteGitAddress)).call();
//            if (username.isBlank() || accessToken.isBlank()) {
//                git.push().setForce(true).setRemote("origin").call();
//            } else {
            CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, accessToken);
            git.push().setCredentialsProvider(credentialsProvider).setForce(true).setRemote("origin").call();
//            }

        }
    }

    public static synchronized List<FileHistoryDTO> fileHistory(Path path, String filePath, int page, int pageSize) throws GitAPIException, IOException {
        List<FileHistoryDTO> list = new ArrayList<>();
        try (Git git = getGit(path);) {
            // 创建 LogCommand 获取提交历史
            LogCommand logCommand = git.log().addPath(filePath);

            // 设置每页的提交记录数量
            logCommand.setMaxCount(pageSize);
            // 计算要跳过的提交记录数量
            int skipCount = (page - 1) * pageSize;
            if (skipCount > 0) {
                logCommand.setSkip(skipCount);
            }

            // 执行 LogCommand 并返回提交历史
            Iterable<RevCommit> call = logCommand.call();
            for (RevCommit commit : call) {
//                System.out.println("Commit ID: " + commit.getId().getName());
//                System.out.println("Author: " + commit.getAuthorIdent().getName());
//                System.out.println("Time: " + commit.getAuthorIdent().getWhen());
//                System.out.println("Message: " + commit.getFullMessage());

                FileHistoryDTO fileHistoryDTO = new FileHistoryDTO();
                fileHistoryDTO.setCommitId(commit.getId().getName());
                fileHistoryDTO.setAuthor(commit.getAuthorIdent().getName());
                fileHistoryDTO.setTime(commit.getAuthorIdent().getWhen());
                fileHistoryDTO.setMessage(commit.getFullMessage());
                fileHistoryDTO.setPath(filePath);
                list.add(fileHistoryDTO);


            }
        }catch (NoHeadException noHeadException){
            return list;
        }
        return list;
    }

    public static synchronized byte[] fileContent(Path path, String commitId, String filePath) throws GitAPIException, IOException {
        try (Git git = getGit(path);) {
            Repository repository = git.getRepository();
            try (RevWalk revWalk = new RevWalk(repository);
                 TreeWalk treeWalk = new TreeWalk(repository)) {
                RevTree tree = revWalk.parseTree(ObjectId.fromString(commitId));
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(filePath));
                if (treeWalk.next()) {
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);
                    return loader.getBytes();
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws GitAPIException, IOException {
        Path user_store = Paths.get("user_store/alter");
        System.out.println(user_store.toAbsolutePath());


//        System.out.println(Arrays.toString(git.getRepository().getWorkTree().list()));

//
//        new GitUtil().commitAll(user_store, "提交时间:" + new Date().getTime(), "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
//        List<FileHistoryDTO> fileHistoryDTOS = new GitUtil().fileHistory(user_store, "vault/未命名 1.md", 1, 5);
//        System.out.println(fileHistoryDTOS);
//        byte[] bytes = new GitUtil().fileContent(user_store, "ae182a32bfa66f80cba9f9efd2291a35b4c3698a", "vault/未命名 1.md");
//        System.out.println(bytes);


    }
}
