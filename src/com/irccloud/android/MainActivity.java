package com.irccloud.android;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.widget.ArrayAdapter;

public class MainActivity extends SherlockFragmentActivity implements ActionBar.TabListener {
	NetworkConnection conn;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        final ActionBar ab = getSupportActionBar();

        ab.setListNavigationCallbacks(ArrayAdapter
                .createFromResource(this, R.array.sections,
                        R.layout.sherlock_spinner_dropdown_item),
                new OnNavigationListener() {
                    public boolean onNavigationItemSelected(int itemPosition,
                            long itemId) {
                        // FIXME add proper implementation
                        return false;
                    }
                });
        
        ab.setDisplayShowTitleEnabled(false);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
    }

    public void onResume() {
    	super.onResume();
    	
    	conn = NetworkConnection.getInstance();
    	
    	if(conn.getState() == NetworkConnection.NETWORK_STATE_DISCONNECTED)
    		conn.connect("ba1938a9bc9a3b682adeaebba8c16892");
    }
    
    public void onPause() {
    	super.onPause();

    	if(conn != null)
    		conn.disconnect();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		
	}
    
}
