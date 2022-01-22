/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.commons.mail.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;
import javax.mail.util.ByteArrayDataSource;

/**
 * Parses a MimeMessage and stores the individual parts such a plain text,
 * HTML text and attachments.
 *
 * Copied from "org.apache.commons:commons-email:1.5" because recent versions
 * of commons-email are not available in Maven. No modifications were made.
 *
 */
public class MimeMessageParser
{
    /** The MimeMessage to convert */
    private final MimeMessage mimeMessage;

    /** Plain mail content from MimeMessage */
    private String plainContent;

    /** Html mail content from MimeMessage */
    private String htmlContent;

    /** List of attachments of MimeMessage */
    private final List<DataSource> attachmentList;

    /** Attachments stored by their content-id */
    private final Map<String, DataSource> cidMap;

    /** Is this a Multipart email */
    private boolean isMultiPart;

    /**
     * Constructs an instance with the MimeMessage to be extracted.
     *
     * @param message the message to parse
     */
    public MimeMessageParser(final MimeMessage message)
    {
        attachmentList = new ArrayList<>();
        cidMap = new HashMap<>();
        this.mimeMessage = message;
        this.isMultiPart = false;
    }

    /**
     * Does the actual extraction.
     *
     * @return this instance
     * @throws Exception parsing the mime message failed
     */
    public MimeMessageParser parse() throws Exception
    {
        this.parse(null, mimeMessage);
        return this;
    }

    /**
     * @return the 'to' recipients of the message
     * @throws Exception determining the recipients failed
     */
    public List<javax.mail.Address> getTo() throws Exception
    {
        final javax.mail.Address[] recipients = this.mimeMessage.getRecipients(Message.RecipientType.TO);
        return recipients != null ? Arrays.asList(recipients) : new ArrayList<javax.mail.Address>();
    }

    /**
     * @return the 'cc' recipients of the message
     * @throws Exception determining the recipients failed
     */
    public List<javax.mail.Address> getCc() throws Exception
    {
        final javax.mail.Address[] recipients = this.mimeMessage.getRecipients(Message.RecipientType.CC);
        return recipients != null ? Arrays.asList(recipients) : new ArrayList<javax.mail.Address>();
    }

    /**
     * @return the 'bcc' recipients of the message
     * @throws Exception determining the recipients failed
     */
    public List<javax.mail.Address> getBcc() throws Exception
    {
        final javax.mail.Address[] recipients = this.mimeMessage.getRecipients(Message.RecipientType.BCC);
        return recipients != null ? Arrays.asList(recipients) : new ArrayList<javax.mail.Address>();
    }

    /**
     * @return the 'from' field of the message
     * @throws Exception parsing the mime message failed
     */
    public String getFrom() throws Exception
    {
        final javax.mail.Address[] addresses = this.mimeMessage.getFrom();
        if (addresses == null || addresses.length == 0)
        {
            return null;
        }
        return ((InternetAddress) addresses[0]).getAddress();
    }

    /**
     * @return the 'replyTo' address of the email
     * @throws Exception parsing the mime message failed
     */
    public String getReplyTo() throws Exception
    {
        final javax.mail.Address[] addresses = this.mimeMessage.getReplyTo();
        if (addresses == null || addresses.length == 0)
        {
            return null;
        }
        return ((InternetAddress) addresses[0]).getAddress();
    }

    /**
     * @return the mail subject
     * @throws Exception parsing the mime message failed
     */
    public String getSubject() throws Exception
    {
        return this.mimeMessage.getSubject();
    }

