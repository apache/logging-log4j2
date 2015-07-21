/*
 * Copyright 2015 Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender.rolling;

import java.io.File;
import java.nio.file.Files;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import org.apache.logging.log4j.Logger;
import static org.apache.logging.log4j.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasName;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RollingAppenderMaxAgeTest {
    
    private static final String DIR = "target/rolling1";

    private final String fileExtension;

    private Logger logger;

    @Parameterized.Parameters(name = "{0} \u2192 {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][]{
                        { "log4j-rolling-maxage.xml", ".gz" },
                }
        );
    }

    @Rule
    public InitialLoggerContext init;

    public RollingAppenderMaxAgeTest(final String configFile, final String fileExtension) {
        this.fileExtension = fileExtension;
        this.init = new InitialLoggerContext(configFile);
    }

    @Before
    public void setUp() throws Exception {
        this.logger = this.init.getLogger(RollingAppenderSizeTest.class.getName());
    }

    @After
    public void tearDown() throws Exception {
        deleteDir();
    }

    @Test
    public void testAppender() throws Exception {
        for (int i=0; i < 100; ++i) {
            logger.debug("This is test message number " + i);
        }
        final File dir = new File(DIR);
        assertTrue("Directory not created", dir.exists() && dir.listFiles().length > 0);
        final File[] files = dir.listFiles();
	Thread.sleep(50);
        assertNotNull(files);
	assertEquals(8, files.length);
	// change creation time
	
	Calendar cal = Calendar.getInstance();
	cal.add(Calendar.DATE, -5);
	FileTime fileTime = FileTime.fromMillis(cal.getTimeInMillis());
	
	for (File file : files) {
	    if (file.getName().endsWith(".zip")) {
		BasicFileAttributeView attributes = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class);
		attributes.setTimes(fileTime, fileTime, fileTime);
	    }
	}
	
	for (int i=0; i < 2; ++i) {
            logger.debug("This is test message number " + i);
	    Thread.sleep(50);
        }
	
	final File dir2 = new File(DIR);
	final File[] filesNew = dir2.listFiles();
	assertNotNull(filesNew);
	assertEquals(2, files.length);
    }

    private static void deleteDir() {
        final File dir = new File(DIR);
        if (dir.exists()) {
            final File[] files = dir.listFiles();
            for (final File file : files) {
                file.delete();
            }
            dir.delete();
        }
    }
}
