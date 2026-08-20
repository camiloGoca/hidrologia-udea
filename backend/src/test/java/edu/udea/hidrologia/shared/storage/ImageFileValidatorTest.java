package edu.udea.hidrologia.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageFileValidatorTest {

    private ImageFileValidator validator;

    @BeforeEach
    void setUp() {
        ImageUploadProperties properties = new ImageUploadProperties();
        validator = new ImageFileValidator(properties);
    }

    @Test
    void returnsEmptyWhenFileIsAbsent() {
        assertThat(validator.validateOptional(null)).isEmpty();
    }

    @Test
    void acceptsRealJpegImage() throws Exception {
        ImageUpload upload = validator.validateOptional(image("image.jpg", "image/jpeg", "jpg")).orElseThrow();

        assertThat(upload.format()).isEqualTo("jpg");
        assertThat(upload.width()).isEqualTo(2);
        assertThat(upload.height()).isEqualTo(2);
        assertThat(upload.bytes()).isPositive();
    }

    @Test
    void acceptsRealPngImage() throws Exception {
        ImageUpload upload = validator.validateOptional(image("image.png", "image/png", "png")).orElseThrow();

        assertThat(upload.format()).isEqualTo("png");
        assertThat(upload.width()).isEqualTo(2);
        assertThat(upload.height()).isEqualTo(2);
        assertThat(upload.bytes()).isPositive();
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> validator.validateOptional(file))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("The uploaded image is empty");
    }

    @Test
    void rejectsFilesLargerThanFiveMb() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "big.png",
                "image/png",
                new byte[(5 * 1024 * 1024) + 1]);

        assertThatThrownBy(() -> validator.validateOptional(file))
                .isInstanceOf(ImageTooLargeException.class)
                .hasMessage("The uploaded image exceeds the maximum size");
    }

    @Test
    void rejectsUnsupportedGifEvenWhenItIsARealImage() {
        byte[] gif = new byte[] {
                71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0, 0, 0, 0, -1, -1, -1, 33, -7, 4, 1, 0, 0, 0,
                0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 68, 1, 0, 59
        };
        MockMultipartFile file = new MockMultipartFile("image", "image.gif", "image/gif", gif);

        assertThatThrownBy(() -> validator.validateOptional(file))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("Only JPEG and PNG images are supported");
    }

    @Test
    void rejectsFakeJpegContent() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "fake.jpg",
                "image/jpeg",
                "not really an image".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validator.validateOptional(file))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("The uploaded file is not a valid JPEG or PNG image");
    }

    @Test
    void rejectsSvgEvenWhenContentTypeClaimsImage() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "image.svg",
                "image/jpeg",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validator.validateOptional(file))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("The uploaded file is not a valid JPEG or PNG image");
    }

    @Test
    void rejectsPdfEvenWhenContentTypeClaimsImage() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "document.pdf",
                "image/png",
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validator.validateOptional(file))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("The uploaded file is not a valid JPEG or PNG image");
    }

    @Test
    void rejectsImagesWithAbsurdDimensionsBeforeFullDecode() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "huge.png",
                "image/png",
                pngHeaderWithDimensions(20_000, 20_000));

        assertThatThrownBy(() -> validator.validateOptional(file))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("The uploaded image dimensions are too large");
    }

    private MockMultipartFile image(String filename, String contentType, String format) throws Exception {
        BufferedImage bufferedImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, format, output);

        return new MockMultipartFile("image", filename, contentType, output.toByteArray());
    }

    private byte[] pngHeaderWithDimensions(int width, int height) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});

        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        DataOutputStream ihdrData = new DataOutputStream(ihdr);
        ihdrData.writeInt(width);
        ihdrData.writeInt(height);
        ihdrData.writeByte(8);
        ihdrData.writeByte(2);
        ihdrData.writeByte(0);
        ihdrData.writeByte(0);
        ihdrData.writeByte(0);
        writeChunk(output, "IHDR", ihdr.toByteArray());

        writeChunk(output, "IEND", new byte[0]);

        return output.toByteArray();
    }

    private void writeChunk(ByteArrayOutputStream output, String type, byte[] data) throws Exception {
        DataOutputStream dataOutput = new DataOutputStream(output);
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        dataOutput.writeInt(data.length);
        dataOutput.write(typeBytes);
        dataOutput.write(data);

        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        dataOutput.writeInt((int) crc.getValue());
    }
}
