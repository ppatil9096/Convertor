import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

public class FileConverter {

    private static final int INITIAL_BUFFER_SIZE = 2048;
    private static final int LF = '\n';
    private static final int NEL = 0x15;
    private static final int WS = ' ';
    static final Charset CP1047 = Charset.forName("Cp1047");
    private static final char[] NON_PRINTABLE_EBCDIC_CHARS = new char[] { 0x00, 0x01, 0x02, 0x03, 0x9C, 0x09, 0x86, 0x7F, 0x97, 0x8D, 0x8E, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x9D, 0x85, 0x08, 0x87, 0x18, 0x19, 0x92, 0x8F, 0x1C, 0x1D, 0x1E, 0x1F, 0x80, 0x81, 0x82, 0x83, 0x84, 0x0A, 0x17, 0x1B, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x05, 0x06, 0x07, 0x90, 0x91, 0x16, 0x93, 0x94, 0x95, 0x96, 0x04, 0x98, 0x99, 0x9A, 0x9B, 0x14, 0x15, 0x9E, 0x1A, 0x20, 0xA0 };

    private final Charset inputCharset;
    private final Charset outputCharset;
    private int fixedLength = -1;

    public FileConverter(Charset inputCharset, Charset outputCharset) {
        this.inputCharset = inputCharset;
        this.outputCharset = outputCharset;
    }

    public void setFixedLength(int numberOfColumn) {
        this.fixedLength = numberOfColumn;
    }

    void convert(File inputFile, File outputFile) {
        Reader reader = null;
        Writer writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), inputCharset));
            int[] ebcdicInput = loadContent(reader);
            close(reader);
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), outputCharset));
            convert(ebcdicInput, writer);
        } catch (Exception e) {
            throw new ConverterException("Unable to convert file :: " + inputFile.getAbsolutePath(), e);
        } finally {
            close(writer);
        }
    }

    private void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            throw new ConverterException("Unable to close", e);
        }
    }

    void convert(String input, Writer convertedOutputWriter) throws IOException {
        convert(loadContent(new StringReader(input)), convertedOutputWriter);
    }

    private void convert(int[] ebcdicInput, Writer convertedOutputWriter) throws IOException {
        int convertedChar;
        for (int index = 0; index < ebcdicInput.length; index++) {
            int character = ebcdicInput[index];
            if (fixedLength != -1 && index > 0 && index % fixedLength == 0) {
                convertedOutputWriter.append((char) LF);
            }
            if (fixedLength == -1 && character == NEL) {
                convertedChar = LF;
            } else {
                convertedChar = replaceNonPrintableCharacterByWhitespace(character);
            }
            convertedOutputWriter.append((char) convertedChar);
        }
    }

    private int replaceNonPrintableCharacterByWhitespace(int character) {
        for (char nonPrintableChar : NON_PRINTABLE_EBCDIC_CHARS) {
            if (nonPrintableChar == (char) character) {
                return WS;
            }
        }
        return character;
    }

    private int[] loadContent(Reader reader) throws IOException {
        int[] buffer = new int[INITIAL_BUFFER_SIZE];
        int bufferIndex = 0;
        int bufferSize = buffer.length;
        int character;
        while ((character = reader.read()) != -1) {
            if (bufferIndex == bufferSize) {
                buffer = resizeArray(buffer, bufferSize + INITIAL_BUFFER_SIZE);
                bufferSize = buffer.length;
            }
            buffer[bufferIndex++] = character;
        }
        return resizeArray(buffer, bufferIndex);
    }

    final int[] resizeArray(int[] orignalArray, int newSize) {
        int[] resizedArray = new int[newSize];
        for (int i = 0; i < newSize && i < orignalArray.length; i++) {
            resizedArray[i] = orignalArray[i];
        }
        return resizedArray;
    }

    public void convertUsingChannel(File readFilePath, File writeFilePath) {
        Path readFile = Paths.get(readFilePath.toString());
        Path writeFile = Paths.get(writeFilePath.toString());
        try (SeekableByteChannel sbcForRead = Files.newByteChannel(readFile, EnumSet.of(READ)); SeekableByteChannel sbcForWrite = Files.newByteChannel(writeFile, EnumSet.of(CREATE, APPEND, WRITE))) {
            ByteBuffer readBuffer = ByteBuffer.allocate(1 << 5);
            ByteBuffer writeBuffer = ByteBuffer.allocate(1 << 5);
            readBuffer.clear();
            while (sbcForRead.read(readBuffer) > 0) {
                readBuffer.flip();
                System.out.print(inputCharset.decode(readBuffer));
                // writeBuffer =
                // ByteBuffer.wrap(inputCharset.decode(readBuffer).toString().getBytes(inputCharset));
                sbcForWrite.write(writeBuffer);
                readBuffer.clear();
                writeBuffer.clear();
            }
        } catch (IOException ioe) {
            throw new ConverterException("Unable to convert file :: " + readFilePath.getAbsolutePath(), ioe);
        }
    }

    void convertData(File inputFile, File outputFile) {
        Reader reader = null;
        Writer writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), inputCharset));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), outputCharset));
            loadContent(reader, writer);
        } catch (Exception e) {
            throw new ConverterException("Unable to convert file :: " + inputFile.getAbsolutePath(), e);
        } finally {
            close(reader);
            close(writer);
        }
    }

    public void loadContent(Reader reader, Writer writer) throws IOException {
        int character;
        while ((character = reader.read()) != -1) {
            if (fixedLength != -1) {
                writer.append((char) LF);
                System.out.println("=>>"+character);
            }
            if (fixedLength == -1 && character == NEL) {
                character = LF;
                System.out.println("<<="+character);
            } else {
                character = replaceNonPrintableCharacterByWhitespace(character);
            }
            writer.append((char) character);
        }
    }


}