    /**
     * Extracts the content of a MimeMessage recursively.
     *
     * @param parent the parent multi-part
     * @param part   the current MimePart
     * @throws MessagingException parsing the MimeMessage failed
     * @throws IOException        parsing the MimeMessage failed
     */
    protected void parse(final Multipart parent, final MimePart part)
            throws MessagingException, IOException
    {
        if (isMimeType(part, "text/plain") && plainContent == null
                && !Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))
        {
            plainContent = (String) part.getContent();
        }
        else
        {
            if (isMimeType(part, "text/html") && htmlContent == null
                    && !Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))
            {
                htmlContent = (String) part.getContent();
            }
            else
            {
                if (isMimeType(part, "multipart/*"))
                {
                    this.isMultiPart = true;
                    final Multipart mp = (Multipart) part.getContent();
                    final int count = mp.getCount();

                    // iterate over all MimeBodyPart

                    for (int i = 0; i < count; i++)
                    {
                        parse(mp, (MimeBodyPart) mp.getBodyPart(i));
                    }
                }
                else
                {
                    final String cid = stripContentId(part.getContentID());
                    final DataSource ds = createDataSource(parent, part);
                    if (cid != null)
                    {
                        this.cidMap.put(cid, ds);
                    }
                    this.attachmentList.add(ds);
                }
            }
        }
    }

    /**
     * Strips the content id of any whitespace and angle brackets.
     * @param contentId the string to strip
     * @return a stripped version of the content id
     */
    private String stripContentId(final String contentId)
    {
        if (contentId == null)
        {
            return null;
        }
        return contentId.trim().replaceAll("[\\<\\>]", "");
    }

    /**
     * Checks whether the MimePart contains an object of the given mime type.
     *
     * @param part     the current MimePart
     * @param mimeType the mime type to check
     * @return {@code true} if the MimePart matches the given mime type, {@code false} otherwise
     * @throws MessagingException parsing the MimeMessage failed
     * @throws IOException        parsing the MimeMessage failed
     */
    private boolean isMimeType(final MimePart part, final String mimeType)
            throws MessagingException, IOException
    {
        // Do not use part.isMimeType(String) as it is broken for MimeBodyPart
        // and does not really check the actual content type.

        try
        {
            final ContentType ct = new ContentType(part.getDataHandler().getContentType());
            return ct.match(mimeType);
        }
        catch (final ParseException ex)
        {
            return part.getContentType().equalsIgnoreCase(mimeType);
        }
    }

    /**
     * Parses the MimePart to create a DataSource.
     *
     * @param parent the parent multi-part
     * @param part   the current part to be processed
     * @return the DataSource
     * @throws MessagingException creating the DataSource failed
     * @throws IOException        creating the DataSource failed
     */
    protected DataSource createDataSource(final Multipart parent, final MimePart part)
            throws MessagingException, IOException
    {
        final DataHandler dataHandler = part.getDataHandler();
        final DataSource dataSource = dataHandler.getDataSource();
        final String contentType = getBaseMimeType(dataSource.getContentType());
        final byte[] content = this.getContent(dataSource.getInputStream());
        final ByteArrayDataSource result = new ByteArrayDataSource(content, contentType);
        final String dataSourceName = getDataSourceName(part, dataSource);

        result.setName(dataSourceName);
        return result;
    }

    /** @return Returns the mimeMessage. */
    public MimeMessage getMimeMessage()
    {
        return mimeMessage;
    }

    /** @return Returns the isMultiPart. */
    public boolean isMultipart()
    {
        return isMultiPart;
    }

    /** @return Returns the plainContent if any */
    public String getPlainContent()
    {
        return plainContent;
    }

    /** @return Returns the attachmentList. */
    public List<DataSource> getAttachmentList()
    {
        return attachmentList;
    }

    /**
     * Returns a collection of all content-ids in the parsed message.
     * <p>
     * The content-ids are stripped of any angle brackets, i.e. "part1" instead
     * of "&lt;part1&gt;".
     *
     * @return the collection of content ids.
     * @since 1.3.4
     */
    public Collection<String> getContentIds()
    {
        return Collections.unmodifiableSet(cidMap.keySet());
    }

    /** @return Returns the htmlContent if any */
    public String getHtmlContent()
    {
        return htmlContent;
    }

    /** @return true if a plain content is available */
    public boolean hasPlainContent()
    {
        return this.plainContent != null;
    }

    /** @return true if HTML content is available */
    public boolean hasHtmlContent()
    {
        return this.htmlContent != null;
    }

    /** @return true if attachments are available */
    public boolean hasAttachments()
    {
        return !this.attachmentList.isEmpty();
    }

    /**
     * Find an attachment using its name.
     *
     * @param name the name of the attachment
     * @return the corresponding datasource or null if nothing was found
     */
    public DataSource findAttachmentByName(final String name)
    {
        DataSource dataSource;

        for (final DataSource element : getAttachmentList()) {
            dataSource = element;
            if (name.equalsIgnoreCase(dataSource.getName()))
            {
                return dataSource;
            }
        }

        return null;
    }

    /**
     * Find an attachment using its content-id.
     * <p>
     * The content-id must be stripped of any angle brackets,
     * i.e. "part1" instead of "&lt;part1&gt;".
     *
     * @param cid the content-id of the attachment
     * @return the corresponding datasource or null if nothing was found
     * @since 1.3.4
     */
    public DataSource findAttachmentByCid(final String cid)
    {
        final DataSource dataSource = cidMap.get(cid);
        return dataSource;
    }

    /**
     * Determines the name of the data source if it is not already set.
     *
     * @param part the mail part
     * @param dataSource the data source
     * @return the name of the data source or {@code null} if no name can be determined
     * @throws MessagingException accessing the part failed
     * @throws UnsupportedEncodingException decoding the text failed
     */
    protected String getDataSourceName(final Part part, final DataSource dataSource)
            throws MessagingException, UnsupportedEncodingException
    {
        String result = dataSource.getName();

        if (result == null || result.isEmpty())
        {
            result = part.getFileName();
        }

        if (result != null && !result.isEmpty())
        {
            result = MimeUtility.decodeText(result);
        }
        else
        {
            result = null;
        }

        return result;
    }

    /**
     * Read the content of the input stream.
     *
     * @param is the input stream to process
     * @return the content of the input stream
     * @throws IOException reading the input stream failed
     */
    private byte[] getContent(final InputStream is)
            throws IOException
    {
        int ch;
        byte[] result;

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BufferedInputStream isReader = new BufferedInputStream(is);
        final BufferedOutputStream osWriter = new BufferedOutputStream(os);

        while ((ch = isReader.read()) != -1)
        {
            osWriter.write(ch);
        }

        osWriter.flush();
        result = os.toByteArray();
        osWriter.close();

        return result;
    }

    /**
     * Parses the mimeType.
     *
     * @param fullMimeType the mime type from the mail api
     * @return the real mime type
     */
    private String getBaseMimeType(final String fullMimeType)
    {
        final int pos = fullMimeType.indexOf(';');
        if (pos >= 0)
        {
            return fullMimeType.substring(0, pos);
        }
        return fullMimeType;
    }
}
