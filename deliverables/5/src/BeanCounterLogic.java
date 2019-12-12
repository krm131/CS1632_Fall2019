import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import gov.nasa.jpf.vm.Verify;

/**
 * Code by @author Wonsun Ahn
 * 
 * <p>BeanCounterLogic: The bean counter, also known as a quincunx or the Galton
 * box, is a device for statistics experiments named after English scientist Sir
 * Francis Galton. It consists of an upright board with evenly spaced nails (or
 * pegs) in a triangular form. Each bean takes a random path and falls into a
 * slot.
 *
 * <p>Beans are dropped from the opening of the board. Every time a bean hits a
 * nail, it has a 50% chance of falling to the left or to the right. The piles
 * of beans are accumulated in the slots at the bottom of the board.
 * 
 * <p>This class implements the core logic of the machine. The MainPanel uses the
 * state inside BeanCounterLogic to display on the screen.
 * 
 * <p>Note that BeanCounterLogic uses a logical coordinate system to store the
 * positions of in-flight beans.For example, for a 4-slot machine: (0, 0) (1, 0)
 *                      (0, 0)
 *               (0, 1)        (1, 1)
 *        (0, 2)        (1, 2)        (2, 2)
 *  (0,3)         (1,3)         (2,3)        (3,3)
 * [Slot0]       [Slot1]       [Slot2]      [Slot3]
 */

public class BeanCounterLogic {

	int slotCount;
	
	int[] slotBeanCount;
	
	Bean[] row;

	Bean[] beans;
	
	LinkedList<Bean> beansInSlots;
	
	int nextBean;

	
	// No bean in that particular Y coordinate
	public static final int NO_BEAN_IN_YPOS = -1;

	/**
	 * Constructor - creates the bean counter logic object that implements the core
	 * logic. Our bean counter should start with a single bean at the top.
	 * 
	 * @param slotCount the number of slots in the machine
	 */
	BeanCounterLogic(int inSlotCount) {
		slotCount = inSlotCount;
		row = new Bean[slotCount];
		slotBeanCount = new int[slotCount];
		beansInSlots = new LinkedList<Bean>();
	}

	/**
	 * Returns the number of beans remaining that are waiting to get inserted.
	 * 
	 * @return number of beans remaining
	 */
	public int getRemainingBeanCount() {
		return beans.length - nextBean;
	}

	/**
	 * Returns the x-coordinate for the in-flight bean at the provided y-coordinate.
	 * 
	 * @param yPos the y-coordinate in which to look for the in-flight bean
	 * @return the x-coordinate of the in-flight bean
	 */
	public int getInFlightBeanXPos(int yPos) {
		if (row[yPos] == null) {
			return NO_BEAN_IN_YPOS;
		} else {
			return row[yPos].getXPos();
		}
	}

	/**
	 * Returns the number of beans in the ith slot.
	 * 
	 * @param i index of slot
	 * @return number of beans in slot
	 */
	public int getSlotBeanCount(int i) {
		return slotBeanCount[i];
	}

	/**
	 * Calculates the average slot bean count.
	 * 
	 * @return average of all slot bean counts
	 */
	public double getAverageSlotBeanCount() {
		double total = 0;
		double beanCount = 0;
		for (int i = 0; i < slotCount; i++) {
			total += slotBeanCount[i] * i;
			beanCount += slotBeanCount[i];
		}
		if (beanCount == 0) {
			return 0;
		}
		return total / beanCount;

	}

	/**
	 * Removes the lower half of all beans currently in slots, keeping only the
	 * upper half.
	 */
	public void upperHalf() {
		int count = beansInSlots.size() / 2;
		Collections.sort(beansInSlots); //ascending
		for (int i = 0; i <= count; i++) {
			slotBeanCount[beansInSlots.remove().getXPos()]--;
		}
		
	}

	/**
	 * Removes the upper half of all beans currently in slots, keeping only the
	 * lower half.
	 */
	public void lowerHalf() {
		int count = beansInSlots.size() / 2;
		Collections.sort(beansInSlots); //ascending
		for (int i = 0; i < count; i++) {
			slotBeanCount[beansInSlots.removeLast().getXPos()]--;
		}
	}

	/**
	 * A hard reset. Initializes the machine with the passed beans. The machine
	 * starts with one bean at the top.
	 */
	public void reset(Bean[] inBeans) {
		beans = inBeans;
		for (Bean b: beans) {
			b.resetSkill();
		}
		if (beans.length > 0) {
			row[0] = beans[0];
			row[0].setXPos(0);
			nextBean = 1;
		} else {
			nextBean = 0;
		}
		slotBeanCount = new int[slotCount];
	}

