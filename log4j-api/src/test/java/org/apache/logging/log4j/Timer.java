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
package org.apache.logging.log4j;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.apache.logging.log4j.util.Strings;

/**
 *
 */
public class Timer implements Serializable
{
    private static final long serialVersionUID = 9175191792439630013L;

    private final String name;        // The timer's name
    private String status;            // The timer's status
    private long startTime;           // The start time
    private long elapsedTime;         // The elapsed time
    private final int iterations;
    private static long NANO_PER_SECOND = 1000000000L;
    private static long NANO_PER_MINUTE = NANO_PER_SECOND * 60;
    private static long NANO_PER_HOUR = NANO_PER_MINUTE * 60;


    /**
     * Constructor.
     * @param name the timer name.
     */
    public Timer(final String name)
    {
        this(name, 0);
    }

    /**
     * Constructor.
     *
     * @param name the timer name.
     */
    public Timer(final String name, final int iterations)
    {
        this.name = name;
        startTime = 0;
        status = "Stopped";
        this.iterations = (iterations > 0) ? iterations : 0;
    }

    /**
     * Start the timer.
     */
    public void start()
    {
        startTime = System.nanoTime();
        elapsedTime = 0;
        status = "Start";
    }

    /**
     * Stop the timer.
     */
    public void stop()
    {
        elapsedTime += System.nanoTime() - startTime;
        startTime = 0;
        status = "Stop";
    }

    /**
     * Pause the timer.
     */
    public void pause()
    {
        elapsedTime += System.nanoTime() - startTime;
        startTime = 0;
        status = "Pause";
    }

    /**
     * Resume the timer.
     */
    public void resume()
    {
        startTime = System.nanoTime();
        status = "Resume";
    }

    /**
     * Accessor for the name.
     * @return the timer's name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Access the elapsed time.
     *
     * @return the elapsed time.
     */
    public long getElapsedTime()
    {
        return elapsedTime / 1000000;
    }

    /**
     * Access the elapsed time.
     *
     * @return the elapsed time.
     */
    public long getElapsedNanoTime()
    {
        return elapsedTime;
    }

    /**
     * Returns the name of the last operation performed on this timer (Start, Stop, Pause or
     * Resume).
     * @return the string representing the last operation performed.
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * Returns the String representation of the timer based upon its current state
     */
    @Override
    public String toString()
    {
        final StringBuilder result = new StringBuilder("Timer ").append(name);
        switch (status) {
            case "Start":
                result.append(" started");
                break;
            case "Pause":
                result.append(" paused");
                break;
            case "Resume":
                result.append(" resumed");
                break;
            case "Stop":
                long nanoseconds = elapsedTime;
                // Get elapsed hours
                long hours = nanoseconds / NANO_PER_HOUR;
                // Get remaining nanoseconds
                nanoseconds = nanoseconds % NANO_PER_HOUR;
                // Get minutes
                long minutes = nanoseconds / NANO_PER_MINUTE;
                // Get remaining nanoseconds
                nanoseconds = nanoseconds % NANO_PER_MINUTE;
                // Get seconds
                long seconds = nanoseconds / NANO_PER_SECOND;
                // Get remaining nanoseconds
                nanoseconds = nanoseconds % NANO_PER_SECOND;

                String elapsed = Strings.EMPTY;

                if (hours > 0) {
                    elapsed += hours + " hours ";
                }
                if (minutes > 0 || hours > 0) {
                    elapsed += minutes + " minutes ";
                }

                DecimalFormat numFormat;
                numFormat = new DecimalFormat("#0");
                elapsed += numFormat.format(seconds) + '.';
                numFormat = new DecimalFormat("000000000");
                elapsed += numFormat.format(nanoseconds) + " seconds";
                result.append(" stopped. Elapsed time: ").append(elapsed);
                if (iterations > 0) {
                    nanoseconds = elapsedTime / iterations;
                    // Get elapsed hours
                    hours = nanoseconds / NANO_PER_HOUR;
                    // Get remaining nanoseconds
                    nanoseconds = nanoseconds % NANO_PER_HOUR;
                    // Get minutes
                    minutes = nanoseconds / NANO_PER_MINUTE;
                    // Get remaining nanoseconds
                    nanoseconds = nanoseconds % NANO_PER_MINUTE;
                    // Get seconds
                    seconds = nanoseconds / NANO_PER_SECOND;
                    // Get remaining nanoseconds
                    nanoseconds = nanoseconds % NANO_PER_SECOND;

                    elapsed = Strings.EMPTY;

                    if (hours > 0) {
                        elapsed += hours + " hours ";
                    }
                    if (minutes > 0 || hours > 0) {
                        elapsed += minutes + " minutes ";
                    }

                    numFormat = new DecimalFormat("#0");
                    elapsed += numFormat.format(seconds) + '.';
                    numFormat = new DecimalFormat("000000000");
                    elapsed += numFormat.format(nanoseconds) + " seconds";
                    result.append(" Average per iteration: ").append(elapsed);
                }
                break;
            default:
                result.append(' ').append(status);
                break;
        }
        return result.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Timer)) {
            return false;
        }

        final Timer timer = (Timer) o;

        if (elapsedTime != timer.elapsedTime) {
            return false;
        }
        if (startTime != timer.startTime) {
            return false;
        }
        if (name != null ? !name.equals(timer.name) : timer.name != null) {
            return false;
        }
        if (status != null ? !status.equals(timer.status) : timer.status != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + (status != null ? status.hashCode() : 0);
        result = 29 * result + (int) (startTime ^ (startTime >>> 32));
        result = 29 * result + (int) (elapsedTime ^ (elapsedTime >>> 32));
        return result;
    }

}
