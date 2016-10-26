package comgothicvoid.httpsgithub.ffscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;

/**
 * Created by Administrator on 2016/10/21.
 */
public class SMSBroadcastReceiver extends BroadcastReceiver {
    private static MessageListener mMessageListener;
    @Override
    public void onReceive(Context context, Intent intent) {
        Object[] pdus = (Object[])intent.getExtras().get("pdus");   //接收数据
        for(Object p: pdus){
            byte[]pdu = (byte[])p;
            SmsMessage message = SmsMessage.createFromPdu(pdu); //根据获得的byte[]封装成SmsMessage
            String body = message.getMessageBody();             //发送内容
            String sender = message.getDisplayOriginatingAddress();    //短信发送方
            mMessageListener.OnReceived(sender,body);
        }
    }
    //回调接口
    public interface MessageListener {
        public void OnReceived(String sender, String body);
    }
    public void setOnReceivedMessageListener(MessageListener messageListener) {
        this.mMessageListener=messageListener;
    }
}