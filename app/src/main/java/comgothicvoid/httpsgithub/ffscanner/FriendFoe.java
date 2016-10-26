package comgothicvoid.httpsgithub.ffscanner;

import com.baidu.mapapi.model.LatLng;

/**
 * Created by Administrator on 2016/10/20.
 */
public class FriendFoe {
    private String name;
    private String num;
    //private LatLng loc = null;

    public void setName(String n){
        name = n;
    }
    public void setNum(String nm){
        num = nm;
    }
    /*public void setLoc(String ll){
        int idx = ll.indexOf("/");
        double la = Double.parseDouble(ll.substring(3,idx));
        double lg = Double.parseDouble(ll.substring(idx+1,ll.length()));
        loc = new LatLng(la,lg);
    }*/

    public String getName(){
        return name;
    }
    public String getNum(){
        return num;
    }
    /*public LatLng getLoc(){
        return loc;
    }*/

}
