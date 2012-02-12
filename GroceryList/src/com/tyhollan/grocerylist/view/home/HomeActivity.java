package com.tyhollan.grocerylist.view.home;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.tyhollan.grocerylist.R;
import com.tyhollan.grocerylist.model.AppNameApplication;

public class HomeActivity extends FragmentActivity
{
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_home);
   }
   
   @Override
   public void onDestroy()
   {
      ((AppNameApplication) getApplicationContext()).onDestroy();
      super.onDestroy();
   }
}
