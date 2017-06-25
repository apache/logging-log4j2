/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.ByteBufferDestinationHelper;
import org.apache.logging.log4j.core.layout.LockableByteBufferDestination;
import org.apache.logging.log4j.core.layout.TextEncoderHelper;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.FileUtils;

//Lines too long...
//CHECKSTYLE:OFF
/**
 * This class maps a region of a file into memory and writes to this memory region.
 * <p>
 *
 * @see <a href="http://www.codeproject.com/Tips/683614/Things-to-Know-about-Memory-Mapped-File-in-Java">
 *      http://www.codeproject.com/Tips/683614/Things-to-Know-about-Memory-Mapped-File-in-Java</a>
 * @see <a href="http://bugs.java.com/view_bug.do?bug_id=6893654">http://bugs.java.com/view_bug.do?bug_id=6893654</a>
 * @see <a href="http://bugs.java.com/view_bug.do?bug_id=4724038">http://bugs.java.com/view_bug.do?bug_id=4724038</a>
 * @see <a
 *      href="http://stackoverflow.com/questions/9261316/memory-mapped-mappedbytebuffer-or-direct-bytebuffer-for-db-implementation">
 *      http://stackoverflow.com/questions/9261316/memory-mapped-mappedbytebuffer-or-direct-bytebuffer-for-db-implementation</a>
 *
 * @since 2.1
 */
//CHECKSTYLE:ON
public class MemoryMappedFileManager extends ByteBufferDestinationManager implements LockableByteBufferDestination {

    /**
     * Default length of region to map.
     */
    static final int DEFAULT_REGION_LENGTH = 32 * 1024 * 1024;

    /**
     * This threshold needs to be as small as possible to minimize possible spin time in {@link
     * Region#getStableBufferWatermark()}, on the other hand, we don't want logging of "normal" entries to require
     * acquiring a lock. 32 KB is chosen as the longest reasonable stack trace (300 frames * 100 symbols in a frame),
     * because stack traces appear in log entries often.
     */
    private static final int SPIN_WAIT_MINIMIZING_LOCKED_WRITE_THRESHOLD = 32 * 1024;

    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final int MAX_REMAP_COUNT = 10;
    private static final MemoryMappedFileManagerFactory FACTORY = new MemoryMappedFileManagerFactory();
    private static final double NANOS_PER_MILLISEC = 1000.0 * 1000.0;

    /**
     * Region is a wrapped ByteBuffer of {@link #regionLength} (actually may vary - see {@link #createNextRegion}) with
     * the fields and state to support lock-free concurrent writes.
     *
     * The structure of the Region's 64-bit {@link #state}:
     *
     * Bits 0-31: the "buffer watermark" is the offset in the region's {@link #mappedBuffer} to the byte after the last
     * written byte of data. Always a positive number, so the 31th bit should always be zero. Lock-free concurrent
     * writes to the {@link #mappedBuffer} are enabled by atomic bumps of the buffer watermark, so that each writer
     * "reserves" a range of bytes in the {@link #mappedBuffer} for the data that it is going to write. Buffer watermark
     * is bumped using wait-free {@link AtomicLong#addAndGet(long)} (in {@link #tryWrite} methods), so buffer watermark
     * overflow beyond the {@link #mappedBuffer}'s capacity is possible under contention between writers. Writers should
     * check for overflow and "rollback" the buffer watermark if needed, that is done in {@link
     * #checkStateAfterUpdatingAndRollbackIfNeeded}. Usage of wait-free operations to update the state also makes
     * starvation possible, this is addressed in {@link #checkStateBeforeUpdating}. Starvation is discussed in details
     * in the comments in the {@link #checkStateBeforeUpdating} method. See also comments in {@link #writeBytes}.
     *
     * Bits 32-61: the number of concurrent data writers to the buffer. Each writer registers and de-registers itself by
     * incrementing and decrementing the number in this 30-bit area of the state. Increment is combined with the buffer
     * watermark bump in a single wait-free {@link AtomicLong#addAndGet(long)} call in {@link
     * #bumpBufferWatermarkAndIncrementConcurrentWriterCount}.
     *
     * Bit 62: the "locked" flag. This flag is used to notify concurrent writers (1) that some thread wants to write a
     * large chunk of data (see {@link #lengthThresholdForExclusiveWrite}), or some continuous data via several separate
     * {@link #writeBytes} calls (see {@link #getDestinationLock()} and {@link TextEncoderHelper}). The thread that
     * wants to write exclusively obtains the "real" buffer watermark via {@link #getStableBufferWatermark}, and to
     * avoid starvation in that method, all concurrent lock-free writers should be ruled out in {@link
     * #checkStateBeforeUpdating} (1). Lock-free writers are also prohibited later, while the exclusive write is
     * ongoing (2). The "locked" flag serves for the both purposes (1) and (2). See {@link #tryLock()}, {@link
     * #unlock()}, {@link DestinationLock} and the places where these methods are used.
     *
     * Bit 63: the "closed" flag. The first purpose of the "closed" flag is similar to the purposes of the "locked"
     * flag: to rule out lock-free writers, when this Region is "finalized" and the {@link #region} field of the
     * parental MemoryMappedFileManager points to a newer region, which should be used instead of this Region (in this
     * case {@link #tryWrite} fails; the writers spin in {@link #writeBytes} and re-read the {@link #region} field). The
     * second purpose of the "closed" flag is to ensure that the region is closed (and the current primary region of the
     * MemoryMappedFileManager is set to the next region) exclusively and only once by a single thread. This is achieved
     * merely by updating the "closed" flag only via atomic CAS operations, the thread which succeeds to actually switch
     * the "closed" flag from 0 to 1 is granted the right to call {@link #doCloseAndSwitchRegionExclusively}. Once the
     * "closed" flag of the region is set to 1, it could never be set back to 0 again. See also {@link
     * #closeAndSwitchRegionWhileLocked()} and {@link #closeAndSwitchRegion}.
     *
     * The locked and closed flags are also mutually exclusive to prohibit races, i. e. The closing thread calls
     * {@link #unmap(MappedByteBuffer)} on the {@link #mappedBuffer}, while the locking thread is going to use this
     * buffer.
     */
    private static class Region {
        private static final int WRITERS_SHIFT = 32;
        private static final long WRITER = 1L << WRITERS_SHIFT;
        private static final long LOCKED = 1L << 62;
        private static final long CLOSED = 1L << 63;
        /**
         * MAX_WRITERS is smaller than {@link #WRITERS_MASK}, because the number of writers is incremented using
         * wait-free {@link AtomicLong#addAndGet(long)} operations (in {@link #tryWrite(ByteBuffer)} methods), so
         * theoretically the number of writers could increase slightly beyond MAX_WRITERS, even though each of them
         * checks that the current number is strictly smaller than MAX_WRITERS in {@link #checkStateBeforeUpdating}.
         * Anyway, neither 2^29 nor 2^30 concurrent writer thread limit couldn't realistically be reached.
         */
        private static final int MAX_WRITERS = 1 << 29;
        private static final long WRITERS_MASK = ((1L << 30) - 1) << WRITERS_SHIFT;

