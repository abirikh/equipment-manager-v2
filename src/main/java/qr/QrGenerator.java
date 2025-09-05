package qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class QrGenerator {

    public static void generateQr(String text, String filename) {
        QRCodeWriter qrWriter = new QRCodeWriter();
        try {
            BitMatrix matrix = qrWriter.encode(text, BarcodeFormat.QR_CODE, 250, 250);
            Path path = new File(filename + ".png").toPath();
            MatrixToImageWriter.writeToPath(matrix, "PNG", path);
            System.out.println("QR-код сохранён: " + filename + ".png");
        } catch (WriterException | IOException e) {
            e.printStackTrace();
        }
    }
}
