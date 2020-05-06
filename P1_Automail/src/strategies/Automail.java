package strategies;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import automail.IMailDelivery;
import automail.Robot;

public class Automail {
	      
    public static Robot[] robots;
    public static int num_robots;
    public IMailPool mailPool;
    public List<Integer> lockedFloors = new ArrayList<Integer>();
    
    public Automail(IMailPool mailPool, IMailDelivery delivery, int numRobots, Properties automailProperties) {
    	// Swap between simple provided strategies and your strategies here
    	    	
    	/** Initialize the MailPool */
    	
    	this.mailPool = mailPool;
    	
    	/** Initialize robots */
    	num_robots = numRobots;
    	robots = new Robot[num_robots];
    	for (int i = 0; i < numRobots; i++) robots[i] = new Robot(delivery, mailPool, automailProperties);
    }
    
}