        /** See {@link #nextRegion} */
        private static final Object NEXT_REGION_INITIAL = new Object();
        /** See {@link #nextRegion} */
        private static final Object NEXT_REGION_PLACEHOLDER = new Object();

        private static boolean isClosed(long state) {
            return state < 0L; // the highest ("closed") bit is 1
        }

        private static boolean isLocked(long state) {
            return (state & LOCKED) != 0L;
        }

        private static int writers(long state) {
            return (int) ((state & WRITERS_MASK) >>> WRITERS_SHIFT);
        }

        private static boolean noWriters(long state) {
            return (state & WRITERS_MASK) == 0L;
        }

        private static int bufferWatermark(long state) {
            // The buffer watermark is held in the lowest 32 bits of the state, so just casting the state to int
            // extracts the buffer watermark from the state.
            return (int) state;
        }

        private static long clearStateWithBufferWatermark(int bufferWatermark) {
            // No writers, not locked, not closed => the long state is simple Java extension of the int bufferWatermark.
            return bufferWatermark;
        }

        /**
         * Region is not an instance inner class and this {@code manager} field is deliberately explicit, to avoid
         * confusion between the fields of the classes: Region's {@link #nextRegion} and manager's {@link #region}.
         */
        final MemoryMappedFileManager manager;

        final MappedByteBuffer mappedBuffer;
        /**
         * This lock is used to prohibit concurrent calls to {@link MappedByteBuffer#force()} and {@link
         * #unmap(MappedByteBuffer)}, because the latter in unspecified.
         */
        final ReadWriteLock mappedBufferSystemOperationsLock = new ReentrantReadWriteLock();
        boolean mappedBufferUnmapped = false;

        /**
         * This field is computed as {@link #mappedBuffer}.capacity() - {@link #manager}.{@link #regionLength}.
         * {@link #mappedBuffer}'s capacity could be bigger than the {@link #regionLength}, because the buffer of the
         * previous region may be (and almost always will) not filled with data until the very last byte, so the {@link
         * #mappedBuffer} need to overlap with the buffer of the previous region, not to leave gaps between data in the
         * {@link #randomAccessFile}. And there is an intention to keep the length of the {@link #randomAccessFile} a
         * multiple of {@link #regionLength}. See {@link #createNextRegion(Region, long)}.
         */
        final long initialBufferCapacityOverRegionLength;

        /**
         * The absolute offset from the beginning of the {@link #randomAccessFile}, where the {@link #mappedBuffer}
         * starts.
         */
        final long mappingOffsetInFile;

        /** See the {@link Region} class comment */
        final AtomicLong state = new AtomicLong(0);

        /**
         * Creation of a new region (see {@link #createNextRegion(Region, long)}) includes calling {@link
         * RandomAccessFile#setLength(long)} and {@link #mmap}, operations which end up in the OS and may trigger long
         * pauses for bookkeeping in the FS or the memory subsystem of the OS. If such a pause occurs during the region
         * switch, some threads may spin-wait for long time in {@link #writeBytes}, that is not a right thing to do in a
         * general-purpose logging library. To avoid this, we create the next region in the context of one of the
         * regular writer threads well before the current region is full (the heuristic quantification of this "well
         * before" is discussed in {@link #computeNextRegionCreationWatermark(int)}). The next region is created in
         * {@link #tryCreateNextRegionIfNeeded}, called from {@link #tryWrite}. By the time the current region is
         * closed, the next one is already created and could be switched in an instant. Since the new region is created
         * before the current region is closed, we cannot employ exclusiveness control that the "closed" flag
         * provides (see the {@link Region} class comment), and need a separate atomic nextRegion field to ensure that
         * the next region is created exclusively from a single thread. The thread which succeeds to actually switch the
         * value of this field from {@link #NEXT_REGION_INITIAL} to {@link #NEXT_REGION_PLACEHOLDER} is granted the
         * right to create the next region.
         *
         * nextRegion reference could contain {@link #NEXT_REGION_INITIAL}, {@link #NEXT_REGION_PLACEHOLDER} a Region
         * object _or null_, if {@link #createNextRegion(Region, long)} returns null. Account for that in {@link
         * #switchRegion(int)}.
         */
        final AtomicReference<Object> nextRegion = new AtomicReference<>(NEXT_REGION_INITIAL);

        final UninterruptibleCountDownLatch nextRegionCreated = new UninterruptibleCountDownLatch(1);

        private Region(final MemoryMappedFileManager manager, final MappedByteBuffer mappedBuffer,
                final long mappingOffsetInFile) {
            this.manager = manager;
            this.mappedBuffer = mappedBuffer;
            this.initialBufferCapacityOverRegionLength = mappedBuffer.capacity() - manager.regionLength;
            this.mappingOffsetInFile = mappingOffsetInFile;
            // The initial state (= the initial bufferWatermark) is set in switchRegion(), when this Region becomes the
            // current primary region of the manager. The initial state is not yet known at the moment when this
            // constructor is called in the context of tryCreateNextRegionIfNeeded().
        }

        /**
         * Returns true if successfully written the given data, false failed. The reasons for failure are enumerated in
         * a comment in {@link #writeBytes(byte[], int, int)}. This method is a twin of {@link #tryWrite(ByteBuffer)},
         * the difference is only the form of the data given, byte array vs. ByteBuffer.
         */
        boolean tryWrite(final byte[] data, int offset, int dataLength) {
            if (!checkStateBeforeUpdating(dataLength)) {
                return false;
            }
            long newState = bumpBufferWatermarkAndIncrementConcurrentWriterCount(dataLength);
            if (!checkStateAfterUpdatingAndRollbackIfNeeded(newState, dataLength)) {
                return false;
            }
            final int bufferWatermark = bufferWatermark(newState);
            int offsetToBufferAreaReservedForWrite = bufferWatermark - dataLength;
            copyDataToBuffer(offsetToBufferAreaReservedForWrite, data, offset, dataLength);
            decrementConcurrentWriterCount();
            tryCreateNextRegionIfNeeded(bufferWatermark);
            return true;
        }

