package org.apache.logging.log4j.core.async;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.jctools.queues.MpscArrayQueue;

/**
 * Factory for creating instances of BlockingQueues backed by JCTools {@link MpscArrayQueue}.
 *
 * @since 2.7
 */
@Plugin(name = "JCToolsBlockingQueue", category = Node.CATEGORY, elementType = BlockingQueueFactory.ELEMENT_TYPE)
public class JCToolsBlockingQueueFactory<E> implements BlockingQueueFactory<E> {

    private JCToolsBlockingQueueFactory() {
    }

    @Override
    public BlockingQueue<E> create(final int capacity) {
        return new MpscBlockingQueue<>(capacity);
    }

    @PluginFactory
    public static <E> JCToolsBlockingQueueFactory<E> createFactory() {
        return new JCToolsBlockingQueueFactory<>();
    }

    /**
     * BlockingQueue wrapper for JCTools multiple producer single consumer array queue.
     */
    private static final class MpscBlockingQueue<E> extends MpscArrayQueue<E> implements BlockingQueue<E> {

        MpscBlockingQueue(final int capacity) {
            super(capacity);
        }

        @Override
        public int drainTo(final Collection<? super E> c) {
            return drainTo(c, capacity());
        }

        @Override
        public int drainTo(final Collection<? super E> c, final int maxElements) {
            return drain(new Consumer<E>() {
                @Override
                public void accept(E arg0) {
                    c.add(arg0);
                }
            }, maxElements);
        }

        @Override
        public boolean offer(final E e, final long timeout, final TimeUnit unit) throws InterruptedException {
            // TODO Auto-generated method stub
            return offer(e);
        }

        @Override
        public E poll(final long timeout, final TimeUnit unit) throws InterruptedException {
            // TODO Auto-generated method stub
            return poll();
        }

        @Override
        public void put(final E e) throws InterruptedException {
            while (!relaxedOffer(e)) {
                LockSupport.parkNanos(1L);
            }
        }

        @Override
        public int remainingCapacity() {
            return capacity() - size();
        }

        @Override
        public E take() throws InterruptedException {
            for (; ; ) {
                final E result = poll();
                if (result != null) {
                    return result;
                }
                LockSupport.parkNanos(1L);
            }
        }
    }
}
