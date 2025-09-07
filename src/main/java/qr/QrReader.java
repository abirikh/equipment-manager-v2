package qr;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class QrReader {

    // Чтение QR из файла PNG
    public static String readQr(String filename) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new File(filename));
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (IOException | NotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Чтение QR из InputStream (для Telegram фото)
    public static String readQr(InputStream input) {
        try {
            BufferedImage bufferedImage = ImageIO.read(input);
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (IOException | NotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}