        /**
         * Returns true if successfully written the given data, false failed. The reasons for failure are enumerated in
         * a comment in {@link #writeBytes(byte[], int, int)}. This method is a twin of {@link
         * #tryWrite(byte[], int, int)}, the difference is only the form of the data given, ByteBuffer vs. byte array.
         */
        boolean tryWrite(ByteBuffer data) {
            final int dataLength = data.remaining();
            if (!checkStateBeforeUpdating(dataLength)) {
                return false;
            }
            long newState = bumpBufferWatermarkAndIncrementConcurrentWriterCount(dataLength);
            if (!checkStateAfterUpdatingAndRollbackIfNeeded(newState, dataLength)) {
                return false;
            }
            final int bufferWatermark = bufferWatermark(newState);
            int offsetToBufferAreaReservedForWrite = bufferWatermark - dataLength;
            copyDataToBuffer(offsetToBufferAreaReservedForWrite, data);
            decrementConcurrentWriterCount();
            tryCreateNextRegionIfNeeded(bufferWatermark);
            return true;
        }

        /** Returns true if could proceed to try to write the data. See the {@link Region} class comment */
        private boolean checkStateBeforeUpdating(int dataLength) {
            long currentState = this.state.get();
            // "closed" and "locked" flags and buffer watermark overflow are checked before updating the state, despite
            // they are also checked after updating the state, that should be enough for correctness, because otherwise
            // a caller of getStableBufferWatermark() could starve, being not able to read a "stable" (i. e. with zero
            // concurrent writers) state while unsuccessful writers are constantly increasing and decreasing the
            // concurrent writer count (keeping it above zero) in tryWrite().

            // It's important to check the "closed" flag first and then "locked", because they may appear to be set at
            // the same time when tryLock() sets the "locked" flag speculatively using addAndGet(). If the flags are set
            // at the same time, the "closed" flag is "real" and the "locked" is going to be rolled back.
            if (isClosed(currentState)) {
                nextRegionCreated.await();
                return false;
            }
            if (isLocked(currentState)) {
                manager.destinationLock.awaitUnlocked();
                return false;
            }
            int currentBufferWatermark = bufferWatermark(currentState);
            // This buffer watermark overflow check could have false positive result, if some other writer bumped the
            // buffer watermark under contention beyond the buffer capacity and is going to rollback the state in
            // checkStateAfterUpdatingAndRollbackIfNeeded(). However this is not a problem, as explained in a comment in
            // manager.writeBytes(), we will spin in writeBytes(), and the spin loop will exit deterministically.
            if (currentBufferWatermark + dataLength > mappedBuffer.capacity()) {
                closeAndSwitchRegion();
                return false;
            }
            if (writers(currentState) > MAX_WRITERS) {
                // Don't allow to write if there are too many writers already, to prevent overflow in the writers area
                // inside the state. As the result, this thread will spin in manager.writeBytes() and Region.tryWrite(),
                // until the concurrent writer count drops below MAX_WRITERS. However, this should never happen, because
                // MAX_WRITERS limit is very high.
                return false;
            }
            return true;
        }

        /** See the {@link Region} class comment */
        private long bumpBufferWatermarkAndIncrementConcurrentWriterCount(final int dataLength) {
            return this.state.addAndGet(WRITER + dataLength);
        }

        /** Returns true if could proceed writing the data to the buffer. See the {@link Region} class comment */
        private boolean checkStateAfterUpdatingAndRollbackIfNeeded(long newState, final long dataLength) {
            // Same checks of the "closed" and "locked" flags and almost the same reasoning as in
            // checkStateBeforeUpdating().
            if (isClosed(newState)) {
                rollbackBufferWatermarkAndDecrementConcurrentWriterCount(dataLength);
                nextRegionCreated.await();
                return false;
            }
            if (isLocked(newState)) {
                rollbackBufferWatermarkAndDecrementConcurrentWriterCount(dataLength);
                manager.destinationLock.awaitUnlocked();
                return false;
            }
            int bufferWatermark = bufferWatermark(newState);
            if (bufferWatermark > mappedBuffer.capacity()) {
                rollbackBufferWatermarkAndDecrementConcurrentWriterCount(dataLength);
                closeAndSwitchRegion();
                return false;
            }
            return true;
        }

        /** See the {@link Region} class comment */
        private void rollbackBufferWatermarkAndDecrementConcurrentWriterCount(final long dataLength) {
            this.state.addAndGet((-WRITER) - dataLength);
        }

        /** See the {@link Region} class comment */
        private void decrementConcurrentWriterCount() {
            this.state.addAndGet(-WRITER);
        }

        private void copyDataToBuffer(int bufferOffset, final byte[] data, int dataOffset, int dataLength) {
            MappedByteBuffer buffer = this.mappedBuffer;
            // As long as we cannot use sun.misc.Unsafe and even wrap the data array as ByteBuffer, and duplicate() the
            // mappedBuffer (because we don't want to create garbage), byte-by-byte copying is the only option left.
            for (int i = 0; i < dataLength; i++) {
                buffer.put(bufferOffset + i, data[dataOffset + i]);
            }
        }

        private void copyDataToBuffer(int bufferOffset, ByteBuffer data) {
            MappedByteBuffer mappedBuffer = this.mappedBuffer;
            int dataLength = data.remaining();
            int i = 0;
            for (; i < dataLength - 7; i += 8) {
                mappedBuffer.putLong(bufferOffset + i, data.getLong());
            }
            for (; i < dataLength; i++) {
                mappedBuffer.put(bufferOffset + i, data.get());
            }
        }

        /** See {@link #nextRegion} field comment */
        private void tryCreateNextRegionIfNeeded(int bufferWatermark) {
            if (bufferWatermark >= initialBufferCapacityOverRegionLength + manager.nextRegionCreationWatermark &&
                nextRegion.get() == NEXT_REGION_INITIAL) {
                tryCreateNextRegion(bufferWatermark);
            }
        }

        /** See {@link #nextRegion} field comment */
        private void tryCreateNextRegion(int bufferWatermark) {
            if (nextRegion.compareAndSet(NEXT_REGION_INITIAL, NEXT_REGION_PLACEHOLDER)) {
                // manager.createNextRegion() could return null, account for that in switchRegion().
                nextRegion.set(manager.createNextRegion(this, mappingOffsetInFile + bufferWatermark));
                nextRegionCreated.countDown();
            }
        }

