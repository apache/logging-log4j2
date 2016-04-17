package org.apache.logging.log4j.core.async.perftest;

/**
 * Idle strategy for use by threads when they do not have work to do.
 *
 * <h3>Note regarding implementor state</h3>
 *
 * Some implementations are known to be stateful, please note that you cannot safely assume implementations to be stateless.
 * Where implementations are stateful it is recommended that implementation state is padded to avoid false sharing.
 *
 * <h3>Note regarding potential for TTSP(Time To Safe Point) issues</h3>
 *
 * If the caller spins in a 'counted' loop, and the implementation does not include a a safepoint poll this may cause a TTSP
 * (Time To SafePoint) problem. If this is the case for your application you can solve it by preventing the idle method from
 * being inlined by using a Hotspot compiler command as a JVM argument e.g:
 * <code>-XX:CompileCommand=dontinline,org.apache.logging.log4j.core.async.perftest.NoOpIdleStrategy::idle</code>
 *
 * @see <a href="https://github.com/real-logic/Agrona/blob/master/src/main/java/org/agrona/concurrent/IdleStrategy.java">
 *     https://github.com/real-logic/Agrona/blob/master/src/main/java/org/agrona/concurrent/IdleStrategy.java</a>
 */
public interface IdleStrategy {
    /**
     * Perform current idle action (e.g. nothing/yield/sleep). To be used in conjunction with {@link IdleStrategy#reset()}
     * to clear internal state when idle period is over (or before it begins). Callers are expected to follow this pattern:
     *
     * <pre>
     * <code>while (isRunning) {
     *   if (!hasWork()) {
     *     idleStrategy.reset();
     *     while (!hasWork()) {
     *       if (!isRunning) {
     *         return;
     *       }
     *       idleStrategy.idle();
     *     }
     *   }
     *   doWork();
     * }
     * </code>
     * </pre>
     */
    void idle();
}
