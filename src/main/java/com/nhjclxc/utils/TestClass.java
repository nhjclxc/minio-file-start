package com.nhjclxc.utils;

import com.idrsolutions.image.png.PngCompressor;
import org.apache.tomcat.util.codec.binary.Base64;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class TestClass {


    /**
     * @param file        图片
     * @param imageWidth  宽
     * @param imageHeight 高
     * @return boolean true：符合要求
     * @description 校验图片比例
     */
    public static boolean checkImageScale(File file, int imageWidth, int imageHeight) throws IOException {

        boolean result = false;
        if (!file.exists()) {
            return false;
        }
        BufferedImage bufferedImage = ImageIO.read(file);
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        if (imageHeight != 0 && height != 0) {
            int scale1 = imageHeight / imageWidth;
            int scale2 = height / width;
            if (scale1 == scale2) {
                result = true;
            }
        }
        return result;
    }


    /**
     * @param path 图片路径
     * @return java.lang.String base64字符串
     * @description 将图片文件转化为字节数组字符串，并对其进行Base64编码处理
     */
    public static String imgToBase64Str(String path) throws IOException {
        byte[] data = null;
        // 读取图片字节数组
        InputStream in = null;
        try {
            in = Files.newInputStream(Paths.get(path));
            data = new byte[in.available()];
            in.read(data);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //返回Base64编码过的字节数组字符串
        return Base64.encodeBase64String(data);
    }


    /**
     * 按比例对图片进行缩放
     *
     * @param scale 图片根据比例缩放
     * @throws IOException
     */
    public static BufferedImage zoomByScale(BufferedImage bufferedImage, double scale) throws IOException {

        //获取图片的长和宽
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        //获取缩放后的长和宽
        int _width = (int) (scale * width);
        int _height = (int) (scale * height);

        //获取缩放后的Image对象
        Image _img = bufferedImage.getScaledInstance(_width, _height, Image.SCALE_DEFAULT);

        //新建一个和Image对象相同大小的画布:BufferedImage.TYPE_INT_ARGB
        BufferedImage image = new BufferedImage(_width, _height, BufferedImage.TYPE_INT_ARGB);
        //获取画笔
        Graphics2D graphics = image.createGraphics();
        //将Image对象画在画布上,最后一个参数,ImageObserver:接收有关 Image 信息通知的异步更新接口,没用到直接传空
        graphics.drawImage(_img, 0, 0, null);
        //释放资源
        graphics.dispose();

        return image;

    }

    /**
     * png -支持图片背景透明
     * 图片指定长和宽对图片进行缩放
     *
     * @param width  长
     * @param height 宽
     * @throws IOException
     */
    public static BufferedImage zoomBySize(BufferedImage bufferedImage, int width, int height) {
        //与按比例缩放的不同只在于,不需要获取新的长和宽,其余相同.
        Image _img = bufferedImage.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(_img, 0, 0, null);
        graphics.dispose();
        return image;
    }

    /**
     * @param background 背景
     * @param foreground 前景
     * @param x          前景x坐标
     * @param y          前景y坐标
     * @return
     * @description 将前景图片合成到背景图上
     */
    public static BufferedImage compose(BufferedImage background, BufferedImage foreground, int x, int y) {
        try {
            Graphics2D g = background.createGraphics();
            g.drawImage(foreground, x, y, foreground.getWidth(), foreground.getHeight(), null);
            g.dispose();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return background;

    }


    /**
     * 裁剪png图片的非透明区域，使用最大x和y的边框
     *
     * @param bufferedImage
     * @return
     * @throws IOException
     */
    public static BufferedImage getNotTransparentArea(BufferedImage bufferedImage) throws IOException {

        int w = bufferedImage.getWidth();
        int h = bufferedImage.getHeight();
        int x = 0, y = 0, width = 0, height = 0;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int dip = bufferedImage.getRGB(i, j);
                if ((dip & 0xff000000) != 0) {
                    if (x == 0 || i < x) {
                        x = i;
                    }
                    if (y == 0 || j < y) {
                        y = j;
                    }
                    if (width == 0 || i > width) {
                        width = i;
                    }
                    if (height == 0 || j > height) {
                        height = j;
                    }
                }
            }
        }
        bufferedImage = bufferedImage.getSubimage(x - 1, y - 1, width - x + 1, height - y + 1);
        return bufferedImage;
    }

    /**
     * bufferedImage 转化为文件
     * @param bufferedImage
     * @param ext 图片的格式：png
     * @throws IOException
     */
    public static byte[] bufferedImageToFileByte(String tempFilePath, BufferedImage bufferedImage, String ext) throws IOException {
        //使用ImageIO的方法进行输出,记得关闭资源
        File file = new File(tempFilePath+ "/" + new Random().nextInt(10) + "_temp_heap_image."+ext);
        OutputStream out = Files.newOutputStream(file.toPath());
        ImageIO.write(bufferedImage, ext, out);
        out.close();
        ByteArrayOutputStream swapStream = getByteArrayOutputStream(file);
        //对于图片是jpg或者小于500KB，就直接输出
        if(ext.equalsIgnoreCase("jpg") || swapStream.toByteArray().length/1024 <=500){
            return swapStream.toByteArray();
        }
        File compressOutFile=new File(tempFilePath+ "/" + new Random().nextInt(10) + "_temp_heap_image_cp."+ext);
        //如果图片是png，且大于500KB，则进行压缩
        PngCompressor.compress(file,compressOutFile);
        swapStream = getByteArrayOutputStream(compressOutFile);
        return swapStream.toByteArray();
    }

    private static ByteArrayOutputStream getByteArrayOutputStream(File file) throws IOException {
        InputStream inputStream = Files.newInputStream(file.toPath());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        inputStream.close();
        return outputStream;
    }

}
