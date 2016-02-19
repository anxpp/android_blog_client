package com.anxpp.blog;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.anxpp.blog.satellite.SatelliteMenu.SateliteClickedListener;

import com.anxpp.blog.satellite.SatelliteMenu;
import com.anxpp.blog.satellite.SatelliteMenuItem;

import java.util.ArrayList;
import java.util.List;

public class SatelliteMenuActivity extends Activity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        SatelliteMenu menu = (SatelliteMenu) findViewById(R.id.menu);
        
//		  Set from XML, possible to programmatically set        
//        float distance = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, getResources().getDisplayMetrics());
//        menu.setSatelliteDistance((int) distance);
//        menu.setExpandDuration(500);
//        menu.setCloseItemsOnClick(false);
//        menu.setTotalSpacingDegree(60);
        
        List<SatelliteMenuItem> items = new ArrayList<>();
        items.add(new SatelliteMenuItem(4, R.drawable.sat_item));
        items.add(new SatelliteMenuItem(3, R.drawable.sat_item));
        items.add(new SatelliteMenuItem(2, R.drawable.sat_item));
        items.add(new SatelliteMenuItem(1, R.drawable.sat_item));
        menu.addItems(items);        
        
        menu.setOnItemClickedListener(new SateliteClickedListener() {
			public void eventOccured(int id) {
                Toast.makeText(SatelliteMenuActivity.this,id+"",Toast.LENGTH_SHORT).show();
			}
		});
    }
}