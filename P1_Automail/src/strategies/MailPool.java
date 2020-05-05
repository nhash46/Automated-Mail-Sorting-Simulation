package strategies;

import java.util.LinkedList;
import java.util.Comparator;
import java.util.ListIterator;

import automail.IMailDelivery;
import automail.MailItem;
import automail.Robot;
import exceptions.BreakingFragileItemException;
import exceptions.ItemTooHeavyException;

public class MailPool implements IMailPool {

	private class Item {
		int destination;
		MailItem mailItem;
		
		public Item(MailItem mailItem) {
			destination = mailItem.getDestFloor();
			this.mailItem = mailItem;
		}
	}
	
	public class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item i1, Item i2) {
			int order = 0;
			if (i1.destination > i2.destination) {  // Further before closer
				order = 1;
			} else if (i1.destination < i2.destination) {
				order = -1;
			}
			return order;
		}
	}
	
	private LinkedList<Item> pool;
	private LinkedList<Robot> robots;

	public MailPool(int nrobots){
		// Start empty
		pool = new LinkedList<Item>();
		robots = new LinkedList<Robot>();
	}

	public void addToPool(MailItem mailItem) {
		Item item = new Item(mailItem);
		pool.add(item);
		pool.sort(new ItemComparator());
	}
	
	@Override
	public void step() throws ItemTooHeavyException, BreakingFragileItemException {
		try{
			ListIterator<Robot> i = robots.listIterator();
			while (i.hasNext()) {
				loadRobot(i);
			}
		} catch (Exception e) { 
            throw e;
        }
	}
	
	private void loadRobot(ListIterator<Robot> i) throws ItemTooHeavyException, BreakingFragileItemException {
		Robot robot = i.next();
		assert(robot.isEmpty());
		// System.out.printf("P: %3d%n", pool.size());
		ListIterator<Item> j = pool.listIterator();
		boolean caution_mode = robot.isCautionMode();
		boolean fragile_mode = robot.isFragileMode();
		
		if( (fragile_mode == false) ) {
			if (pool.size() > 0) {
				try {
					robot.addToHand(j.next().mailItem); // hand first as we want higher priority delivered first
					j.remove();
					if (pool.size() > 0) {
						robot.addToTube(j.next().mailItem);
						j.remove();
					}
					robot.dispatch(); // send the robot off if it has any items to deliver
					i.remove();       // remove from mailPool queue
				}catch (Exception e) {
		            throw e; 
		        	}
			}
		}
		else {
			try {
				while ( (pool.size() > 0) ) {
					
					Item current = j.next();
					j.remove();
					boolean added = false;
					
					if(current.mailItem.getFragile() == true) {
						System.out.println("SPECIAL ITEM CAME IN HOT");
						if(caution_mode == false) {
							System.out.println(" -But rejected cause caution mode off");
							robot.getDelivery().reject(current.mailItem);
							continue;
						}
						else {
							if(robot.specialEmpty() == true) {
								robot.addToSpecialHand(current.mailItem);
								System.out.println("ADDED TO SPECIAL HAND");
								added = true;
							}
						}
					}
					
					else {
						if(robot.handEmpty() == true) {
							robot.addToHand(current.mailItem);
							
							if(caution_mode == true) {
								System.out.println("ADDED TO HAAAAAND");
							}
							
							added = true;
						}
						if( (robot.tubeEmpty() == true) && (added == false)) {
							robot.addToTube(current.mailItem);
							
							if(caution_mode == true) {
								System.out.println("ADDED TO TUUUUUUBE");
							}
							
							added = true;
						}
					}
					
					if(added == false) {
						pool.add(current);
						System.out.println("ADDED BACK TO POOL");
						break;
					}
					
				}
				robot.dispatch(); // send the robot off if it has any items to deliver
				i.remove();       // remove from mailPool queue 
			}catch (Exception e) {
	            throw e; 
        		}
		}
	}
	

	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

}
