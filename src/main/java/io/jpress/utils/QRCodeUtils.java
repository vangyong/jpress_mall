package io.jpress.utils;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class QRCodeUtils {
	
	/**
	 * 创建二维码
	 * @param text URL
	 * @param fileName /Users/wangyong/Downloads/1.png
	 * @param width 100
	 * @param height 100
	 * @param fileName 2
	 */
	public static void createQRCode(String text,String fileName,int width,int height,int margin) {
		String format = "png";
		Hashtable hints = new Hashtable();
		hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
		hints.put(EncodeHintType.MARGIN, margin);
		try {
			BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints);
			Path file = new java.io.File(fileName).toPath();
			MatrixToImageWriter.writeToPath(bitMatrix, format, file);
		} catch (WriterException | IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 创建二维码
	 * @param text URL
	 * @param fileName /Users/wangyong/Downloads/imgs/baidu.png
	 * @param width 100
	 * @param height 100
	 * @param fileName 2
	 */
	public static Result encodeQRCode(String fileName){
		MultiFormatReader formatReader = new MultiFormatReader();
		File file = new File(fileName);
		BufferedImage image = null;
		try {
			image = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
		Hashtable hints = new Hashtable();
		hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
		Result result = null;
		try {
			result = formatReader.decode(binaryBitmap, hints);
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("解析结果：" + result.toString());
		System.out.println(result.getBarcodeFormat());
		System.out.println(result.getText());
		return result;
	}
	
	/**
	 * 合并图片
	 * @param img1 /Users/wangyong/Downloads/1.png
	 * @param img2 /Users/wangyong/Downloads/2.png
	 * @param outImg /Users/wangyong/Downloads/hebing.png
	 * @return
	 */
	public static void mergeImage(String img1,String img2,String outImg){
		
		//创建四个文件对象（注：这里两张图片的宽度都是相同的，因此下文定义BufferedImage宽度就取第一只的宽度就行了）
		File _file1 = new File(img1);
		File _file2 = new File(img2);
 
		Image src1;
		Image src2;
		try {
			src1 = javax.imageio.ImageIO.read(_file1);
			src2 = javax.imageio.ImageIO.read(_file2);
			//获取图片的宽度
			int width = src1.getWidth(null);
			//获取各个图像的高度
			int height1 = src1.getHeight(null);
			int height2 = src2.getHeight(null);
			
			//构造一个类型为预定义图像类型之一的 BufferedImage。 宽度为第一只的宽度，高度为各个图片高度之和
			BufferedImage tag = new BufferedImage(width, height1 + height2, BufferedImage.TYPE_INT_RGB);
			//创建输出流
			FileOutputStream out = new FileOutputStream(outImg);
			//绘制合成图像
			Graphics g = tag.createGraphics();
			g.drawImage(src1, 0, 0, width, height1, null);
			g.drawImage(src2, 0, height1, width , height2, null);
			// 释放此图形的上下文以及它使用的所有系统资源。
			g.dispose();
			//将绘制的图像生成至输出流
			JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
			encoder.encode(tag);
			//关闭输出流
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	
	
	
	

}
