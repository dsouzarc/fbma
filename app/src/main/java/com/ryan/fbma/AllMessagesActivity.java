package com.ryan.fbma;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class AllMessagesActivity extends FragmentActivity {

    private ActionBar theActionBar;
    private ViewPager theViewPager;
    private Context theC;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_messages);
        theViewPager = (ViewPager) findViewById(R.id.theViewPager);

        theC = getApplicationContext();
        theActionBar = getActionBar();
        theActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        prefs = this.getSharedPreferences("com.ryan.fbma", Context.MODE_PRIVATE);

        final Inbox_Thread temp = (Inbox_Thread) getIntent().getExtras().getSerializable("data");
        final Friend[] temp1 = temp.getNames();
        final long threadID = temp.getThreadID();

        final Friend[] everyone = new Friend[temp1.length + 1];

        final StringBuilder activityBarTitle = new StringBuilder("");
        for (int i = 0; i < temp1.length; i++) {
            everyone[i] = temp1[i];
            activityBarTitle.append(everyone[i].getName() + " ");
        }

        everyone[temp1.length] = new Friend(prefs.getLong("id", 0), getField("name"));
        theActionBar.setTitle(activityBarTitle.toString());

        final FragmentManager theManager = getSupportFragmentManager();

        // listener for pageChange
        final ViewPager.SimpleOnPageChangeListener thePageListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                theActionBar.setSelectedNavigationItem(position);
            }
        };

        // Set the page listener to the view listener
        theViewPager.setOnPageChangeListener(thePageListener);

        // Create FragmentPageAdapter
        final AnalyzeMessagesActivity fragmentPagerAdapter = new AnalyzeMessagesActivity(theManager, theC, everyone,
                threadID);

        theViewPager.setAdapter(fragmentPagerAdapter);
        theActionBar.setDisplayShowTitleEnabled(true);

        // Tab listener
        final ActionBar.TabListener tabListener = new ActionBar.TabListener() {

            @Override
            public void onTabReselected(Tab arg0, android.app.FragmentTransaction arg1) {

            }

            @Override
            public void onTabSelected(Tab tab, FragmentTransaction ft) {
                theViewPager.setCurrentItem(tab.getPosition());

                switch (tab.getPosition()) {
                    case 0:
                        // theActionBar.setTitle("All Messages");
                        break;
                    case 1:
                        // theActionBar.setTitle("Analysis");
                        break;

                    default:
                        // theActionBar.setTitle("Facebook Message Analyzer");
                        break;
                }
            }

            @Override
            public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {
            }
        };
        // Create the tabs
        Tab tab = theActionBar.newTab().setText("All Messages").setTabListener(tabListener);
        theActionBar.addTab(tab);

        tab = theActionBar.newTab().setText("Analysis").setTabListener(tabListener);
        theActionBar.addTab(tab);
    }

    /** Returns a string field from saved preferences */
    private String getField(final String fieldName) {
        return prefs.getString(fieldName, "");
    }

    public void log(final String message) {
        Log.e("com.ryan.fbma", message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.all_messages, menu);
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