        /**
         * As explained in the {@link Region} class comment, actual closing and switching region should be performed
         * only once from a single thread, so this method actually *tries* to close, but the fail means that some other
         * thread has succeed to close.
         *
         * If the caller of this method was attempting to write large data, the retry loop inside this method may fail
         * until concurrent threads push the buffer watermark until the end of the region, writing small pieces of data.
         * This is another reason to limit the data size that could be written using the wait-free way, see {@link
         * #computeLockedWriteThreshold(int)}.
         */
        private void closeAndSwitchRegion() {
            while (true) {
                long state = this.state.get();
                // Don't allow to close if the Region is locked. In this case closing the Region becomes the
                // responsibility of the lock holder thread.
                if (isClosed(state) || isLocked(state)) {
                    return;
                }
                if (this.state.compareAndSet(state, state | CLOSED)) {
                    doCloseAndSwitchRegionExclusively();
                    return;
                }
                ThreadHints.onSpinWait();
            }
        }

        /**
         * Similar to {@link #closeAndSwitchRegion()}, additionally returns "stable" buffer watermark (see the {@link
         * Region} class comment).
         */
        void closeAndSwitchRegionWhileLocked() {
            manager.verifyDestinationLockHeld();
            // CAS update with retry loop is needed despite we are holding the lock, because concurrent (unsuccessful)
            // writers could still increment and decrement the buffer watermark and the writer count, so if the state is
            // changed here without CAS, invariants could be broken, e. g. the concurrent writer count could underflow.
            while (true) {
                final long state = this.state.get();
                if (this.state.compareAndSet(state, (state & ~LOCKED) | CLOSED)) {
                    doCloseAndSwitchRegionExclusively();
                    return;
                }
                // The above CAS may fail if other threads on are stopped after successful checkStateBeforeUpdating()
                // and before actually updating the state, and it's more a theoretical case that it may fail more than
                // 1 or 2 times, so starvation is not possible here. Adding ThreadHints.onSpinWait() just in case.
                ThreadHints.onSpinWait();
            }
        }

        private void doCloseAndSwitchRegionExclusively() {
            final int finalBufferWatermark;
            if (manager.destinationLock.isHeldByCurrentThread()) {
                finalBufferWatermark = mappedBuffer.position();
            } else {
                finalBufferWatermark = getStableBufferWatermark();
            }
            switchRegion(finalBufferWatermark);
            // It is safe to unmap the mappedBuffer, because getStableBufferWatermark() is called above and upon
            // it's return it is guaranteed that all concurrent writers has completed.
            unmapMappedBuffer();
        }

        private void switchRegion(final int finalBufferWatermark) {
            // Creation of the next Region should normally be started (and likely finished) already, by the first writer
            // that bumped the buffer watermark beyond manager.nextRegionCreationWatermark. But if
            // manager.regionLength - manager.nextRegionCreationWatermark < manager.lengthThresholdForExclusiveWrite,
            // it is possible that switchRegion() is called in the context of checkStateBeforeUpdating() before the
            // buffer watermark is bumped beyond manager.nextRegionCreationWatermark, so calling tryCreateNextRegion()
            // here to ensure that creation of the next Region is started.
            tryCreateNextRegion(finalBufferWatermark);
            nextRegionCreated.await();
            final Object nextRegionObject = this.nextRegion.get();
            // nextRegionObject could be null.
            if (nextRegionObject instanceof Region && !manager.closing) {
                Region nextRegion = (Region) nextRegionObject;
                final long bufferWatermarkInFile = mappingOffsetInFile + finalBufferWatermark;
                final long nextRegionBufferWatermark = bufferWatermarkInFile - nextRegion.mappingOffsetInFile;
                if (nextRegionBufferWatermark < 0 || nextRegionBufferWatermark > Integer.MAX_VALUE) {
                    throw new Error("nextRegionBufferWatermark: " + nextRegionBufferWatermark);
                }
                final long nextRegionState;
                if (manager.destinationLock.isHeldByCurrentThread()) {
                    nextRegion.mappedBuffer.position((int) nextRegionBufferWatermark);
                    nextRegionState = LOCKED;
                } else {
                    nextRegionState = clearStateWithBufferWatermark((int) nextRegionBufferWatermark);
                }
                nextRegion.state.set(nextRegionState);
                manager.region = nextRegion;
            } else {
                if (nextRegionObject instanceof Region) {
                    ((Region) nextRegionObject).unmapMappedBuffer();
                }
                // The nextRegionObject is not an instance of Region means that the nextRegionObject is null, if
                // manager.createNextRegion() returned null, either because it is closing or because buffer mapping for
                // the next region has failed. Handle this in the methods of MemoryMappedFileManager by checking that
                // the manager.region field is not null.
                manager.region = null;
                // Used in manager.doClose()
                manager.finalRegionMappingOffset = mappingOffsetInFile;
                manager.finalRegionBufferWatermark = finalBufferWatermark;
            }
        }

        private boolean tryLock() {
            final long state = this.state.get();
            if (isClosed(state)) {
                return false;
            }
            // The "locked" flag may already be set, because manager.destinationLock (it calls tryLock()) is
            // reentrant. Reenter count is kept in manager.destinationLock.byteBufferApiLock.
            if (isLocked(state)) {
                return true;
            }
            // This addAndGet() couldn't lead to "conversion" of the "locked" flag into "closed"
            // (LOCKED + LOCKED = CLOSED), because tryLock() could be called only from a single thread (it' protected by
            // manager.destinationLock.byteBufferApiLock) and it's checked above that the locked flag is not set
            // already.
            long newState = this.state.addAndGet(LOCKED);
            // If while doing addAndGet() some other thread has set the "closed" flag, give it the priority, the
            // "locked" and "closed" flags couldn't be set at the same time. See the Region class comment.
            if (isClosed(newState)) {
                // Rollback the "locked" flag.
                this.state.addAndGet(-LOCKED);
                return false;
            }
            // Prepare the mappedBuffer to be used via ByteBufferDestination.getByteBuffer(), actualize it's
            // position.
            int stableBufferWatermark = getStableBufferWatermark();
            mappedBuffer.position(stableBufferWatermark);
            return true;
        }

