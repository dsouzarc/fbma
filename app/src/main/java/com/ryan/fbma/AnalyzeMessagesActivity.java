package com.ryan.fbma;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;

public class AnalyzeMessagesActivity extends FragmentPagerAdapter {

    // Send time for two messages in a row (75 seconds)
    private static final int DUP_SEND_TIME = 75;
    private static final int PAGE_COUNT = 2;
    private static final int MOST_USED_WORDS = 7;
    private static final int DONE_CHAR = 15;
    private static final int MAX = 10;

    private static final DecimalFormat theFormatter = new DecimalFormat("#,###,##0.000");

    private final ConcurrentHashMap<String, Friend> everyoneMap = new ConcurrentHashMap<String, Friend>();
    private final ConcurrentHashMap<Friend, HashMap<String, Short>> wordCounter = new ConcurrentHashMap<Friend, HashMap<String, Short>>();
    private final ConcurrentHashMap<Friend, LinkedList<Message>> everyoneMessages = new ConcurrentHashMap<Friend, LinkedList<Message>>();
    private final LinkedBlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>();
    private final LinkedList<Message> theMessages = new LinkedList<Message>();

    private final Thread updateWordCounter = new Thread(new UpdateWordCount());
    private final Bundle accessTokenBundle = new Bundle();

    private SharedPreferences prefs;
    private Context theC;
    private Session theSession;

    private LinearLayout allMessagesLayout;
    private LinearLayout analysisLayout;

    private int counter;
    private int messageCounter;
    private int numFriends;
    private boolean finishedMessages = false;
    private String accessToken;
    private String[] friendNames;

    public AnalyzeMessagesActivity(final FragmentManager fm, final Context theC, final Friend[] everyone,
                                   final long threadID) {
        super(fm);

        initializeGlobalVariables(theC, everyone, threadID);
        getMessages(String.valueOf(threadID));
    }

    /** Initializes all the global variables */
    private void initializeGlobalVariables(final Context theC, final Friend[] everyone, final long threadID) {
        this.theC = theC;
        this.prefs = theC.getSharedPreferences("com.ryan.fbma", Context.MODE_PRIVATE);
        this.theSession = Session.getActiveSession();
        this.numFriends = everyone.length;
        this.friendNames = new String[numFriends];

        short counter = 0;
        for (Friend participant : everyone) {
            friendNames[counter] = participant.getName();
            everyoneMap.put(participant.getName(), participant);
            wordCounter.put(participant, new HashMap<String, Short>());
            everyoneMessages.put(participant, new LinkedList<Message>());
            counter++;
        }

        accessToken = getField("access token");
        accessTokenBundle.putString("access token", accessToken);
    }