	/**
	 * Repeats the experiment by scooping up all beans in the slots and all beans
	 * in-flight and adding them into the pool of remaining beans. As in the
	 * beginning, the machine starts with one bean at the top.
	 */
	public void repeat() {
		
		for (Bean b: row) {
			if (b != null) {
				beansInSlots.add(b);
			}
		}
		for (int i = nextBean; i < beans.length; i++) {
			beansInSlots.add(beans[i]);
		}
		for (Bean b: beansInSlots) {
			b.resetSkill();
			b.setXPos(-1);
		}
		
		beans = beansInSlots.toArray(new Bean[beansInSlots.size()]);
		beansInSlots = new LinkedList<Bean>();
		row = new Bean[slotCount];
		slotBeanCount = new int[slotCount];
		row[0] = beans[0];
		row[0].setXPos(0);
		nextBean = 1;
		
	}

	/**
	 * Advances the machine one step. All the in-flight beans fall down one step to
	 * the next peg. A new bean is inserted into the top of the machine if there are
	 * beans remaining.
	 * 
	 * @return whether there has been any status change. If there is no change, that
	 *         means the machine is finished.
	 */
	public boolean advanceStep() {
		boolean change = false;
		
		if (row[slotCount - 1] != null) {
			change = true;
			slotBeanCount[row[slotCount - 1].getXPos()]++;
			beansInSlots.add(row[slotCount - 1]);
			row[slotCount - 1] = null;
		}
		for (int i = slotCount - 1; i >= 0; i--) {
			if (row[i] != null) {
				change = true;
				row[i + 1] = row[i];
				row[i + 1].move();
				row[i] = null;
			}
		}
		if (nextBean != beans.length) {
			row[0] = beans[nextBean++];
			row[0].setXPos(0);
		}
		
		return change;
	}

	public static void showUsage() {
		System.out.println("Usage: java BeanCounterLogic <number of beans> <luck | skill>");
		System.out.println("Example: java BeanCounterLogic 400 luck");
	}

	/**
	 * Auxiliary main method. Runs the machine in text mode with no bells and
	 * whistles. It simply shows the slot bean count at the end. Also, when the
	 * string "test" is passed to args[0], the program enters test mode. In test
	 * mode, the Java Pathfinder model checking tool checks the logic of the machine
	 * for a small number of beans and slots.
	 * 
	 * @param args args[0] is an integer bean count, args[1] is a string which is
	 *             either luck or skill.
	 */
	public static void main(String[] args) {
		boolean luck;
		int beanCount = 0;
		int slotCount = 0;

		if (args.length == 1 && args[0].equals("test")) {
			// TODO: Verify the model checking passes for beanCount values 0-3 and slotCount
			// values 1-5 using the JPF Verify API.
			beanCount = Verify.getInt(0, 3);
			slotCount = Verify.getInt(1,5);
			
			// Create the internal logic
			BeanCounterLogic logic = new BeanCounterLogic(slotCount);
			// Create the beans (in luck mode)
			Bean[] beans = new Bean[beanCount];
			for (int i = 0; i < beanCount; i++) {
				beans[i] = new Bean(true, new Random());
			}
			// Initialize the logic with the beans
			logic.reset(beans);

			while (true) {
				if (!logic.advanceStep()) {
					break;
				}

				// Checks invariant property: all positions of in-flight beans have to be
				// legal positions in the logical coordinate system.
				for (int yPos = 0; yPos < slotCount; yPos++) {
					int xPos = logic.getInFlightBeanXPos(yPos);
					assert xPos == BeanCounterLogic.NO_BEAN_IN_YPOS || (xPos >= 0 && xPos <= yPos);
				}

				// TODO: Check invariant property: the sum of remaining, in-flight, and in-slot
				// beans always have to be equal to beanCount
				
			}
			// TODO: Check invariant property: when the machine finishes,
			// 1. There should be no remaining beans.
			// 2. There should be no beans in-flight.
			// 3. The number of in-slot beans should be equal to beanCount.
			
			return;
		}

		if (args.length != 2) {
			showUsage();
			return;
		}

		try {
			beanCount = Integer.parseInt(args[0]);
		} catch (NumberFormatException ne) {
			showUsage();
			return;
		}
		if (beanCount < 0) {
			showUsage();
			return;
		}

		if (args[1].equals("luck")) {
			luck = true;
		} else if (args[1].equals("skill")) {
			luck = false;
		} else {
			showUsage();
			return;
		}
		
		slotCount = 10;

		// Create the internal logic
		BeanCounterLogic logic = new BeanCounterLogic(slotCount);
		// Create the beans (in luck mode)
		Bean[] beans = new Bean[beanCount];
		for (int i = 0; i < beanCount; i++) {
			beans[i] = new Bean(luck, new Random());
		}
		// Initialize the logic with the beans
		logic.reset(beans);
					
		// Perform the experiment
		while (true) {
			if (!logic.advanceStep()) {
				break;
			}
		}
		// display experimental results
		System.out.println("Slot bean counts:");
		for (int i = 0; i < slotCount; i++) {
			System.out.print(logic.getSlotBeanCount(i) + " ");
		}
		System.out.println("");
	}
}