        private void unlock() {
            manager.verifyDestinationLockHeld();
            int bufferWatermark = mappedBuffer.position();
            // While the Region was locked and mappedBuffer was updated directly, nextRegionCreationWatermark was
            // not checked, so need to check it now.
            tryCreateNextRegionIfNeeded(bufferWatermark);
            long newState = clearStateWithBufferWatermark(bufferWatermark);
            while (true) {
                long state = this.state.get();
                // Switch to the new state only at a "quiet" moment (no concurrent writers), no ensure that the buffer
                // watermark is not corrupted under a race. The comment to getStableBufferWatermark() explains it in
                // more details.
                if (noWriters(state) && this.state.compareAndSet(state, newState)) {
                    return;
                }
                // The above CAS may fail if other threads on are stopped after successful checkStateBeforeUpdating()
                // and before actually updating the state, and it's more a theoretical case that it may fail more than
                // 1 or 2 times, so starvation is not possible here. Adding ThreadHints.onSpinWait() just in case.
                ThreadHints.onSpinWait();
            }
        }

        /**
         * Even after the region is locked or closed, the state could be "unstable", because concurrent writers may
         * increment and decrement the buffer watermark (see {@link #tryWrite}). To get the "true" buffer watermark, we
         * should catch a moment when there are zero concurrent writers in the state. See also a comment in {@link
         * #checkStateBeforeUpdating(int)}, where the possibility of starvation in getStableBufferWatermark() is
         * discussed, and the {@link Region} class comment.
         *
         * Catching the moment when there are no writers also means that all writers, which were in progress when the
         * Region was locked or closed, have completed. So calling getStableBufferWatermark() also provides
         * happens-before between all pre-lock or pre-close writers and the actions that follow the
         * getStableBufferWatermark() call.
         *
         * The time for how long this method may spend spinning is controlled by {@link
         * #lengthThresholdForExclusiveWrite}, because the spin time is approximately bound by the slowest concurrent
         * lock-free writer, which writes it's data to the buffer. See {@link #computeLockedWriteThreshold(int)} and
         * {@link #SPIN_WAIT_MINIMIZING_LOCKED_WRITE_THRESHOLD} for discussion of this.
         */
        private int getStableBufferWatermark() {
            while (true) {
                long state = this.state.get();
                if (noWriters(state)) {
                    return bufferWatermark(state);
                }
                // Spin loop, waiting for the moment when there are no concurrent writers.
                ThreadHints.onSpinWait();
            }
        }

        private void unmapMappedBuffer() {
            mappedBufferSystemOperationsLock.writeLock().lock();
            try {
                try {
                    unmap(mappedBuffer);
                } catch (final Exception ex) {
                    manager.logError("Unable to unmap MappedBuffer", ex);
                }
            } finally {
                // Even if unmap failed with exception, set mappedBufferUnmapped to true not to try to force the
                // mappedBuffer in forceMappedBuffer(), because mappedBuffer might be left in an inconsistent state.
                mappedBufferUnmapped = true;
                mappedBufferSystemOperationsLock.writeLock().unlock();
            }
        }

        private void forceMappedBuffer() {
            // MappedByteBuffer.force() is called under read lock, i. e. concurrent calls to force() are allowed,
            // because nothing in it's Javadoc says that it shouldn't be ok.
            if (!mappedBufferSystemOperationsLock.readLock().tryLock()) {
                // mappedBufferSystemOperationsLock.readLock is not available means that unmapMappedBuffer() is in
                // progress (i. e. this Region is in the process of closing) that is equivalent to forcing.
                return;
            }
            try {
                if (!mappedBufferUnmapped) {
                    mappedBuffer.force();
                }
            } finally {
                mappedBufferSystemOperationsLock.readLock().unlock();
            }
        }
    }

    private final Layout<?> layout;
    private final boolean immediateFlush;
    private final int regionLength;
    private final int nextRegionCreationWatermark;
    private final int lengthThresholdForExclusiveWrite;
    private final String advertiseURI;
    private final RandomAccessFile randomAccessFile;
    private final ThreadLocal<Boolean> isEndOfBatch = new ThreadLocal<>();
    private final DestinationLock destinationLock = new DestinationLock();
    /**
     * The {@code region} field needs to be volatile to ensure safe publication and visibility of the new Region from
     * threads spinning in {@link #writeBytes} methods. Update itself is done in {@link Region#switchRegion}, no other
     * synchronization actions are done in {@link Region#switchRegion} to ensure safe publication and visibility of
     * the new Region, when it is assigned to this field.
     */
    private volatile Region region;
    private boolean closing = false;
    private long finalRegionMappingOffset;
    private long finalRegionBufferWatermark;

    MemoryMappedFileManager(final RandomAccessFile file, final String fileName,
            final boolean immediateFlush, final long position, final int regionLength, final String advertiseURI,
            final Layout<? extends Serializable> layout, final boolean writeHeader) throws IOException {
        super(null, fileName);
        this.layout = layout;
        this.immediateFlush = immediateFlush;
        this.randomAccessFile = Objects.requireNonNull(file, "RandomAccessFile");
        this.regionLength = regionLength;
        this.nextRegionCreationWatermark = computeNextRegionCreationWatermark(regionLength);
        this.lengthThresholdForExclusiveWrite = computeLockedWriteThreshold(regionLength);
        this.advertiseURI = advertiseURI;
        this.isEndOfBatch.set(Boolean.FALSE);
        MappedByteBuffer mappedBuffer = mmap(randomAccessFile.getChannel(), getFileName(), position, regionLength);
        region = new Region(this, mappedBuffer, position);

        if (writeHeader && layout != null) {
            final byte[] header = layout.getHeader();
            if (header != null) {
                writeBytes(header, 0, header.length);
            }
        }
    }

    /**
     * Chooses how full the current {@link #region} should become when the creation of the next region is triggered (see
     * the comment to the {@link Region} class for discussion why it is needed). With the {@link #DEFAULT_REGION_LENGTH}
     * of 32 MB and the next region creation watermark of 0.75, the next region should be created when the application
     * writes 8 MB of logs, that is 8 seconds even for heavily logging applications at 1 MB / s logging rate.
     */
    private static int computeNextRegionCreationWatermark(int regionLength) {
        if (NCPU == 1) {
            // Don't need to care about concurrent writers, if there is just one core.
            return regionLength;
        }
        return regionLength / 4 * 3;
    }

