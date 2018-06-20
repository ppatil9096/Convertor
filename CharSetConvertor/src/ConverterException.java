public class ConverterException extends RuntimeException {
    /*******************************************************
     * @author pravin patil 
     * @since  01-June-2018
     * This class is used throw an exceptions generated
     * during the file conversions
     *******************************************************/
    private static final long serialVersionUID = 1L;

    public ConverterException(String message, Throwable e) {
        super(message, e);
    }

    public ConverterException(String message) {
        super(message);
    }

}
