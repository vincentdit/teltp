package tz.go.tirdo.teltp.certification.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tz.go.tirdo.teltp.certification.entity.Certificate;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/** Renders a certificate to PDF (OpenPDF) with an embedded QR (ZXing) pointing at the verification URL. */
@Component
public class CertificatePdfGenerator {

    @Value("${teltp.certificate.verification-base-url}")
    private String verificationBaseUrl;

    public byte[] render(Certificate cert) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 48, 48, 48, 48);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = new Font(Font.HELVETICA, 30, Font.BOLD, new Color(20, 60, 120));
            Font normal = new Font(Font.HELVETICA, 14);
            Font nameFont = new Font(Font.HELVETICA, 24, Font.BOLD);

            Paragraph header = new Paragraph("TIRDO e-Learning & Training Platform", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            doc.add(header);
            doc.add(spacer());

            Paragraph sub = new Paragraph("Certificate of Completion", new Font(Font.HELVETICA, 18, Font.ITALIC));
            sub.setAlignment(Element.ALIGN_CENTER);
            doc.add(sub);
            doc.add(spacer());

            Paragraph awarded = new Paragraph("This is to certify that", normal);
            awarded.setAlignment(Element.ALIGN_CENTER);
            doc.add(awarded);

            Paragraph name = new Paragraph(cert.getRecipientName(), nameFont);
            name.setAlignment(Element.ALIGN_CENTER);
            doc.add(name);
            doc.add(spacer());

            Paragraph course = new Paragraph("has successfully completed\n" + cert.getCourseTitle(), normal);
            course.setAlignment(Element.ALIGN_CENTER);
            doc.add(course);
            doc.add(spacer());

            String issued = cert.getIssuedOn().format(DateTimeFormatter.ISO_DATE);
            Paragraph meta = new Paragraph(
                    "Reference: " + cert.getReferenceNumber() + "    Issued: " + issued, normal);
            meta.setAlignment(Element.ALIGN_CENTER);
            doc.add(meta);

            if (cert.getAccreditingBody() != null) {
                Paragraph accred = new Paragraph(
                        "Accredited by " + cert.getAccreditingBody()
                                + (cert.getAccreditationLevel() == null ? "" : " (" + cert.getAccreditationLevel() + ")"),
                        normal);
                accred.setAlignment(Element.ALIGN_CENTER);
                doc.add(accred);
            }

            doc.add(spacer());
            String verifyUrl = verificationBaseUrl + "/" + cert.getVerificationCode();
            Image qr = Image.getInstance(qrPng(verifyUrl));
            qr.scaleAbsolute(110, 110);
            qr.setAlignment(Element.ALIGN_CENTER);
            doc.add(qr);

            Paragraph verify = new Paragraph("Verify at " + verifyUrl, new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY));
            verify.setAlignment(Element.ALIGN_CENTER);
            doc.add(verify);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessRuleException("Failed to render certificate PDF: " + e.getMessage());
        }
    }

    private Paragraph spacer() {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(8);
        return p;
    }

    private byte[] qrPng(String text) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 220, 220);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", png);
        return png.toByteArray();
    }
}
