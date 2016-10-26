package comgothicvoid.httpsgithub.ffscanner;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Administrator on 2016/10/25.
 */
public class DBConnection {
    public static final String DATABASE_NAME = "FF.db"; //数据库名
    public static final int DATABASE_VERSION = 1;       //数据库版本号

    //打开数据库
    public static SQLiteDatabase open(Context context){
        return new DBOpenHelper(context).getWritableDatabase();
    }
    //关闭数据库
    public static void close(SQLiteDatabase db){
        if(db != null) db.close();
    }

    private static class DBOpenHelper extends SQLiteOpenHelper{
        public DBOpenHelper(Context context){
            super(context,DATABASE_NAME,null,DATABASE_VERSION);
        }
        //建立数据库
        @Override
        public void onCreate(SQLiteDatabase db){        // TODO Auto-generated method stub
            db.execSQL("create table friends(_id integer primary key autoincrement," +
                    "name text,num text)");
            db.execSQL("create table foes(_id integer primary key autoincrement," +
                    "name text,num text)");
        }
        //建立新版本的数据库
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            db.execSQL("DROP TABLE IF EXISTS friends");
            db.execSQL("DROP TABLE IF EXISTS foes");
            onCreate(db);
        }
    }
}