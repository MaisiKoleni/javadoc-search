package net.maisikoleni.javadoc.util;

import static java.lang.invoke.VarHandle.AccessMode.COMPARE_AND_EXCHANGE;
import static java.lang.invoke.VarHandle.AccessMode.COMPARE_AND_SET;
import static java.lang.invoke.VarHandle.AccessMode.GET_AND_ADD;
import static java.lang.invoke.VarHandle.AccessMode.GET_AND_BITWISE_OR;
import static java.lang.invoke.VarHandle.AccessMode.GET_AND_SET;
import static java.lang.invoke.VarHandle.AccessMode.GET_VOLATILE;
import static java.lang.invoke.VarHandle.AccessMode.SET_VOLATILE;

import java.lang.invoke.VarHandle;
import java.lang.invoke.VarHandle.AccessMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High performance minimal lock for extremely short-lived tasks that locks on a
 * primitive <code>int</code> field supplied as {@link VarHandle} using busy
 * waiting.
 * <p>
 * The instances of this class are intended to be shared to allow for a minimal
 * memory footprint when a large number of locks is needed.
 *
 * @param <T> the owner type of the lock <code>int</code> field.
 * @see Thread#onSpinWait()
 */
public final class PrimitiveReentrantReadWriteLock<T> {

	private static final Logger LOG = LoggerFactory.getLogger(PrimitiveReentrantReadWriteLock.class);

	private static final int LOCK_UNUSED = 0;
	private static final int LOCK_INACTIVE_OFFSET = 31;
	private static final int LOCK_INACTIVE = 1 << LOCK_INACTIVE_OFFSET;
	private static final int LOCK_WRITE_OFFSET = 30;
	private static final int LOCK_WRITE = 1 << LOCK_WRITE_OFFSET;
	private static final int LOCK_COORD_OFFSET = 29;
	private static final int LOCK_COORD = 1 << LOCK_COORD_OFFSET;
	private static final int READ_COORD_RESET_THRESHOLD = 1 << (LOCK_COORD_OFFSET - 3);
	private static final int LOCK_READS_MASK = LOCK_COORD - 1;

	private static final AccessMode[] REQUIREMENTS = { GET_VOLATILE, COMPARE_AND_SET, GET_AND_ADD, GET_AND_BITWISE_OR,
			SET_VOLATILE, COMPARE_AND_EXCHANGE, GET_AND_SET };

	private final VarHandle intLock;

	public PrimitiveReentrantReadWriteLock(VarHandle intLock) {
		this.intLock = intLock;
		if (intLock.varType() != int.class)
			throw new IllegalArgumentException("VarHandle intLock must refer to a field of type int");
		for (var requirement : REQUIREMENTS)
			if (!intLock.isAccessModeSupported(requirement))
				throw new IllegalArgumentException("VarHandle intLock must support " + requirement);
	}

	private boolean lockInactive(T holder) {
		return (int) intLock.getVolatile(holder) == LOCK_INACTIVE;
	}

	public void setInactive(T holder, boolean value) {
		if (value) {
			if (!intLock.compareAndSet(holder, LOCK_UNUSED, LOCK_INACTIVE))
				throw new IllegalStateException("cannot set lock inactive while in use");
		} else if (!intLock.compareAndSet(holder, LOCK_INACTIVE, LOCK_UNUSED))
			throw new IllegalStateException("lock already active");
	}

	public void startRead(T holder) {
		if (lockInactive(holder))
			return;
		int value;
		// try to acquire an unused lock be incrementing the readers
		// continuously while the write lock might be held
		while ((value = (int) intLock.getAndAdd(holder, 1)) >= LOCK_WRITE) {
			// as this read attempt failed, check how many read attempts there are
			var readAttempts = value & LOCK_READS_MASK;
			// should the read attempts threaten to overflow into the write flag
			if (readAttempts > READ_COORD_RESET_THRESHOLD) {
				// try to acquire the coordination flag
				int coordResult = (int) intLock.getAndBitwiseOr(holder, LOCK_COORD);
				// if it was 0, we are now permitted to change the lock, endWrite() will wait
				// the write flag must still be set
				if ((coordResult & LOCK_COORD) == 0 && (coordResult & LOCK_WRITE) != 0) {
					// so we can reset the lock to the write flag only
					intLock.setVolatile(holder, LOCK_WRITE);
					// now the coordination flag as well as all read attempts are cleared
					LOG.debug("Lock successfully reset, {} read attemps occurred", readAttempts);
				}
			}
			Thread.onSpinWait();
		}
	}

	public void endRead(T holder) {
		if (lockInactive(holder))
			return;
		// remove the reader by decrementing the reader count
		int lockBeforeRelese = (int) intLock.getAndAdd(holder, -1);
		// debug/error code
		if (lockBeforeRelese <= 0)
			throw new IllegalStateException("endRead(): negative amount of readers");
	}

	public void startWrite(T holder) {
		if (lockInactive(holder))
			return;
		// wait for the lock to be unused
		while ((int) intLock.compareAndExchange(holder, LOCK_UNUSED, LOCK_WRITE) != LOCK_UNUSED)
			Thread.onSpinWait();
	}

	public void endWrite(T holder) {
		if (lockInactive(holder))
			return;
		// wait for potential lock reset by readers to be finished
		while (((int) intLock.getAndBitwiseOr(holder, LOCK_COORD) & LOCK_COORD) != 0)
			Thread.onSpinWait();
		// reset everything, including the write flag
		int lockBeforeRelese = (int) intLock.getAndSet(holder, LOCK_UNUSED);
		// debug/error code
		if (lockBeforeRelese < LOCK_WRITE)
			throw new IllegalStateException("endWrite(): no write lock, too many read requests");
		int readAttempts = lockBeforeRelese & LOCK_READS_MASK;
		if (readAttempts > READ_COORD_RESET_THRESHOLD)
			LOG.debug("Many read attemps after write finished: {}", readAttempts);
	}
}
