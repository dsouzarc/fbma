package com.ryan.fbma;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Message {

    private final GregorianCalendar messageSendDate;
    private final Friend sentBy;
    private String message;
    private final String messageID;

    public Message(final String messageID, GregorianCalendar messageSendDate, Friend sentBy, String message) {
        this.messageID = messageID;
        this.messageSendDate = messageSendDate;
        this.sentBy = sentBy;
        this.message = message;
    }

    /** Transform ISO 8601 string to Calendar. */
    public static GregorianCalendar toCalendar(final String iso8601string) {
        try {
            Calendar calendar = GregorianCalendar.getInstance();
            String s = iso8601string.replace("Z", "+00:00");
            s = s.substring(0, 22) + s.substring(23); // to get rid of the ":"
            Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
            calendar.setTime(date);
            return (GregorianCalendar) calendar;
        } catch (Exception e) {
            return new GregorianCalendar();
        }
    }

    public String getShortformSendDate() {
        final int month = messageSendDate.get(Calendar.MONTH) + 1;
        final int day = messageSendDate.get(Calendar.DAY_OF_MONTH);
        final int year = messageSendDate.get(Calendar.YEAR);
        final int hour = messageSendDate.get(Calendar.HOUR_OF_DAY);
        final int minute = messageSendDate.get(Calendar.MINUTE);

        return hour + ":" + minute + " on " + month + "/" + day + "/" + year;
    }

    public void addMessage(final String otherMessage) {
        this.message += " " + otherMessage; // = otherMessage + " " + this.message;
    }

    /**
     * @return the messageSendDate
     */
    public GregorianCalendar getMessageSendDate() {
        return messageSendDate;
    }

    /**
     * @return the sentBy
     */
    public Friend getSentBy() {
        return sentBy;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the message ID
     */
    public String getMessageID() {
        return this.messageID;
    }

    @Override
    public String toString() {
        return "From: " + sentBy.toString() + " on " + getShortformSendDate() + " Message:" + this.message;
    }

    public int compareTo(final Object theOther) {
        return this.messageSendDate.compareTo(((Message) theOther).getMessageSendDate());
    }
}