    public class MessageAnalysis extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View rootInflater = inflater.inflate(R.layout.analyze_messages_layout, container, false);
            analysisLayout = (LinearLayout) rootInflater.findViewById(R.id.theLayout);
            return rootInflater;
        }
    }

    public class ViewAllMessages extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View rootInflater = inflater.inflate(R.layout.all_messages_layout, container, false);
            allMessagesLayout = (LinearLayout) rootInflater.findViewById(R.id.theLayout);
            updateWordCounter.start();
            log("HERE");
            return rootInflater;
        }
    }

    /** Gets the messages for the thread */
    private void getMessages(final String threadID) {
        String forMessageSending;

        try {
            forMessageSending = "/" + Long.parseLong(threadID);
        } catch (Exception e) {
            forMessageSending = threadID;
        }

        /** Request to get all of the messages */
        new Request(theSession, forMessageSending, null, HttpMethod.GET, new Request.Callback() {
            public void onCompleted(Response response) {
                if (response == null) {
                    log("NULL UP HERE");
                } else {
                    try {
                        parseData(response.getGraphObject().getInnerJSONObject().getJSONObject("comments"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        log("Response Comments: " + response.getGraphObject().getInnerJSONObject().toString());
                    }
                }
            }
        }).executeAsync();
    }

    /** Runnable that updates the HashMap of words used with the latest Messages */
    private class UpdateWordCount implements Runnable {
        @Override
        public void run() {

            while (true) {
                try {
                    final Message aMessage = messageQueue.take();
                    final HashMap<String, Short> friendsWords = wordCounter.get(aMessage.getSentBy());
                    final String[] theWords = aMessage.getMessage().split(" ");

                    for (String aWord : theWords) {
                        if (friendsWords.containsKey(aWord)) {
                            friendsWords.put(aWord, (short) (friendsWords.get(aWord) + 1));
                        } else {
                            friendsWords.put(aWord, (short) 1);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** Parses the data from the JSONObject into Messages. Adds the message to the global array */
    private void parseData(final JSONObject theMessages) {
        try {
            final JSONObject nextPage = theMessages.getJSONObject("paging");
            final JSONArray actualObjects = theMessages.getJSONArray("data");
            for (int i = 0; i < actualObjects.length(); i++) {
                try {
                    final JSONObject theMessage = actualObjects.getJSONObject(i);
                    final GregorianCalendar sentDate = toCalendar(theMessage.getString("created_time"));
                    final String messageID = theMessage.getString("id");
                    final String message = theMessage.getString("message");
                    final String sender = theMessage.getJSONObject("from").getString("name");
                    final Friend sendingFriend = everyoneMap.get(sender);

                    final Message theM = new Message(messageID, sentDate, sendingFriend, message);
                    messageQueue.add(theM);
                    this.theMessages.add(theM);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            updateDisplay();
            final String next = nextPage.getString("next");

            if (counter < MAX) {
                new getOtherMessages().execute(next);
            }
        } catch (Exception e) {
            e.printStackTrace();

            if (theMessages.toString().length() < DONE_CHAR || counter >= MAX) {
                finishedMessages = true;
                updateDisplay();
            }

            log("Parse messaging problem: " + counter + " " + e.toString() + " because : " + theMessages.toString());
        }
    }

    /** Updates the screen display with the latest messages + analytics */
    private void updateDisplay() {

        removeDuplicateMessages();

        if (finishedMessages) {
            log("UP HERE");
            makeToast("Total Messages: " + theMessages.size());
        }

        final long startTime = System.currentTimeMillis();

        for (int i = messageCounter; i < theMessages.size(); i++) {
            final Message aMessage = theMessages.get(i);
            allMessagesLayout.addView(getTV(aMessage.getSentBy().getName() + ": " + aMessage.getMessage() + " on "
                    + aMessage.getShortformSendDate(), messageCounter));
            messageCounter++;
        }
        log("Added to display: " + (System.currentTimeMillis() - startTime));
        log("On: " + counter + " out of " + MAX);

        if (counter % 3 == 0) {
            makeToast("On: " + counter + " out of " + MAX);
        }

        if (finishedMessages || counter >= MAX) {
            performDataAnalytics();
        }
    }

    /** Removes all messages sent by the same person in a row within the specified time */
    public void removeDuplicateMessages() {
        final long startTime = System.currentTimeMillis();
        Collections.sort(theMessages, theComp);

        for (int i = 0; i < theMessages.size() - 1; i++) {
            final Message currentMessage = theMessages.get(i);
            final Message nextMessage = theMessages.get(i + 1);

            if (currentMessage.getSentBy().equals(nextMessage.getSentBy())) {
                // If the messages were sent within X time if eachother
                if ((nextMessage.getMessageSendDate().get(Calendar.SECOND)
                        - currentMessage.getMessageSendDate().get(Calendar.SECOND) < DUP_SEND_TIME)) {
                    nextMessage.addMessage(currentMessage.getMessage());
                    theMessages.remove(i);
                    i--;
                }
            }
        }
        time(startTime, "Sort and remove duplicate: ");
    }

    /** Actually performs the data analytics */
    private void performDataAnalytics() {
        new UpdateDisplayMostUsedWords().execute();

        final long startTime = System.currentTimeMillis();
        Iterator<Message> theIterator = this.theMessages.iterator();
        while (theIterator.hasNext()) {
            final Message tempMessage = theIterator.next();
            everyoneMessages.get(tempMessage.getSentBy()).add(tempMessage);
        }
        time(startTime, "Added to Map");

        for (ConcurrentHashMap.Entry entry : everyoneMessages.entrySet()) {

            final Friend personFriend = (Friend) entry.getKey();

            long messageWords = 0;
            double averageWordLength = 0;

            final LinkedList<Message> personMessages = (LinkedList<Message>) entry.getValue();
            theIterator = personMessages.iterator();

            while (theIterator.hasNext()) {
                final Message tm = (Message) theIterator.next();

                final String[] mContents = tm.getMessage().split(" ");

                messageWords += mContents.length;

                double wordLength = tm.getMessage().length() - mContents.length;
                averageWordLength += (wordLength / mContents.length);

            }

            final double totalMessages = personMessages.size();

            analysisLayout.addView(getTV(
                    personFriend.getName() + " Avg. Words: " + theFormatter.format((messageWords / totalMessages)), 0));
            analysisLayout.addView(getTV(
                    personFriend.getName() + " Avg. Word Length: "
                            + theFormatter.format((averageWordLength / totalMessages)), 0));
            analysisLayout.addView(getTV(personFriend.getName() + " Total Messages: " + ((int) totalMessages), 0));

        }

    }

    /** Prints run time given start time and info to print */
    private void time(final long startTime, final String toSay) {
        log(toSay + " : " + (System.currentTimeMillis() - startTime));
    }

    /** Shows most used words on the screen */
    private class UpdateDisplayMostUsedWords extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            while (messageQueue.size() > 0) {
                // Do nothing
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            int counter = 0;

            for (ConcurrentHashMap.Entry entry : wordCounter.entrySet()) {
                final HashMap<String, Short> theMap = (HashMap<String, Short>) entry.getValue();
                final String[] mostUsed = sortHashMap(theMap);

                for (String used : mostUsed) {
                    analysisLayout.addView(getTV(((Friend) entry.getKey()).getName() + ": " + used + ". Freq: "
                            + theMap.get(used), counter));
                    counter++;
                }
            }
        }
    }

    /** Alternative sorting method to return the most used words */
    private String[] sortHashMap(final HashMap<String, Short> map) {
        final Set<String> set = map.keySet();
        final List<String> keys = new ArrayList<String>(set);

        Collections.sort(keys, new Comparator<String>() {

            @Override
            public int compare(String s1, String s2) {
                return map.get(s2).compareTo(map.get(s1));
            }
        });

        if (keys.size() >= MOST_USED_WORDS) {
            return keys.subList(0, MOST_USED_WORDS).toArray(new String[MOST_USED_WORDS]);
        } else {
            return keys.toArray(new String[keys.size()]);
        }
    }

    /** Used for sorting messages based on time (returns the one sent last) */
    private static Comparator<Message> theComp = new Comparator<Message>() {
        public int compare(Message m1, Message m2) {
            return m2.getMessageSendDate().compareTo(m1.getMessageSendDate());
        }
    };

    /** Gets the next group of messages from FB */
    private class getOtherMessages extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            final String URL = params[0];
            counter++;

            final StringBuilder builder = new StringBuilder(1000);

            final DefaultHttpClient client = new DefaultHttpClient();
            final HttpGet httpGet = new HttpGet(URL);
            try {
                final HttpResponse execute = client.execute(httpGet);
                final InputStream content = execute.getEntity().getContent();
                final BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                String s = "";
                while ((s = buffer.readLine()) != null) {
                    builder.append(s);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return builder.toString();
        }

        @Override
        protected void onPostExecute(final String result) {
            try {
                final JSONObject theMessages = new JSONObject(result);
                parseData(theMessages);
            } catch (Exception e) {
                e.printStackTrace();
                log(e.toString());
            }
        }
    }

    /** Makes toast message */
    private void makeToast(final String message) {
        Toast.makeText(theC, message, Toast.LENGTH_SHORT).show();
    }

    /** Returns simple TV */
    private TextView getTV(final String message, final int counter) {
        final TextView theTV = new TextView(theC);
        theTV.setText(message);

        if (counter % 2 == 0) {
            theTV.setTextColor(Color.BLACK);
        } else {
            theTV.setTextColor(Color.BLUE);
        }
        theTV.setTextSize(20);
        theTV.setPadding(30, 0, 0, 30);
        return theTV;
    }

    /** Returns a string field from saved preferences */
    private String getField(final String fieldName) {
        return prefs.getString(fieldName, "");
    }

    /** Log a message (error log) */
    public void log(final String message) {
        Log.e("com.ryan.fbma", message);
    }

    @Override
    public Fragment getItem(int tabSelected) {
        Bundle data = new Bundle();

        switch (tabSelected) {

            case 0:
                final ViewAllMessages theAE = new ViewAllMessages();
                data.putInt("current_page", tabSelected + 1);
                theAE.setArguments(data);
                return theAE;

            case 1:
                final MessageAnalysis allE = new MessageAnalysis();
                data.putInt("current_page", tabSelected + 1);
                allE.setArguments(data);
                return allE;

        }
        return null;
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

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
