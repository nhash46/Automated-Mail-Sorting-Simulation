package automail;

/**
 * a MailDelivery is used by the Robot to deliver mail once it has arrived at the correct location
 */
public interface IMailDelivery {

	/**
     * Delivers an item at its floor
     * @param mailItem the mail item being delivered.
     */
	void deliver(MailItem mailItem);
	
	
	/**
     * Rejects an item before loading to robot
     * @param mailItem the mail item being rejected.
     */
	void reject(MailItem mailItem);
    
}