    /**
     * Chooses the threshold of the data size to write, after which the data should be written exclusively (see {@link
     * #destinationLock}) rather than using the concurrent wait-free path.
     *
     * There are three factors limiting this threshold:
     *  - Minimizing possible spin-wait time in {@link Region#getStableBufferWatermark()} and {@link
     *  Region#closeAndSwitchRegion()}, see comments to these methods for explanation, and also {@link
     *  #SPIN_WAIT_MINIMIZING_LOCKED_WRITE_THRESHOLD}.
     *  - If there are several threads which frequently write large data, so that number of threads * the size of data
     *  exceeds the {@link #regionLength}, wait-free writes could turn into contention on spin-locking {@link
     *  Region#closeAndSwitchRegion}, that may result in higher pauses, than with proper locking. So we prohibit this
     *  situation by limiting the threshold accordingly (see spinLockContentionThreshold in code). However, this is
     *  more a theoretical limit for reasonable {@link #regionLength} (e. g. the {@link #DEFAULT_REGION_LENGTH}),
     *  because the previous limit {@link #SPIN_WAIT_MINIMIZING_LOCKED_WRITE_THRESHOLD} should be smaller.
     *  - We don't want to switch regions (buffers) if they are filled just a little, so if the threshold is e. g. equal
     *  to regionLength, and the current region is filled just by one byte, a wait-free write will require to close the
     *  current region and create a new one, because wait-free write needs the current {@link Region#mappedBuffer} to
     *  have equal or more free bytes than we need.
     *
     *  If there is just one CPU, the first two factors are irrelevant.
     */
    private static int computeLockedWriteThreshold(int regionLength) {
        int regionSwitchMinimizingThreshold = regionLength / 4;
        if (NCPU == 1) {
            return regionSwitchMinimizingThreshold;
        }
        int maxHeavilyContendingThreads = Math.min(NCPU, 64);
        int spinLockContentionThreshold = regionLength / maxHeavilyContendingThreads;
        return Math.min(Math.min(regionSwitchMinimizingThreshold, spinLockContentionThreshold),
                SPIN_WAIT_MINIMIZING_LOCKED_WRITE_THRESHOLD);
    }

    /**
     * Returns the MemoryMappedFileManager.
     *
     * @param fileName The name of the file to manage.
     * @param append true if the file should be appended to, false if it should be overwritten.
     * @param immediateFlush true if the contents should be flushed to disk on every write
     * @param regionLength The mapped region length.
     * @param advertiseURI the URI to use when advertising the file
     * @param layout The layout.
     * @return A MemoryMappedFileManager for the File.
     */
    public static MemoryMappedFileManager getFileManager(final String fileName, final boolean append,
            final boolean immediateFlush, final int regionLength, final String advertiseURI,
            final Layout<? extends Serializable> layout) {
        return getManager(fileName, FACTORY,
                new FactoryData(append, immediateFlush, regionLength, advertiseURI, layout));
    }

    private Region getCurrentRegionChecked() {
        Region region = this.region;
        if (region == null) {
            throw new IllegalStateException("The MemoryMappedFileManager is closing or failed to map file into memory");
        }
        return region;
    }

    public Boolean isEndOfBatch() {
        return isEndOfBatch.get();
    }

    public void setEndOfBatch(final boolean endOfBatch) {
        this.isEndOfBatch.set(endOfBatch);
    }

    @Override
    protected void write(final byte[] data, final boolean immediateFlush) {
        writeBytes(data, 0, data.length);
        if (immediateFlush) {
            flush();
        }
    }

    /**
     * This method is a twin of {@link #writeBytes(ByteBuffer)}, the difference is only the form of the data given, byte
     * array vs. ByteBuffer.
     */
    @Override
    public void writeBytes(final byte[] data, final int offset, final int length) {
        if (destinationLock.isHeldByCurrentThread()) {
            // If we are in exclusive write mode, write the data using the ByteBufferDestination's getByteBuffer() API.
            ByteBufferDestinationHelper.writeToUnsynchronized(data, offset, length, this);
            return;
        }
        if (length >= lengthThresholdForExclusiveWrite) {
            writeExclusively(data, offset, length);
            return;
        }
        while (true) {
            Region region = getCurrentRegionChecked();
            if (region.tryWrite(data, offset, length)) {
                return;
            }
            // tryWrite() could fail for one of the following reasons:
            //  1. The region is closed. In this case we spin, waiting for the thread which calls
            //     region.doCloseAndSwitchRegionExclusively() to set the next region to the region field.
            //  2. The region was locked. Unlock is already awaited inside tryWrite(). Call tryWrite() again in a spin
            //     loop.
            //  3. False positive buffer watermark overflow check in Region.checkStateBeforeUpdating(), because some
            //     other writer bumped the buffer watermark under contention beyond the buffer capacity and is going to
            //     rollback the state. In this case one of the contenders or some other thread should succeed to switch
            //     the region (they all call closeAndSwitchRegion() from checkStateAfterUpdatingAndRollbackIfNeeded() or
            //     checkStateBeforeUpdating()). So we spin, waiting for some thread to set the next region to the region
            //     field (similar to 1.)
            //  4. There are too many concurrent writers (practically impossible). We spin, waiting to some writers to
            //     complete.

            ThreadHints.onSpinWait();
        }
    }

    /**
     * This method is a twin of {@link #writeBytes(byte[], int, int)}, the difference is only the form of the data
     * given, ByteBuffer vs. byte array.
     */
    @Override
    public void writeBytes(final ByteBuffer data) {
        if (destinationLock.isHeldByCurrentThread()) {
            // If we are in exclusive write mode, write the data using the ByteBufferDestination's getByteBuffer() API.
            ByteBufferDestinationHelper.writeToUnsynchronized(data, this);
            return;
        }
        if (data.remaining() >= lengthThresholdForExclusiveWrite) {
            writeExclusively(data);
            return;
        }
        while (true) {
            Region region = getCurrentRegionChecked();
            if (region.tryWrite(data)) {
                return;
            }
            // See a big comment at the same place in writeBytes(byte[], int, int, boolean)

            ThreadHints.onSpinWait();
        }
    }

    private void writeExclusively(final byte[] bytes, int offset, int length) {
        destinationLock.lock();
        try {
            writeBytes(bytes, offset, length);
        } finally {
            destinationLock.unlock();
        }
    }

    private void writeExclusively(ByteBuffer data) {
        destinationLock.lock();
        try {
            writeBytes(data);
        } finally {
            destinationLock.unlock();
        }
    }

