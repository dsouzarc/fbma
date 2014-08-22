package com.ryan.fbma;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InboxActivity extends Activity {

    private LinearLayout theLayout;
    private Context theC;

    private Inbox_Thread[] theThreads;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);
        theC = getApplicationContext();

        theLayout = (LinearLayout) findViewById(R.id.theLayout);
        final Object[] temps = (Object[]) getIntent().getExtras().getSerializable("inbox");

        theThreads = new Inbox_Thread[temps.length];

        for (int i = 0; i < temps.length; i++) {
            theThreads[i] = (Inbox_Thread) temps[i];
        }

        for (int i = 0; i < theThreads.length; i++) {
            theLayout.addView(getTV(theThreads[i], i));
        }

    }

    private void log(final String message) {
        Log.e("com.ryan.fbma", message);
    }

    private class theListener implements View.OnClickListener {
        private final Inbox_Thread theThread;

        public theListener(final Inbox_Thread theThread) {
            this.theThread = theThread;
        }

        @Override
        public void onClick(final View view) {
            final Intent toAnalyze = new Intent(InboxActivity.this, AllMessagesActivity.class); // AnalyzeThreadActivity.class);
            toAnalyze.putExtra("data", theThread);
            startActivity(toAnalyze);
        }
    }

    private TextView getTV(final Inbox_Thread aThread, final int counter) {
        final String[] names = aThread.getNamesString();

        StringBuilder allNames = new StringBuilder("");

        for (String name : names) {
            allNames.append(name + " ");
        }

        final TextView theTV = new TextView(theC);

        theTV.setText(allNames);

        if (counter % 2 == 0) {
            theTV.setTextColor(Color.BLACK);
        } else {
            theTV.setTextColor(Color.BLUE);
        }
        theTV.setTextSize(20);
        theTV.setPadding(30, 0, 0, 30);
        theTV.setOnClickListener(new theListener(aThread));
        return theTV;
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
