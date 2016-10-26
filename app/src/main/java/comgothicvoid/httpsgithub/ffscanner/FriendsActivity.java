package comgothicvoid.httpsgithub.ffscanner;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

/**
 * Created by Administrator on 2016/10/21.
 */
public class FriendsActivity extends AppCompatActivity {

    //数据库相关
    private static SQLiteDatabase db;
    private SimpleCursorAdapter simpleCursorAdapter;
    //信息列表
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.friends_list);

        //打开数据库
        db = DBConnection.open(this);

        listView = (ListView) findViewById(R.id.lvw_friends_list);
        setListView();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(FriendsActivity.this,FriendsDetailActivity.class);
                startActivityForResult(intent, 101);
            }
        });

        final CustomDialog.Builder builder = new CustomDialog.Builder(this);
        Button add = (Button)findViewById(R.id.btn_friends_list_add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.setPositiveButton(new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("name", builder.getname());
                        contentValues.put("num", builder.getnum());
                        db.insert("friends",null,contentValues);
                        setListView();
                    }
                });
                builder.setNegativeButton(new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
        });

        Button radar = (Button)findViewById(R.id.btn_friends_list_radar);
        radar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(100,intent);
                finish();
            }
        });

        Button foe = (Button)findViewById(R.id.btn_friends_list_enemies);
        foe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    protected void onStop(){
        simpleCursorAdapter.changeCursor(null);
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        DBConnection.close(db);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 102 && data != null){
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", data.getStringExtra("name"));
            contentValues.put("num", data.getStringExtra("num"));
            db.insert("friends",null,contentValues);
            setListView();
        }
    }

    private void setListView(){
        String[] from = new String[]{"name"};
        int[] to = new int[]{R.id.name_cell};
        Cursor cursor = db.query("friends",new String[]{"_id","name"},null,null,null,null,"name");
        simpleCursorAdapter = new SimpleCursorAdapter
                (this,R.layout.friends_list_item,cursor,from,to,0);
        listView.setAdapter(simpleCursorAdapter);
    }
}