    /**
     * Creates a new Region after the given Region. May return null, if Region creation is not possible, either because
     * this MemoryMappedFileManager is closing, or an IO error occurred while trying to create the new region.
     *
     * The buffer of the created Region is allocated to be bigger than the {@link #regionLength} (by
     * currentRegionLeftover), because if newFileLength = currentRegionMappingEnd + regionLength, that is likely if the
     * {@link #randomAccessFile} grows as the log is written, there is an intention to make the {@link
     * Region#mappedBuffer} to cover the file until the end. Also it helps to keep the invariant that the
     * currentRegionMappingEnd is always a multiple of the regionLength, so that if the regionLength is a multiple of 4K
     * (the default and most likely), and the log file grows from size 0, the file size will always be a multiple of the
     * page size, that is efficient from the perspective of the FS and the memory subsystem of the OS. See also {@link
     * Region#initialBufferCapacityOverRegionLength}.
     *
     * @param newMappingOffsetInFile the offset from the beginning of the {@link #randomAccessFile}, from which the
     * {@link Region#mappedBuffer} of the created region should start.
     */
    private Region createNextRegion(Region currentRegion, long newMappingOffsetInFile) {
        if (closing) {
            return null;
        }
        try {
            final long currentFileLength = randomAccessFile.length();
            final long currentRegionMappingEnd = currentRegion.mappingOffsetInFile + currentRegion.mappedBuffer.capacity();
            final long newFileLength = Math.max(currentRegionMappingEnd + regionLength, currentFileLength);
            if (newFileLength > currentFileLength) {
                extendFileLength(currentFileLength, newFileLength);
            }
            final int currentRegionLeftover = (int) (currentRegionMappingEnd - newMappingOffsetInFile);
            MappedByteBuffer mappedBuffer = mmap(randomAccessFile.getChannel(), getFileName(), newMappingOffsetInFile,
                currentRegionLeftover + regionLength);
            return new Region(this, mappedBuffer, newMappingOffsetInFile);
        } catch (IOException ex) {
            logError("Unable to create next region", ex);
            return null;
        }
    }

    private void extendFileLength(final long currentFileLength, final long newFileLength) throws IOException {
        LOGGER.debug("{} {} extending {} by {} bytes to {}", getClass().getSimpleName(), getName(),
                getFileName(), newFileLength - currentFileLength, newFileLength);

        final long startNanos = System.nanoTime();
        randomAccessFile.setLength(newFileLength);
        final float millis = (float) ((System.nanoTime() - startNanos) / NANOS_PER_MILLISEC);
        LOGGER.debug("{} {} extended {} OK in {} millis", getClass().getSimpleName(), getName(), getFileName(),
            millis);
    }

    /**
     * The implementation of this method guarantees that only pieces of data that are written to this
     * MemoryMappedFileManager from the current thread are fully flushed to the disk behind the {@link
     * #randomAccessFile}. Data pieces that are written concurrently from other threads and may appear intermittent with
     * the data pieces written from the current thread may be flushed half-baked, so areas of garbage or zero bytes may
     * appear both before and after the last data piece, written from the current thread:
     * [A Message written from the current thread]
     * [Message written from other threa..]
     * [Last message written from the current thread]
     * [Messa^&%$#1..]
     *
     * This is due to concurrent non-blocking nature of MemoryMappedFileManager and cannot be fixed without taking the
     * {@link #destinationLock} on each flush, that would make MemoryMappedFileManager to behave essentially as a
     * blocking synchronized manager, if {@link #immediateFlush} is true.
     *
     */
    @Override
    public void flush() {
        Region region = this.region;
        if (region != null) {
            region.forceMappedBuffer();
        }
    }

