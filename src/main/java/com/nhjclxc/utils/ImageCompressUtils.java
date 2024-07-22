package com.nhjclxc.utils;

import net.coobird.thumbnailator.Thumbnails;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 图片压缩工具
 */
public class ImageCompressUtils {

    private static ByteArrayOutputStream inputStream2OutputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream;
    }

    /**
     * 在线图片转换成base64字符串
     */
    public static String compressByImageUrl(String imgURL) {
        try {
            // 创建URL
            URL url = new URL(imgURL);
            byte[] by = new byte[1024];
            // 创建链接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            InputStream inputStream = compressByInputStream(conn.getInputStream(), 40000);
            assert inputStream != null;
            ByteArrayOutputStream outputStream = inputStream2OutputStream(inputStream);
            inputStream.close();

            // 对字节数组Base64编码
            return new BASE64Encoder().encode(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InputStream compressByInputStream(InputStream inputStream) {
        return compressByInputStream(inputStream, 1204 * 1024);
    }

    public static InputStream compressByInputStream(InputStream inputStream, int maxSize) {
        if (maxSize < 0)
            maxSize = 1204 * 1024;

        try {
            BufferedImage src = ImageIO.read(inputStream);
            BufferedImage output = Thumbnails.of(src).size(src.getWidth() / 3, src.getHeight() / 3).asBufferedImage();
            int size = output.getData().getDataBuffer().getSize();
            if (size - size / 8 * 2 > maxSize) {
                output = Thumbnails.of(output).scale(1.0 / (size / maxSize)).asBufferedImage();
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(output, "png", os);
            return new ByteArrayInputStream(os.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 本地图片转换成base64字符串
     *
     * @param filePath 图片路径
     */
    public static String imageToBase64(String filePath) throws IOException {
        InputStream in = Files.newInputStream(Paths.get(filePath));
        InputStream inputStream = compressByInputStream(in);
        assert inputStream != null;
        // 对字节数组Base64编码
        String encode = new BASE64Encoder().encode(inputStream2OutputStream(inputStream).toByteArray());
        inputStream.close();
        return encode;
    }

}
