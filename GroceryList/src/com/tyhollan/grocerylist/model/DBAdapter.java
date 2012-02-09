package com.tyhollan.grocerylist.model;

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

   private static final String TAG              = "DBAdapter";
   private DatabaseHelper      mDbHelper;
   private SQLiteDatabase      mDb;
   private GoogleDocsAdapter   mDocsAdapter;

   /**
    * Database creation sql statement
    */
   private static final String DATABASE_NAME    = "data";
   private static final String GROCERY_TABLE    = "grocerylist";
   private static final String GROCERY_CREATE   = "create table " + GROCERY_TABLE + " (" + KEY_ID
                                                      + " integer primary key " + "autoincrement, " + KEY_ITEMNAME
                                                      + " text not null, " + KEY_AMOUNT + " text not null, "
                                                      + KEY_STORE + " text not null, " + KEY_CATEGORY
                                                      + " text not null, " + "UNIQUE " + " (" + KEY_ITEMNAME + " )"
                                                      + ");";

   private static final int    DATABASE_VERSION = 2;

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

   public void sync()
   {
      sync(null);
   }

   public void sync(final Cursor cursor)
   {
      if(isGDocsSyncingEnabled())
      {
         new SyncGroceryList().execute(cursor);
      }
   }
   
   private class SyncGroceryList extends AsyncTask<Cursor, Void, Void>
   {
      Cursor cursor;
      @Override
      protected Void doInBackground(Cursor... params)
      {
         mDocsAdapter = new GoogleDocsAdapter(mCtx);
         cursor = params[0];
         deleteAll();
         GroceryList list = mDocsAdapter.getGroceryList();
         for (GroceryItem item : list.getGroceryList())
         {
            saveGroceryItem(item);
         }
         return null;
      }
      
      @Override
      protected void onPostExecute(Void result)
      {
         cursor.requery();
      }
   }

   /**
    * Add a grocery item to the database
    */
   public long saveGroceryItem(GroceryItem item)
   {
      if(isGDocsSyncingEnabled())
      {
         //mDocsAdapter.saveGroceryItem(item);
      }
      Log.i(TAG, "In DatabaseHelper.saveGroceryItem");
      ContentValues initialValues = new ContentValues();
      initialValues.put(KEY_ITEMNAME, item.getItemName());
      initialValues.put(KEY_AMOUNT, item.getAmount());
      initialValues.put(KEY_STORE, item.getStore());
      initialValues.put(KEY_CATEGORY, item.getCategory());
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
      if(isGDocsSyncingEnabled())
      {
         mDocsAdapter.deleteGroceryItem(item);
      }
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
   public ArrayList<GroceryItem> fetchAllGroceryItems()
   {
      Cursor noteCursor = mDb.rawQuery("SELECT * FROM " + GROCERY_TABLE, null);
      int id = noteCursor.getColumnIndexOrThrow(KEY_ID);
      int itemname = noteCursor.getColumnIndexOrThrow(KEY_ITEMNAME);
      int amount = noteCursor.getColumnIndexOrThrow(KEY_AMOUNT);
      int store = noteCursor.getColumnIndexOrThrow(KEY_STORE);
      int group = noteCursor.getColumnIndexOrThrow(KEY_CATEGORY);

      ArrayList<GroceryItem> groceryItems = new ArrayList<GroceryItem>();
      if (noteCursor.moveToFirst())
      {
         do
         {
            groceryItems.add(new GroceryItem(noteCursor.getLong(id), noteCursor.getString(itemname), noteCursor
                  .getString(amount), noteCursor.getString(store), noteCursor.getString(group)));
         } while (noteCursor.moveToNext());
      }
      noteCursor.close();
      return groceryItems;
   }

   public Cursor getGroceryCursor()
   {
      return mDb.rawQuery("SELECT * FROM " + GROCERY_TABLE, null);
   }

   public static String[] getFields()
   {
      String[] fields =
      { KEY_ITEMNAME, KEY_AMOUNT, KEY_STORE, KEY_CATEGORY };
      return fields;
   }
   
   private boolean isGDocsSyncingEnabled()
   {
      //TODO: Actually see if it is enabled
      return true;
   }
}