    /**
     * Default hook to write footer during close.
     */
    @Override
    public boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        writeFooter();
        return doClose();
    }

    /**
     * Writes the footer.
     */
    private void writeFooter() {
        if (layout == null) {
            return;
        }
        final byte[] footer = layout.getFooter();
        if (footer != null) {
            writeBytes(footer, 0, footer.length);
        }
    }

    private boolean doClose() {
        destinationLock.lock();
        try {
            if (closing) {
                throw new IllegalStateException("doClose() called the second time");
            }
            destinationLock.setClosing();
            closing = true;
            final Region region = this.region;
            if (region != null) {
                // Forces the region to set finalRegionMappingOffset and finalRegionBufferWatermark in
                // Region.switchRegion(). If the region is null, this is already done.
                region.closeAndSwitchRegionWhileLocked();
            }
            final long fileLength = finalRegionMappingOffset + finalRegionBufferWatermark;
            try {
                LOGGER.debug("MMapAppender closing. Setting {} length to {} (offset {} + position {})", getFileName(),
                        fileLength, finalRegionMappingOffset, finalRegionBufferWatermark);
                randomAccessFile.setLength(fileLength);
                randomAccessFile.close();
                return true;
            } catch (final IOException ex) {
                logError("Unable to close MemoryMappedFile", ex);
                return false;
            }
        } finally {
            destinationLock.unlock();
        }
    }

    private static MappedByteBuffer mmap(final FileChannel fileChannel, final String fileName, final long start,
            final int size) throws IOException {
        for (int i = 1;; i++) {
            try {
                LOGGER.debug("MMapAppender remapping {} start={}, size={}", fileName, start, size);

                final long startNanos = System.nanoTime();
                final MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_WRITE, start, size);
                map.order(ByteOrder.nativeOrder());

                final float millis = (float) ((System.nanoTime() - startNanos) / NANOS_PER_MILLISEC);
                LOGGER.debug("MMapAppender remapped {} OK in {} millis", fileName, millis);

                return map;
            } catch (final IOException e) {
                if (e.getMessage() == null || !e.getMessage().endsWith("user-mapped section open")) {
                    throw e;
                }
                LOGGER.debug("Remap attempt {}/{} failed. Retrying...", i, MAX_REMAP_COUNT, e);
                if (i < MAX_REMAP_COUNT) {
                    Thread.yield();
                } else {
                    try {
                        Thread.sleep(1);
                    } catch (final InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
    }

    private static void unmap(final MappedByteBuffer mbb) throws PrivilegedActionException {
        LOGGER.debug("MMapAppender unmapping old buffer...");
        final long startNanos = System.nanoTime();
        AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws Exception {
                final Method getCleanerMethod = mbb.getClass().getMethod("cleaner");
                getCleanerMethod.setAccessible(true);
                final Object cleaner = getCleanerMethod.invoke(mbb); // sun.misc.Cleaner instance
                final Method cleanMethod = cleaner.getClass().getMethod("clean");
                cleanMethod.invoke(cleaner);
                return null;
            }
        });
        final float millis = (float) ((System.nanoTime() - startNanos) / NANOS_PER_MILLISEC);
        LOGGER.debug("MMapAppender unmapped buffer OK in {} millis", millis);
    }

    /**
     * Returns the name of the File being managed.
     *
     * @return The name of the File being managed.
     */
    public String getFileName() {
        return getName();
    }

    /**
     * Returns the length of the memory mapped region.
     *
     * @return the length of the mapped region
     */
    public int getRegionLength() {
        return regionLength;
    }

    /**
     * Returns {@code true} if the content of the buffer should be forced to the storage device on every write,
     * {@code false} otherwise.
     *
     * @return whether each write should be force-sync'ed
     */
    public boolean isImmediateFlush() {
        return immediateFlush;
    }

    /**
     * Gets this FileManager's content format specified by:
     * <p>
     * Key: "fileURI" Value: provided "advertiseURI" param.
     * </p>
     *
     * @return Map of content format keys supporting FileManager
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>(super.getContentFormat());
        result.put("fileURI", advertiseURI);
        return result;
    }

    @Override
    public Lock getDestinationLock() {
        return destinationLock;
    }

    @Override
    public ByteBuffer getByteBuffer() {
        checkDestinationLockHeld();
        return getCurrentRegionChecked().mappedBuffer;
    }

    private void checkDestinationLockHeld() {
        if (!destinationLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("destinationLock must be held");
        }
    }

    private void verifyDestinationLockHeld() {
        if (!destinationLock.isHeldByCurrentThread()) {
            throw new Error("destinationLock is not held");
        }
    }

    @Override
    public ByteBuffer drain(final ByteBuffer buf) {
        checkDestinationLockHeld();
        Region currentRegion = getCurrentRegionChecked();
        if (currentRegion.mappedBuffer.remaining() >= regionLength) {
            // Don't switch the region, if it's remaining capacity is still bigger, than the nominal regionLength.
            return currentRegion.mappedBuffer;
        }
        currentRegion.closeAndSwitchRegionWhileLocked();
        // Another call to getCurrentRegionChecked(), because the region was switched in the previous line.
        return getCurrentRegionChecked().mappedBuffer;
    }

    /**
     * The reentrant lock implementation for {@link #getDestinationLock()}. See {@link LockableByteBufferDestination}.
     */
    private class DestinationLock implements Lock {
        /**
         * Implements the actual reentrant lock mechanics of the DestinationLock.
         */
        private final UnlockedAwaitableReentrantLock byteBufferApiLock = new UnlockedAwaitableReentrantLock();

        @Override
        public void lock() {
            byteBufferApiLock.lock();
            lockCurrentRegion();
        }

        /**
         * lockCurrentRegion() must not fail if the region is null, because it would leave the DestinationLock
         * in a bad state, because the byteBufferApiLock is already locked. So DestinationLock lets the caller
         * to fail later, when calling getByteBuffer() or drain():
         *
         * destinationLock.lock(); // successful
         * try {
         *     // IllegalStateException: MemoryMappedFileManager is closing or failed to map file into memory
         *     ByteBuffer buffer = destination.getByteBuffer();
         * } finally {
         *     destinationLock.unlock(); // successful
         * }
         */
        private void lockCurrentRegion() {
            while (true) {
                Region region = MemoryMappedFileManager.this.region;
                if (region == null || region.tryLock()) {
                    return;
                }
                // The above region.tryLock() may fail only if the region is closed at the same time, so it's pretty
                // much impossible that it fails more than once. Adding ThreadHints.onSpinWait() just in case.
                ThreadHints.onSpinWait();
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            byteBufferApiLock.lockInterruptibly();
            lockCurrentRegion();
        }

        @Override
        public boolean tryLock() {
            if (byteBufferApiLock.tryLock()) {
                lockCurrentRegion();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
            if (byteBufferApiLock.tryLock(time, unit)) {
                lockCurrentRegion();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void unlock() {
            Region region = MemoryMappedFileManager.this.region;
            if (region != null) {
                region.unlock();
            }
            byteBufferApiLock.unlock();
        }

        private void setClosing() {
            byteBufferApiLock.memoryMappedFileManagerIsClosing = true;
        }

        private boolean isHeldByCurrentThread() {
            return byteBufferApiLock.isHeldByCurrentThread();
        }

        private void awaitUnlocked() {
            byteBufferApiLock.awaitUnlocked();
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Conditions are not supported by this lock");
        }
    }

    /**
     * Factory Data.
     */
    private static class FactoryData {
        private final boolean append;
        private final boolean immediateFlush;
        private final int regionLength;
        private final String advertiseURI;
        private final Layout<? extends Serializable> layout;

        /**
         * Constructor.
         *
         * @param append Append to existing file or truncate.
         * @param immediateFlush forces the memory content to be written to the storage device on every event
         * @param regionLength length of the mapped region
         * @param advertiseURI the URI to use when advertising the file
         * @param layout The layout.
         */
        public FactoryData(final boolean append, final boolean immediateFlush, final int regionLength,
                final String advertiseURI, final Layout<? extends Serializable> layout) {
            this.append = append;
            this.immediateFlush = immediateFlush;
            this.regionLength = regionLength;
            this.advertiseURI = advertiseURI;
            this.layout = layout;
        }
    }

    /**
     * Factory to create a MemoryMappedFileManager.
     */
    private static class MemoryMappedFileManagerFactory
            implements ManagerFactory<MemoryMappedFileManager, FactoryData> {

        /**
         * Create a MemoryMappedFileManager.
         *
         * @param name The name of the File.
         * @param data The FactoryData
         * @return The MemoryMappedFileManager for the File.
         */
        @SuppressWarnings("resource")
        @Override
        public MemoryMappedFileManager createManager(final String name, final FactoryData data) {
            final File file = new File(name);
            if (!data.append) {
                file.delete();
            }

            final boolean writeHeader = !data.append || !file.exists();
            RandomAccessFile raf = null;
            try {
                FileUtils.makeParentDirs(file);
                raf = new RandomAccessFile(name, "rw");
                final long position = (data.append) ? raf.length() : 0;
                raf.setLength(position + data.regionLength);
                return new MemoryMappedFileManager(raf, name, data.immediateFlush, position, data.regionLength,
                        data.advertiseURI, data.layout, writeHeader);
            } catch (final Exception ex) {
                LOGGER.error("MemoryMappedFileManager (" + name + ") " + ex, ex);
                Closer.closeSilently(raf);
            }
            return null;
        }
    }
}
