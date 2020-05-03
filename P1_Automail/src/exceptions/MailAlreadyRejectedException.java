package exceptions;

/**
 * An exception thrown when a mail that is already rejected attempts to be rejected again.
 */
public class MailAlreadyRejectedException extends Throwable    {
    public MailAlreadyRejectedException(){
        super("This mail has already been rejected!");
    }
}
