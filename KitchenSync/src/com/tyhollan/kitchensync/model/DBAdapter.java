package com.tyhollan.kitchensync.model;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.ContactsContract.Data;
import android.util.Log;

public class DBAdapter
{

   // Event fields
   public static final String  KEY_ID           = BaseColumns._ID;
   public static final String  KEY_ITEMNAME     = "itemname";
   public static final String  KEY_AMOUNT       = "amount";
   public static final String  KEY_STORE        = "store";
   public static final String  KEY_CATEGORY     = "category";
   public static final String  KEY_ROWINDEX     = "rowindex";
   public static final String  KEY_CROSSEDOFF   = "crossedoff";

   private static final String TAG              = "DBAdapter";
   private DatabaseHelper      mDbHelper;
   private SQLiteDatabase      mDb;

   /**
    * Database creation sql statement
    */
   private static final String DATABASE_NAME    = "data";
   private static final String GROCERY_TABLE    = "grocerylist";
   private static final String GROCERY_CREATE   = "create table " + GROCERY_TABLE + " (" + KEY_ID
                                                      + " integer primary key " + "autoincrement, " + KEY_ITEMNAME
                                                      + " text not null, " + KEY_AMOUNT + " text not null, "
                                                      + KEY_STORE + " text not null, " + KEY_CATEGORY
                                                      + " text not null, " + KEY_ROWINDEX + " text not null, "
                                                      + KEY_CROSSEDOFF + " integer not null, " + "UNIQUE " + " ("
                                                      + KEY_ITEMNAME + " )" + ");";

   private static final int    DATABASE_VERSION = 4;

   private final Context       mCtx;

   private class DatabaseHelper extends SQLiteOpenHelper
   {

      DatabaseHelper(Context context)
      {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
      }

      @Override
      public void onCreate(SQLiteDatabase db)
      {
         Log.i(TAG, "In DatabaseHelper.onCreate");
         db.execSQL(GROCERY_CREATE);
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
      {
         // TODO: Make this alter tables instead of drop everything
         Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
               + ", which will destroy all data");
         Log.i(TAG, "DB version is: " + db.getVersion());
         db.execSQL("drop table " + GROCERY_TABLE + ";");
         db.execSQL(GROCERY_CREATE);
      }
   }

   /**
    * Constructor - takes the context to allow the database to be opened/created
    * 
    * @param ctx
    *           the Context within which to work
    */
   public DBAdapter(Context ctx)
   {
      this.mCtx = ctx;
   }

   /**
    * Open the database. If it cannot be opened, try to create a new instance of
    * the database. If it cannot be created, throw an exception to signal the
    * failure
    * 
    * @return this (self reference, allowing this to be chained in an
    *         initialization call)
    * @throws SQLException
    *            if the database could be neither opened or created
    */
   public DBAdapter open() throws SQLException
   {
      Log.i(TAG, "In DatabaseHelper.open");
      mDbHelper = new DatabaseHelper(mCtx);
      mDb = mDbHelper.getWritableDatabase();
      return this;
   }

   public void close()
   {
      Log.i(TAG, "In DatabaseHelper.close");
      mDbHelper.close();
   }

   /**
    * Add a grocery item to the database
    */
   public long saveGroceryItem(GroceryItem item)
   {
      Log.i(TAG, "In DatabaseHelper.saveGroceryItem");
      ContentValues initialValues = new ContentValues();
      initialValues.put(KEY_ITEMNAME, item.getItemName());
      initialValues.put(KEY_AMOUNT, item.getAmount());
      initialValues.put(KEY_STORE, item.getStore());
      initialValues.put(KEY_CATEGORY, item.getCategory());
      if (item.getRowIndex() != null)
      {
         initialValues.put(KEY_ROWINDEX, item.getRowIndex());
      }
      else
      {
         initialValues.put(KEY_ROWINDEX, "");
      }
      initialValues.put(KEY_CROSSEDOFF, item.isCrossedOff());
      return mDb.replace(GROCERY_TABLE, null, initialValues);
   }

   /**
    * Delete the event with the given rowId
    * 
    * @param rowId
    *           id of note to delete
    * @return true if deleted, false otherwise
    */
   public boolean deleteGroceryItem(GroceryItem item)
   {
      return mDb.delete(GROCERY_TABLE, KEY_ID + "=" + item.getId(), null) > 0;
   }

   public void deleteAll()
   {
      mDb.execSQL("drop table " + GROCERY_TABLE + ";");
      mDb.execSQL(GROCERY_CREATE);
   }

   /**
    * Return an ArrayList of all notes in the database
    * 
    * @return ArrayList<Note> All Notes in the database
    */
   public ArrayList<GroceryItem> getFullList()
   {
      Cursor cursor = mDb.rawQuery("SELECT * FROM " + GROCERY_TABLE, null);
      return makeListFromCursor(cursor);
   }
   
   public ArrayList<GroceryItem> getGroceryList()
   {
      Cursor cursor = mDb.query(GROCERY_TABLE, null, KEY_CROSSEDOFF + "=0", null, null, null, null);
      return makeListFromCursor(cursor);
   }
   
   public ArrayList<GroceryItem> getCrossedOffList()
   {
      Cursor cursor = mDb.query(GROCERY_TABLE, null, KEY_CROSSEDOFF + "=1", null, null, null, null);
      return makeListFromCursor(cursor);
   }
   
   private ArrayList<GroceryItem> makeListFromCursor(Cursor cursor)
   {
      int id = cursor.getColumnIndexOrThrow(KEY_ID);
      int itemname = cursor.getColumnIndexOrThrow(KEY_ITEMNAME);
      int amount = cursor.getColumnIndexOrThrow(KEY_AMOUNT);
      int store = cursor.getColumnIndexOrThrow(KEY_STORE);
      int group = cursor.getColumnIndexOrThrow(KEY_CATEGORY);
      int rowindex = cursor.getColumnIndexOrThrow(KEY_ROWINDEX);
      int crossedoff = cursor.getColumnIndexOrThrow(KEY_CROSSEDOFF);
      
      ArrayList<GroceryItem> list = new ArrayList<GroceryItem>();
      if (cursor.moveToFirst())
      {
         do
         {
            list.add(new GroceryItem(cursor.getLong(id), cursor.getString(itemname), cursor
                  .getString(amount), cursor.getString(store), cursor.getString(group), cursor
                  .getString(rowindex), cursor.getInt(crossedoff)>0));
         } while (cursor.moveToNext());
      }
      cursor.close();
      return list;
   }

   public Cursor getGroceryCursor()
   {
      return mDb.rawQuery("SELECT * FROM " + GROCERY_TABLE, null);
   }
}
