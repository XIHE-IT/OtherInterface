package com.xihe.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

/**
 * @Author gzy
 * @Date 2024/9/12 14:05
 */
@Slf4j
public class IOUtil {
    //上传目录的基础路径
    private static final String basePath = System.getProperty("user.dir") + "/upload/";

    /**
     * 根据文件夹名称获取上传路径
     *
     * @param folderName 需要上传的业务文件夹
     * @return java.lang.String
     * @author gzy
     * @date 2024/9/12 14:23
     */
    public static String getUploadPath(String folderName) {
        return basePath + folderName + "/";
    }

    /**
     * 根据文件夹名称获取上传目录，若不存在则创建
     *
     * @param filePath 文件夹路径
     * @author gzy
     * @date 2024/9/12 14:17
     */
    public static void judgeUploadPath(String filePath) {
        File tempFile = new File(filePath);
        if (!tempFile.exists()) {
            boolean isCreated = tempFile.mkdirs();
            if (isCreated) {
                log.info("目录创建成功：{}", filePath);
            } else {
                log.info("目录创建失败");
            }
        }
    }

    /**
     * 把文件保存到指定的目录，并返回文件保存的路径
     *
     * @param folderName 业务需要上传的文件夹
     * @param file       需要上传的文件
     * @return java.lang.String
     * @author gzy
     * @date 2024/9/12 14:29
     */
    public static String saveFile(String folderName, MultipartFile file) {
        String tempFileName = file.getOriginalFilename();
        assert tempFileName != null;
        int index = tempFileName.indexOf(".");
        String fileName = UUID.randomUUID() + tempFileName.substring(index);

        try {
            String filePath = getUploadPath(folderName);
            judgeUploadPath(filePath);
            File tempFile = new File(filePath + fileName);
            file.transferTo(tempFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fileName;
    }

    /**
     * 获取文件的完整路径
     *
     * @param folderName 业务文件夹名称
     * @param fileName   文件名
     * @return java.lang.String
     * @author gzy
     * @date 2024/9/12 14:50
     */
    public static String getCompleteFilePath(String folderName, String fileName) {
        return basePath + folderName + "/" + fileName;
    }

    /**
     * 删除文件
     *
     * @param folderName 业务文件夹名称
     * @param fileName   文件名
     * @author gzy
     * @date 2024/9/12 15:00
     */
    public static void deleteFile(String folderName, String fileName) {
        String filePath = getCompleteFilePath(folderName, fileName);
        File file = new File(filePath);
        if (file.exists()) {
            boolean isDeleted = file.delete();
            if (isDeleted) {
                log.info("文件删除成功：{}", filePath);
            } else {
                log.info("文件删除失败：{}", filePath);
            }
        }
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @author gzy
     * @date 2024/11/25 13:34
     */
    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            boolean isDeleted = file.delete();
            if (isDeleted) {
                log.info("文件删除成功：{}", filePath);
                return;
            }
            log.info("文件删除失败：{}", filePath);
            throw new RuntimeException("文件删除失败：" + filePath + "！");
        }
    }

    /**
     * 将字节转换为更易读的格式（例如 KB, MB, GB）
     *
     * @param size 字节大小
     * @return 格式化后的字符串
     */
    public static String formatSize(long size) {
        long KB = 1024;
        long MB = KB * 1024;
        long GB = MB * 1024;

        if (size >= GB) {
            return String.format("%.2f GB", (double) size / GB);
        } else if (size >= MB) {
            return String.format("%.2f MB", (double) size / MB);
        } else if (size >= KB) {
            return String.format("%.2f KB", (double) size / KB);
        } else {
            return String.format("%d B", size);
        }
    }

    /**
     * 字节数组写入文件
     *
     * @param allFileName 包括路径和名称的完整路径
     * @param imageBytes  图片的byte数组
     * @author gzy
     * @date 2024/10/27 16:11
     */
    public static void byteChangeFile(String allFileName, byte[] imageBytes) {
        // 将字节数组写入文件
        try (FileOutputStream fos = new FileOutputStream(allFileName)) {
            fos.write(imageBytes);
        } catch (Exception e) {
            throw new RuntimeException("上传文件失败，失败原因：" + e.getMessage() + "！");
        }
    }

    /**
     * 文件转base64
     *
     * @param filePath 需要转换的文件
     * @return java.lang.String
     * @author gzy
     * @date 2024/11/23 11:36
     */
    public static String convertToBase64(String filePath) throws IOException {
        // 读取文件内容到字节数组
        FileInputStream fileInputStream = new FileInputStream(filePath);
        byte[] fileContent = fileInputStream.readAllBytes();
        fileInputStream.close();

        // 将字节数组编码为Base64字符串
        return Base64.getEncoder().encodeToString(fileContent);
    }
}