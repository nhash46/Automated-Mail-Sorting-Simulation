package exceptions;

/**
 * An exception thrown when a mail that is already delivered attempts to be delivered again.
 */
public class MailAlreadyRejectedException extends Throwable    {
    public MailAlreadyRejectedException(){
        super("This mail has already been rejected!");
    }
}
