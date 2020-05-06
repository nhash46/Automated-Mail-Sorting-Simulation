package automail;
import java.util.Properties;

import exceptions.BreakingFragileItemException;
import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import strategies.Automail;
import strategies.IMailPool;
import java.util.Map;
import java.util.TreeMap;

/**
 * The robot delivers mail!
 */
public class Robot {
	
    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;

    private IMailDelivery delivery;
    protected final String id;
    /** Possible states the robot can be in */
    public enum RobotState { DELIVERING, WAITING, RETURNING, WRAP_STAGE_1, WRAP_STAGE_2, DELIVER_FRAGILE, HOLD_POS }
    public RobotState current_state;
    private int current_floor;
    private int destination_floor;
    private IMailPool mailPool;
    private boolean receivedDispatch;
    
    private boolean CAUTION_ENABLED;
    private boolean FRAGILE_ENABLED;
    
    private MailItem deliveryItem = null;
    private MailItem tube = null;
    private MailItem specialHand = null;
    
    private int deliveryCounter;
    

    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     * @param behaviour governs selection of mail items for delivery and behaviour on priority arrivals
     * @param delivery governs the final delivery
     * @param mailPool is the source of mail items
     */
    public Robot(IMailDelivery delivery, IMailPool mailPool, Properties automailProperties){
    	id = "R" + hashCode();
        // current_state = RobotState.WAITING;
    	current_state = RobotState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        this.delivery = delivery;
        this.mailPool = mailPool;

        this.receivedDispatch = false;
        this.deliveryCounter = 0;
        this.CAUTION_ENABLED = Boolean.parseBoolean(automailProperties.getProperty("Caution"));
        this.FRAGILE_ENABLED = Boolean.parseBoolean(automailProperties.getProperty("Fragile"));
    }
    
    public void dispatch() {
    	receivedDispatch = true;
    }

