package org.apache.logging.log4j.nosql.appender;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.junit.CleanFolders;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class LuceneAppenderTest {
	private static final String CONFIGURATION_FILE = "log4j2-lucene.xml";
	private static final String TARGET_FOLDER = "target/lucene";
	private static final String LEVEL = "level";
	private static final String CONTENT = "content";
	private static final String LOGGER_NAME = "TestLogger";
	private static final String LOG_MESSAGE = "Hello world!";
	private static final String EXEPECTED_REGEX = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3} \\[[^\\]]*\\] INFO "
			+ LOGGER_NAME + " - " + LOG_MESSAGE;
	private static final Path PATH = Paths.get(TARGET_FOLDER);
	private static final int THREAD_COUNT = 50;

	@Rule
	public LoggerContextRule ctx = new LoggerContextRule(CONFIGURATION_FILE);

	@Rule
	public CleanFolders folders = new CleanFolders(PATH);

	@Test
	public void testSimple() throws Exception {
		write();
		verify(1);
	}

	@Test
	public void testMultipleThreads() throws Exception {
		final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_COUNT);
		final LuceneAppenderRunner runner = new LuceneAppenderRunner();
		for (int i = 0; i < THREAD_COUNT; ++i) {
			threadPool.execute(runner);
		}

		// Waiting for lucene records to complete and submit
		Thread.sleep(3000);

		verify(THREAD_COUNT);
	}

	private final void write() throws Exception {
		final LuceneAppender appender = (LuceneAppender) ctx.getRequiredAppender("LuceneAppender");
		try {
			appender.start();
			assertTrue("Appender did not start", appender.isStarted());
			final Log4jLogEvent event = Log4jLogEvent.newBuilder().setLoggerName(LOGGER_NAME)
					.setLoggerFqcn(LuceneAppenderTest.class.getName()).setLevel(Level.INFO)
					.setMessage(new SimpleMessage(LOG_MESSAGE)).build();
			appender.append(event);
		} finally {
			appender.stop();
		}
		assertFalse("Appender did not stop", appender.isStarted());
	}

	private final synchronized void verify(final int exepectedTotalHits) throws Exception {
		final FSDirectory fsDir = FSDirectory.open(PATH);
		final IndexReader reader = DirectoryReader.open(fsDir);
		try {
			final IndexSearcher searcher = new IndexSearcher(reader);
			final TopDocs all = searcher.search(new MatchAllDocsQuery(), Integer.MAX_VALUE);
			Assert.assertEquals(all.totalHits, exepectedTotalHits);
			for (ScoreDoc scoreDoc : all.scoreDocs) {
				final Document doc = searcher.doc(scoreDoc.doc);
				Assert.assertEquals(doc.getFields().size(), 3);
				final String field1 = doc.get(LEVEL);
				Assert.assertTrue("Unexpected field1: " + field1, Level.INFO.toString().equals(field1));
				final String field2 = doc.get(CONTENT);
				final Pattern pattern = Pattern.compile(EXEPECTED_REGEX);
				final Matcher matcher = pattern.matcher(field2);
				Assert.assertTrue("Unexpected field2: " + field2, matcher.matches());
			}
		} finally {
			reader.close();
			fsDir.close();
		}
	}

	private class LuceneAppenderRunner implements Runnable {
		@Override
		public void run() {
			try {
				write();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
