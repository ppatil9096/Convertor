import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**********************************************************
     * @author pravin patil                               *    
     * @since  01-June-2018                               * 
     * This class is used convert the file from one format* 
     * to another provided format                         *  
 **********************************************************/
public final class MainClass {

    private static final String INPUT_CHARSET_DEFAULT = "CP1047";
    private static final int FIXED_LENGTH_DEFAULT = -1;
    private int fixedLength = FIXED_LENGTH_DEFAULT;
    private File source = null;
    private File destination = null;

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        String sourceDir = "/home/ppatil/Desktop/ebdec/s";
        String destinationDir = "/home/ppatil/Desktop/ebdec/d";
        String originalDir = "/home/ppatil/Desktop/ebdec/o";
//        new MainClass().parseArguments(sourceDir, destinationDir).convertEbdecToAscii(charsetForName(INPUT_CHARSET_DEFAULT), Charset.defaultCharset());
        new MainClass().parseArguments(sourceDir, destinationDir).convertEbdecToAscii(Charset.defaultCharset(), Charset.defaultCharset());
        new MainClass().parseArguments(destinationDir, originalDir).convertAsciiToEbdec(Charset.defaultCharset(), charsetForName(INPUT_CHARSET_DEFAULT));
        long endTime = System.nanoTime();
        long elapsedTimeInMillis = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
        System.out.println("Total elapsed time: " + elapsedTimeInMillis + " ms");
    }

    private MainClass() {
    }

    private MainClass parseArguments(String sourceDir, String destinationDir) {
        File sourceFile = new File(sourceDir);
        File destinationFile = new File(destinationDir);
        if (source == null) {
            source = sourceFile;
            if (!source.isDirectory()) {
                printError("No such directory :: " + source);
            }
        }
        if (destination == null) {
            destination = destinationFile;
            if (!destination.isDirectory()) {
                printError("No such directory :: " + destination);
            }
        }
        if (source == null) {
            printError("Missing source directory");
        }
        if (destination == null) {
            printError("Missing destination directory");
        }
        return this;
    }

    private void convertEbdecToAscii(Charset EbdecCharset, Charset AsciiCharset) {
        try {
            FileConverter converter = new FileConverter(EbdecCharset, AsciiCharset);
            converter.setFixedLength(fixedLength);
            List<String> files = listFiles(source);
            for (String s : files) {
                File sourceFile = new File(source, s);
                File destFile = new File(destination, s);
                log("Converting => " + sourceFile + " into => " + destFile);
                destFile.getParentFile().mkdirs();
                // converter.convert(sourceFile, destFile);
                converter.convertData(sourceFile, destFile);
                // converter.convertUsingChannel(sourceFile, destFile);
            }
            log("SUCCESS");
        } catch (ConverterException e) {
            log("Unable to convert files :: ", e);
            log("FAILURE");
        }
    }

    private void convertAsciiToEbdec(Charset AsciiCharset, Charset EbdecCharset) {
        try {
            FileConverter converter = new FileConverter(AsciiCharset, EbdecCharset);
            converter.setFixedLength(fixedLength);

            List<String> files = listFiles(source);
            for (String s : files) {
                File sourceFile = new File(source, s);
                File destFile = new File(destination, s);
                log("Converting => " + sourceFile + " into => " + destFile);
                destFile.getParentFile().mkdirs();
                // converter.convert(sourceFile, destFile);
                converter.convertData(sourceFile, destFile);
            }
            log("SUCCESS");
        } catch (ConverterException e) {
            log("Unable to convert files :: ", e);
            log("FAILURE");
        }
    }

    private static Charset charsetForName(String charsetName) {
        try {
            return Charset.forName(charsetName);
        } catch (Exception e) {
            printError("Unknown charset :: " + charsetName);
            throw new ConverterException("'" + charsetName + "' is an unknown charset", e);
        }
    }

    private static List<String> listFiles(File dir) {
        List<String> files = new ArrayList<String>();
        recursivelyListFiles(dir, "", files);
        return files;
    }

    private static void recursivelyListFiles(File dir, String relativePath, List<String> files) {
        for (String s : dir.list()) {
            String path = relativePath + File.separator + s;
            File file = new File(dir, s);
            if (file.isFile() && !file.isHidden()) {
                files.add(path);
            } else if (file.isDirectory() && !file.isHidden()) {
                recursivelyListFiles(file, path, files);
            }
        }
    }

    private static void printError(String message) {
        log(message);
    }

    private static void log(String message) {
        System.out.println(message);
    }

    private static void log(String message, Throwable e) {
        System.out.println(message + " :: " + e);
    }

}