    /**
     * This is called on every time step
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public void step() throws ExcessiveDeliveryException {    	
    	switch(current_state) {
    		/** This state is triggered when the robot is returning to the mailroom after a delivery */
    		case RETURNING:
    			/** If its current position is at the mailroom, then the robot should change state */
                if(current_floor == Building.MAILROOM_LOCATION){
                	if (tube != null) {
                		mailPool.addToPool(tube);
                        System.out.printf("T: %3d >  +addToPool [%s]%n", Clock.Time(), tube.toString());
                        tube = null;
                	}
        			/** Tell the sorter the robot is ready */
        			mailPool.registerWaiting(this);
                	changeState(RobotState.WAITING);
                } else {
                	/** If the robot is not at the mailroom floor yet, then move towards it! */
                    moveTowards(Building.MAILROOM_LOCATION);
                	break;
                }
    		case WAITING:
                /** If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
                if(!isEmpty() && receivedDispatch){
                	System.out.println("Caution enabled in Robot: " + CAUTION_ENABLED);
                	receivedDispatch = false;
                	deliveryCounter = 0; // reset delivery counter
        			setRoute();
                	changeState(RobotState.DELIVERING);
                }
                break;
    		case DELIVERING:
    			if( (specialHand != null) && (specialHand.getWrapped() == false) ) {
    				//changeWrapState(RobotState.WRAP_STAGE_1);
    				System.out.println("FIRST DELIVERING IF STATEMENT");
    				changeState(RobotState.WRAP_STAGE_1);
    				break;
    			}
    			if(current_floor == destination_floor){ // If already here drop off either way
    				if(specialHand != null) {
    					changeState(RobotState.DELIVER_FRAGILE);
    					break;
    				}
                    /** Delivery complete, report this to the simulator! */
                    delivery.deliver(deliveryItem);
                    deliveryItem = null;
                    deliveryCounter++;
                    if(deliveryCounter > 3){  // Implies a simulation bug
                    	throw new ExcessiveDeliveryException();
                    }
                    /** Check if want to return, i.e. if there is no item in the tube*/
                    if(tube == null){
                    	changeState(RobotState.RETURNING);
                    }
                    else{
                        /** If there is another item, set the robot's route to the location to deliver the item */
                        deliveryItem = tube;
                        tube = null;
                        setRoute();
                        changeState(RobotState.DELIVERING);
                    }
    			} else {
	        		/** The robot is not at the destination yet, move towards it! */
	                moveTowards(destination_floor);
    			}
                break;
    		case WRAP_STAGE_1:
    			changeState(RobotState.WRAP_STAGE_2);
    			break;
    		case WRAP_STAGE_2:
    			wrapItem(specialHand);
    			Automail.frag_floors[getRobotNumber()] = destination_floor;
    			changeState(RobotState.DELIVERING);
    			break;
    		case DELIVER_FRAGILE:
    			assert(specialHand.isWrapped);
    			unwrapItem(specialHand);
				delivery.deliver(specialHand);
				specialHand = null;
				deliveryCounter++;
				Automail.frag_floors[getRobotNumber()] = -1;
				if(deliveryItem != null) {
					setRoute();
					changeState(RobotState.DELIVERING);
				}
    			changeState(RobotState.RETURNING);
    			break;
    		case HOLD_POS:
    			changeState(RobotState.DELIVER_FRAGILE);
    			break;
    	}
    }

    /**
     * Sets the route for the robot
     */
    private void setRoute() {
        /** Set the destination floor */
    	if(specialHand != null){
    		destination_floor = specialHand.getDestFloor();
    	}
    	else{
    		destination_floor = deliveryItem.getDestFloor();
    	}
    }

    /**
     * Generic function that moves the robot towards the destination
     * @param destination the floor towards which the robot is moving
     */
    /*private void moveTowards(int destination) {
        if(current_floor < destination){
            current_floor++;
        } else {
            current_floor--;
        }
    }*/
    
    private void moveTowards(int destination) {
    	/*for(int i = 0; i<Automail.num_robots; i++) {
    		System.out.println(Automail.frag_floors[i]);
    	}
    	System.out.println(priority);*/
        if(current_floor < destination){
        	current_floor++;
        	if(fragileCollision(current_floor)) {
        		current_floor--;
        		System.out.println("Collision immenent, need to wait");
        		return;
        	}
        }
        else {
        	current_floor--;
        	if(fragileCollision(current_floor)) {
        		current_floor++;
        		System.out.println("Collision immenent, need to wait");
        		return;
        	}
        }
    }
    
    /**
     * Checks if inputed floor is an/will be floor that has a robot with a fragile item
     * @param floor we are checking
     * @return true if floor is a fragile floor
     */
    private boolean fragileCollision(int floor_num) {
    	for(int i = 0; i < Automail.num_robots; i++) {
    		if( (Automail.frag_floors[i] == floor_num) && (getRobotNumber() != i) ) {
    			Robot collision = Automail.robots[i];
    			System.out.println("ROBOT "+ getRobotNumber()+ " GOING TO FLOOR "
    					+ getDestination() +" POSSIBLE COLLISION WITH ROBOT " + i + " GOING TO FLOOR " +
    					collision.getDestination());
    			System.out.println("ROBOT "+collision.getRobotNumber()+" POSITION " + collision.getPosition());
    			System.out.println("ROBOT "+collision.getRobotNumber()+" DESTINATION " + collision.getDestination());
    			System.out.println("ROBOT "+collision.getRobotNumber()+" NEXT POSITION " + nextMove(collision.getDestination()));
    			System.out.println("ROBOT "+getRobotNumber()+" POSITION " + getPosition());
    			System.out.println("ROBOT "+getRobotNumber()+" DESTINATION " + getDestination());
    			System.out.println("ROBOT "+getRobotNumber()+" NEXT POSITION " + nextMove(getDestination()));
    			if(nextMove(collision.getDestination()) == floor_num) {
    				if(getRobotNumber() < collision.getRobotNumber()) {
    					System.out.println("ROBOT " +getRobotNumber()+ " GETS PRIORITY");
    					continue;
    				}
    				
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    /**
     * Predicts next move of a robot with no restrictions
     * @param destination of a robot
     * @return would be next move of a robot
     */
    public int nextMove(int destination) {
    	if(current_floor < destination){
            return current_floor + 1;
        }
    	else if(current_floor > destination) {
        	return current_floor - 1;
        }
    	else {
    		return current_floor;
    	}
    }
    
    

    
    private String getIdTube() {
    	return String.format("%s(%1d)", id, (tube == null ? 0 : 1));
    }
    
    
    /**
     * Prints out the change in state
     * @param nextState the state to which the robot is in transition
     */
    private void changeState(RobotState nextState){
    	assert(!(deliveryItem == null && tube != null));
    	if (current_state != nextState) {
            System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), current_state, nextState);
    	}
    	current_state = nextState;
    	if( (nextState == RobotState.DELIVERING) && (specialHand == null) ){
            System.out.printf("T: %3d > %9s-> [%s]%n", Clock.Time(), getIdTube(), deliveryItem.toString());
    	}
    	
    	if( (nextState == RobotState.DELIVERING) && !(specialHand == null) ){
            System.out.printf("T: %3d > %9s-> [%s]%n", Clock.Time(), getIdTube(), specialHand.toString());
    	}
    }
    
    
	static private int count = 0;
	static private Map<Integer, Integer> hashMap = new TreeMap<Integer, Integer>();

	@Override
	public int hashCode() {
		Integer hash0 = super.hashCode();
		Integer hash = hashMap.get(hash0);
		if (hash == null) { hash = count++; hashMap.put(hash0, hash); }
		return hash;
	}
	
	public int getRobotNumber() {
		return Integer.parseInt(id.substring(id.length() - 1));
	}
	
	public int getDestination() {
		return destination_floor;
	}
	
	/**
	 * @return current floor of robot
	 */
	public int getPosition() {
		return destination_floor;
	}
	
	public boolean isCautionMode() {
		return CAUTION_ENABLED;
	}
	
	public boolean isFragileMode() {
		return FRAGILE_ENABLED;
	}
	
	public IMailDelivery getDelivery() {
		return delivery;
	}

	public boolean isEmpty() {
		return (handEmpty() == true && tubeEmpty() == true && specialEmpty() == true);
	}
	
	public boolean spaceLeft() {
		return (handEmpty() == true || tubeEmpty() == true || specialEmpty() == true);
	}
	
	public boolean handEmpty() {
		return deliveryItem == null;
	}
	
	public boolean tubeEmpty() {
		return tube == null;
	}
	
	public boolean specialEmpty() {
		return specialHand == null;
	}
	

	public void addToHand(MailItem mailItem) throws ItemTooHeavyException, BreakingFragileItemException {
		assert(deliveryItem == null);
		if(mailItem.fragile) throw new BreakingFragileItemException();
		deliveryItem = mailItem;
		if (deliveryItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}

	public void addToTube(MailItem mailItem) throws ItemTooHeavyException, BreakingFragileItemException {
		assert(tube == null);
		if(mailItem.fragile) throw new BreakingFragileItemException();
		tube = mailItem;
		if (tube.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}
	
	public void addToSpecialHand(MailItem mailItem) throws ItemTooHeavyException {
		assert(specialHand == null);
		specialHand = mailItem;
		if (specialHand.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}
	
	public void wrapItem(MailItem mailItem) {
		assert((mailItem.isWrapped == false) && (mailItem.fragile));
		System.out.println("WRAPPING ITEM");
		mailItem.isWrapped = true;
	}
	
	public void unwrapItem(MailItem mailItem) {
		assert((mailItem.isWrapped == true) && (mailItem.fragile));
		System.out.println("UNWRAPPING ITEM");
		mailItem.isWrapped = false;
	}
	
}
