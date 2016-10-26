package comgothicvoid.httpsgithub.ffscanner;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ZoomControls;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //地图相关
    TextureMapView mMapView = null;
    BaiduMap mBaiduMap;
    //定位相关
    boolean isFirstLoc = true; // 是否首次定位
    LocationClient mLocationClient;
    BDLocation mlocation;
    BDLocationListener myListener = new MyLocationListener();
    BitmapDescriptor mCurrentMarker;
    LatLng ll;
    int mXDirection = 0;
    //方向传感器相关
    MyOrientationListener myOrientationListener;
    //计时器相关
    Handler handler;
    Runnable runnable;
    //敌友相关
    List<FriendFoe> friends = new ArrayList<FriendFoe>();
    List<FriendFoe> foes = new ArrayList<FriendFoe>();
    //数据库相关
    private static SQLiteDatabase db;
    //动画相关
    private Animation myAnimation;
    //短信相关
    SMSBroadcastReceiver smsBR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());

        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.registerLocationListener( myListener );    //注册监听函数

        setContentView(R.layout.main);

        //打开数据库
        db = DBConnection.open(this);

        //获取地图控件引用
        mMapView = (TextureMapView) findViewById(R.id.bmapView);
        mMapView.showZoomControls(false);// 不显示默认的缩放控件
        mMapView.showScaleControl(false);// 不显示默认比例尺控件
        // 隐藏logo
        View child = mMapView.getChildAt(1);
        if (child != null && (child instanceof ImageView || child instanceof ZoomControls)){
            child.setVisibility(View.INVISIBLE);
        }
        mBaiduMap = mMapView.getMap();

        //定位设置
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); //打开GPRS
        option.setCoorType("bd09ll"); //返回的定位结果是百度经纬度,默认值gcj02
        option.disableCache(true); //禁止启用缓存定位
        mLocationClient.setLocOption(option);  //设置定位参数
        //设置定位图层的配置（定位模式，是否允许方向信息，用户自定义定位图标）
        mCurrentMarker = BitmapDescriptorFactory.fromResource(R.drawable.bat);
        MyLocationConfiguration config = new MyLocationConfiguration
                (MyLocationConfiguration.LocationMode.NORMAL, true, mCurrentMarker);
        mBaiduMap.setMyLocationConfigeration(config);

        //方向传感器监听
        myOrientationListener = new MyOrientationListener(getApplicationContext());
        myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mXDirection = (int) x;
            }
        });

        //开始定位及方向监听
        myOrientationListener.start();
        mLocationClient.start();

        //获取敌友列表
        setFriends(friends);
        setFoes(foes);

        //定时更新方向
        handler = new Handler();
        runnable = new Runnable(){
            @Override
            public void run() {
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(0)
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(mXDirection)
                        .latitude(mlocation.getLatitude())
                        .longitude(mlocation.getLongitude()).build();
                mBaiduMap.setMyLocationData(locData);
                // 设置定位图层的配置（定位模式，是否允许方向信息，用户自定义定位图标）
                mCurrentMarker = BitmapDescriptorFactory.fromResource(R.drawable.bat);
                MyLocationConfiguration config = new MyLocationConfiguration
                        (MyLocationConfiguration.LocationMode.NORMAL, true, mCurrentMarker);
                mBaiduMap.setMyLocationConfigeration(config);

                if (isFirstLoc) {
                    isFirstLoc = false;
                    ll = new LatLng(mlocation.getLatitude(), mlocation.getLongitude());
                    MapStatus.Builder builder = new MapStatus.Builder();
                    builder.target(ll).zoom(18.0f);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                }
                handler.postDelayed(this, 500); // 50是延时时长
            }
        };

        final ImageView imgPic = (ImageView)findViewById(R.id.imageview_sweep);
        myAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_indefinitely);

        Button bref = (Button)findViewById(R.id.btn_refresh);
        bref.setOnClickListener (new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mBaiduMap.clear();
                imgPic.startAnimation(myAnimation);
                int i;
                for(i = 0; i != friends.size(); i++){
                    String phone = friends.get(i).getNum();
                    String context = "where are you?";
                    SmsManager manager = SmsManager.getDefault();
                    //因为一条短信有字数限制，因此要将长短信拆分
                    ArrayList<String> list = manager.divideMessage(context);
                    for(String text:list){
                        manager.sendTextMessage(phone, null, text, null, null);
                    }
                }
                for(i = 0; i != foes.size(); i++){
                    String phone = foes.get(i).getNum();
                    String context = "where are you?";
                    SmsManager manager = SmsManager.getDefault();
                    //因为一条短信有字数限制，因此要将长短信拆分
                    ArrayList<String> list = manager.divideMessage(context);
                    for(String text:list){
                        manager.sendTextMessage(phone, null, text, null, null);
                    }
                }
                smsBR = new SMSBroadcastReceiver();
                smsBR.setOnReceivedMessageListener(new SMSBroadcastReceiver.MessageListener() {
                    @Override
                    public void OnReceived(String sender, String body) {
                        if(body.equals("where are you?") && mlocation != null){
                            double lat = mlocation.getLatitude();
                            double lng = mlocation.getLongitude();
                            String loc = "isl" + Double.toString(lat) + "/" + Double.toString(lng);
                            SmsManager manager = SmsManager.getDefault();
                            ArrayList<String> list = manager.divideMessage(loc);
                            for(String text:list){
                                manager.sendTextMessage(sender, null, text, null, null);
                            }
                        } else if(body.substring(0,3).equals("isl")){
                            int j;
                            String n;
                            for(j = 0; j != friends.size(); j++){
                                n = "+86" + friends.get(j).getNum();
                                if (n.equals(sender)) DrawLine(getLoc(body),true);
                            }
                            for(j = 0; j != foes.size(); j++){
                                n = "+86" + foes.get(j).getNum();
                                if (n.equals(sender)) DrawLine(getLoc(body),false);
                            }
                            smsBR.abortBroadcast();   //中断广播
                        }
                    }
                });
            }
        });

        Button bloc = (Button)findViewById(R.id.btn_locate);
        bloc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 开启定位图层
                if(!mBaiduMap.isMyLocationEnabled()) mBaiduMap.setMyLocationEnabled(true);
                mLocationClient.requestLocation();
                ll = new LatLng(mlocation.getLatitude(), mlocation.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        });

        Button fnd = (Button)findViewById(R.id.btn_friends);
        fnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,FriendsActivity.class);
                startActivityForResult(intent, 100);
            }
        });

        /*Button foe = (Button)findViewById(R.id.btn_enemies);
        foe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,FoesActivity.class);
                startActivityForResult(intent, 200);
            }
        });*/
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100){
            setFriends(friends);
        }
        if(requestCode == 200){
            setFoes(foes);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关闭数据库
        DBConnection.close(db);
        // 关闭定时器处理
        handler.removeCallbacks(runnable);
        // 退出时销毁定位
        mLocationClient.stop();
        // 关闭方向传感器
        myOrientationListener.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) return;
            mlocation = location;
            if(isFirstLoc) handler.postDelayed(runnable, 500); // 打开定时器，执行操作
        }
    }

    //在自己与敌/友之间连线
    public void  DrawLine(LatLng fl, boolean b){
        LatLng p1 = ll; //自己
        LatLng p2 = fl; //敌或友

        //Double dis = DistanceUtil.getDistance(p1, p2);
        //构建敌或友图标
        BitmapDescriptor bitmap;
        OverlayOptions option;
        //构建MarkerOption，用于在地图上添加敌或友图标
        if(b) {
            bitmap = BitmapDescriptorFactory.fromResource(R.drawable.green);
            option = new MarkerOptions().position(p2).icon(bitmap);
        }
        else {
            bitmap = BitmapDescriptorFactory.fromResource(R.drawable.red);
            option = new MarkerOptions().position(p2).icon(bitmap);
        }
        //在地图上添加敌或友图标，并显示
        mBaiduMap.addOverlay(option);

        //在两点间画线
        List<LatLng> points = new ArrayList<LatLng>();
        points.add(p1);
        points.add(p2);
        OverlayOptions ooPolyline;
        if(b) ooPolyline = new PolylineOptions().width(7).color(0xAA00FF00).points(points);
        else ooPolyline = new PolylineOptions().width(7).color(0xAAFF0000).points(points);
        mBaiduMap.addOverlay(ooPolyline);
    }

    //获取朋友列表
    private void setFriends(List<FriendFoe> l){
        if(!l.isEmpty()) l.clear();
        Cursor cursor = db.query("friends",null,null,null,null,null,"name");
        while (cursor.moveToNext()){
            FriendFoe friend = new FriendFoe();
            friend.setName(cursor.getString(cursor.getColumnIndex("name")));
            friend.setNum(cursor.getString(cursor.getColumnIndex("num")));
            l.add(friend);
        }
        cursor.close();
    }

    //获取敌人列表
    private void setFoes(List<FriendFoe> l){
        if(!l.isEmpty()) l.clear();
        Cursor cursor = db.query("foes",null,null,null,null,null,"name");
        while (cursor.moveToNext()){
            FriendFoe foe = new FriendFoe();
            foe.setName(cursor.getString(cursor.getColumnIndex("name")));
            foe.setNum(cursor.getString(cursor.getColumnIndex("num")));
            l.add(foe);
        }
        cursor.close();
    }

    //解读坐标
    public LatLng getLoc(String ll){
        int idx = ll.indexOf("/");
        double la = Double.parseDouble(ll.substring(3,idx));
        double lg = Double.parseDouble(ll.substring(idx+1,ll.length()));
        return new LatLng(la,lg);
    }
}