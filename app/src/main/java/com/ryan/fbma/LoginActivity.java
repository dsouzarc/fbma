package com.ryan.fbma;

import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphObject;

public class LoginActivity extends Activity {

    private static final int numThreads = 9;

    private TextView theView;
    private final List<String> PERMISSIONS = Arrays.asList("read_mailbox", "user_about_me");

    private SharedPreferences prefs;
    private String myName;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        theView = (TextView) findViewById(R.id.theV);

        prefs = this.getSharedPreferences("com.ryan.fbma", Context.MODE_PRIVATE);
        myName = getValue("name");
        editor = prefs.edit();

        loginFacebook();
    }

    private void loginFacebook() {
        Session.openActiveSession(this, true, PERMISSIONS, new Session.StatusCallback() {
            @Override
            public void call(final Session session, SessionState state, Exception exception) {
                if (session.isOpened()) {

                    final Bundle accessToken = new Bundle();
                    accessToken.putString("access token", session.getAccessToken());
                    save("access token", session.getAccessToken());
                    new Request(session, "/me", accessToken, HttpMethod.GET, new Request.Callback() {
                        public void onCompleted(Response response) {
                            if (response == null) {
                                log("Response null");
                                return;
                            }

                            if (response.getGraphObject() == null) {
                                log("Graph object null");
                                log(response.toString());
                                return;
                            }

                            try {
                                final JSONObject test = response.getGraphObject().getInnerJSONObject();
                                getInbox(test.getString("name"), session, accessToken);
                            } catch (Exception e) {
                                e.printStackTrace();
                                log(e.toString());
                            }

                        }
                    }).executeAsync();
                }
            }
        });
    }

    private String getValue(final String id) {
        return prefs.getString(id, "");
    }

    private void save(final String id, final String name) {
        editor.putString(id, name);
        editor.commit();
    }

    private void save(final String id, final long value) {
        editor.putLong(id, value);
        editor.commit();
    }

    private void getInbox(final String myName, final Session session, final Bundle accessToken) {
        new Request(session, "me/inbox?fields=id,to&limit=" + numThreads, accessToken, HttpMethod.GET,
                new Request.Callback() {
                    @Override
                    public void onCompleted(Response response) {
                        save("name", myName);

                        final Intent toInbox = new Intent(LoginActivity.this, InboxActivity.class);

                        if (response == null) {
                            startProblemActivity(toInbox);
                        }

                        final GraphObject theResponse = response.getGraphObject();

                        if (theResponse == null) {
                            startProblemActivity(toInbox);
                        }

                        final JSONObject theInbox = theResponse.getInnerJSONObject();

                        if (theInbox == null) {
                            startProblemActivity(toInbox);
                        }

                        try {
                            final JSONArray theThreads = theInbox.getJSONArray("data");
                            final Inbox_Thread[] theInboxThreads = new Inbox_Thread[theThreads.length()];

                            for (int i = 0; i < theThreads.length(); i++) {
                                final JSONObject aThread = theThreads.getJSONObject(i);
                                final JSONArray participants = aThread.getJSONObject("to").getJSONArray("data");

                                final long conversationID = Long.parseLong(aThread.getString("id"));
                                final Friend[] participantFriends = new Friend[participants.length() - 1];

                                if (participants.length() >= 2) {
                                    int counter = 0;
                                    for (int y = 0; y < participants.length(); y++) {
                                        final String name = participants.getJSONObject(y).getString("name");

                                        if (!name.equals(myName)) {
                                            final JSONObject subsection = participants.getJSONObject(y);
                                            participantFriends[counter] = new Friend(subsection.getLong("id"),
                                                    subsection.getString("name"));
                                            counter++;
                                        } else {
                                            save("id", participants.getJSONObject(y).getLong("id"));
                                        }
                                    }
                                }

                                theInboxThreads[i] = new Inbox_Thread(participantFriends, conversationID);
                            }

                            toInbox.putExtra("inbox", theInboxThreads);
                            startActivity(toInbox);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            log(e.toString());
                            startProblemActivity(toInbox);
                        }
                    }
                }).executeAsync();

    }

    private void startProblemActivity(final Intent problemIntent) {
        problemIntent.putExtra("names", new String[] { "" });
        problemIntent.putExtra("ids", new Integer[] { 0 });
        startActivity(problemIntent);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    private void log(final String message) {
        Log.e("com.ryan.fbma", message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.inbox, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
