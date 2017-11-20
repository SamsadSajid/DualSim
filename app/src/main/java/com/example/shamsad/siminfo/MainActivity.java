package com.example.shamsad.siminfo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.SEND_SMS;

public class MainActivity extends AppCompatActivity {
    private Button bsim;
    private Button bCall1;
    private Button bCall2;
    private Button bSms1;
    private Button bSms2;
    private TextView tsim;
    public Context context;
    public TelephonyManager telephonyManager;
    private String imeiSIM1;
    private String imeiSIM2;
    private boolean isSIM1Ready;
    private boolean isSIM2Ready;
    public static List<SubscriptionInfo>subscriptionInfos;
    public Boolean isRegistered;

    private static final int REQUEST_SMS = 0;

    public static Boolean flag = null;

    private BroadcastReceiver mUSSDReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d("Sajid", "USSD msg: "+message);
            showText(message);
        }
    };

    private BroadcastReceiver mSmsReceiver = new BroadcastReceiver() {
        private SharedPreferences preferences;

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
                Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
                SmsMessage[] msgs = null;
                String msg_from;
                if (bundle != null){
                    //---retrieve the SMS message received---
                    try{
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        msgs = new SmsMessage[pdus.length];
                        for(int i=0; i<msgs.length; i++){
                            msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                            msg_from = msgs[i].getOriginatingAddress();
                            String msgBody = msgs[i].getMessageBody();
                            Log.d("Sajid", msgBody);
                            tsim.append(msgBody+"\n");
                            String simPackage = getSimPackage(msgBody);
                            tsim.append(simPackage+"\n");
                        }
                    }catch(Exception e){
                        Log.d("Sajid", "Exception caught: "+e.getMessage());
                    }
                }
                context.unregisterReceiver(mSmsReceiver);
            }

        }

    };

    private String getSimPackage(String msgBody) {
        String[] packages = new String[]{"Nishchinto", "Bondhu", "Djuice"};
        String temp = msgBody.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        for(int i=0; i<packages.length;i++){
            String pack = packages[i].replaceAll("[^a-zA-Z0-9]", "")
                    .toLowerCase();
            if(temp.contains(pack))
            {
                return packages[i];
            }
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        changeFlag("true");

        bsim = findViewById(R.id.bSim);
        bCall1 = findViewById(R.id.bCall1);
        bCall2 = findViewById(R.id.bCall2);
        tsim = findViewById(R.id.tsim);
        bSms1 = findViewById(R.id.bSms1);
        bSms2 = findViewById(R.id.bSms2);

        IntentFilter mFilter = new IntentFilter("REFRESH");
        context.registerReceiver(mUSSDReceiver, mFilter);
        isRegistered = true;
        Log.d("Sajid", "Registering receiver...");

        // for Sms

        context.registerReceiver(mSmsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

//        startService(new Intent(this, USSDService.class));

//        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);


        bsim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String methodName = "getSimOperatorName";
                int slotId1 = 0;
                int slotId2 = 1;

                String siminfo1 = getSimInfo(context, methodName, slotId1);
                Log.d("Sajid","Sim 1: "+siminfo1);


                String siminfo2 = getSimInfo(context, methodName, slotId2);
                Log.d("Sajid","Sim 2: "+siminfo2);

                tsim.append(siminfo1+"\n"+siminfo2);

//                String carrierName = telephonyManager.getNetworkOperatorName();
//                Log.d("Sajid","carrier: "+ carrierName);
            }
        });

        bCall1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runUSSD(0);
            }
        });

        bCall2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runUSSD(1);
            }
        });

        bSms1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkPermSms(0);
            }
        });

        bSms2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermSms(1);
            }
        });

    }

    private void changeFlag(String flag) {
        SharedPreferences sharedPref= getSharedPreferences("mypref", 0);
        SharedPreferences.Editor editor= sharedPref.edit();
        editor.putString("flag", flag);
        editor.commit();
    }

    public void checkPermSms(int slotId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int hasSMSPermission = checkSelfPermission(SEND_SMS);
            if (hasSMSPermission != PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(SEND_SMS)) {
                    showMessageOKCancel("You need to allow access to Send SMS",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[] {SEND_SMS},
                                                REQUEST_SMS);
                                    }
                                }
                            });
                    return;
                }
                requestPermissions(new String[] {SEND_SMS},
                        REQUEST_SMS);
                return;
            }
            sendMySMS(slotId);
        }
        else{
            sendMySMS(slotId);
        }
    }

    public static boolean sendMultipartTextSMS(String name,Context ctx, int simID, String toNum,
                                               String centerNum, ArrayList<String> smsTextlist,
                                               ArrayList<PendingIntent> sentIntentList,
                                               ArrayList<PendingIntent> deliveryIntentList) {
        try {
            Method method = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
            method.setAccessible(true);
            Object param = method.invoke(null, name);

            method = Class.forName("com.android.internal.telephony.ISms$Stub").
                    getDeclaredMethod("asInterface", IBinder.class);
            method.setAccessible(true);
            Object stubObj = method.invoke(null, param);
            if (Build.VERSION.SDK_INT < 18) {
                method = stubObj.getClass().getMethod("sendMultipartText",
                        String.class, String.class, List.class, List.class, List.class);
                method.invoke(stubObj, toNum, centerNum, smsTextlist, sentIntentList,
                        deliveryIntentList);
            } else {
                method = stubObj.getClass().getMethod("sendMultipartText", String.class,
                        String.class, String.class, List.class, List.class, List.class);
                method.invoke(stubObj, ctx.getPackageName(), toNum, centerNum, smsTextlist,
                        sentIntentList, deliveryIntentList);
            }
            return true;
        } catch (ClassNotFoundException e) {
            Log.d("Sajid", "ClassNotFoundException: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            Log.d("Sajid", "NoSuchMethodException: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Log.d("Sajid", "InvocationTargetException: " + e.getMessage());
        } catch (IllegalAccessException e) {
            Log.d("Sajid", "IllegalAccessException: " + e.getMessage());
        } catch (Exception e) {
            Log.d("Sajid", "Exception:sendMultipartTextSMS " + e.getMessage());
        }
        return false;
    }

    public static boolean sendSMS(String name, Context ctx, int simID, String toNum, String centerNum, String smsText, PendingIntent sentIntent, PendingIntent deliveryIntent) {

        try{
            Method method = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
            method.setAccessible(true);
            Object param = method.invoke(null, name);

            method = Class.forName("com.android.internal.telephony.ISms$Stub").getDeclaredMethod("asInterface", IBinder.class);
            method.setAccessible(true);
            Object stubObj = method.invoke(null, param);
            if (Build.VERSION.SDK_INT < 18) {
                method = stubObj.getClass().getMethod("sendText", String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
                method.invoke(stubObj, toNum, centerNum, smsText, sentIntent, deliveryIntent);
            } else {
                method = stubObj.getClass().getMethod("sendText", String.class, String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
                method.invoke(stubObj, ctx.getPackageName(), toNum, centerNum, smsText, sentIntent, deliveryIntent);
            }

            return true;
        } catch (ClassNotFoundException e) {
            Log.d("Sajid", "ClassNotFoundException:" + e.getMessage());
        } catch (NoSuchMethodException e) {
            Log.d("Sajid", "NoSuchMethodException:" + e.getMessage());
        } catch (InvocationTargetException e) {
            Log.d("Sajid", "InvocationTargetException:" + e.getMessage());
        } catch (IllegalAccessException e) {
            Log.d("Sajid", "IllegalAccessException:" + e.getMessage());
        } catch (Exception e) {
            Log.d("Sajid", "Exception: sendSMS " + e.getMessage());
        }
        return false;
    }

    public void sendMySMS(int slotId) {
        changeFlag("false");
        String smsNumber="" ;
        String message="";
        String name="";

        String methodName = "getSimOperatorName";
        String siminfo = getSimInfo(context, methodName, slotId).toLowerCase();
        if(siminfo.contains("grameenphone")){
            smsNumber = "4444";
            message = "P";
        }
        else if(siminfo.contains("teletalk")){
            smsNumber = "154";
            message = "P";
        }
        else return;

        Log.d("Sajid", "Calculating sms cost.......");
        if (slotId == 0) {
            name = "isms";
        } else if (slotId == 1) {
            name = "isms2";
        }

        boolean result;
        ArrayList<String> messageList = SmsManager.getDefault().divideMessage(message);
        if (messageList.size() > 1) {
            result = sendMultipartTextSMS(name, this, slotId, smsNumber, null, messageList, null, null);
        } else {
            result = sendSMS(name, this, slotId, smsNumber, null, message, null, null);
        }

        if (!result) {
            if (slotId == 0) {
                name = "isms0";
            } else if (slotId == 1) {
                name = "isms1";
            }
            if (messageList.size() > 1) {
                result = sendMultipartTextSMS(name, this, slotId, smsNumber, null, messageList, null, null);
            } else {
                result = sendSMS(name, this, slotId, smsNumber, null, message, null, null);
            }
        }
        if (!result ) {
            Log.d("Sajid", "Sorry. Message switching on your device isn't supported by us yet.");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                final ArrayList<Integer> simCardList = new ArrayList<>();
                SubscriptionManager subscriptionManager;
                subscriptionManager = SubscriptionManager.from(context);
                final List<SubscriptionInfo> subscriptionInfoList;
                subscriptionInfoList = subscriptionManager
                        .getActiveSubscriptionInfoList();
                for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                    int subscriptionId = subscriptionInfo.getSubscriptionId();
                    simCardList.add(subscriptionId);
                }
                int smsToSendFrom = simCardList.get(slotId);
                SmsManager.getSmsManagerForSubscriptionId(smsToSendFrom)
                        .sendTextMessage(smsNumber, null, message, null, null);
            }
            else {
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage("phoneNo", null, "sms message", null, null);
                }catch (Exception e){
                    Log.d("Sajid", "Your device is not compatible with this app");
                }
            }
        }

        changeFlag("true");
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_SMS:
                if (grantResults.length > 0 &&  grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d("Sajid", "Permission Granted, Now you can access sms");
//                    sendMySMS();

                }else {
                    Log.d("Sajid", "Permission Denied, You cannot access and sms");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(SEND_SMS)) {
                            showMessageOKCancel("You need to allow access to both the permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(new String[]{SEND_SMS},
                                                        REQUEST_SMS);
                                            }
                                        }
                                    });
                            return;
                        }
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new android.support.v7.app.AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public void runUSSD(int slotId) {
        String methodName = "getSimOperatorName";
        String siminfo = getSimInfo(context, methodName, slotId).toLowerCase();
        if(siminfo.contains("grameenphone")){
            String encodedHash = Uri.encode("#");
            String ussd = "*566" + encodedHash;
            startActivityForResult(new Intent("android.intent.action.CALL",
                    Uri.parse("tel:" + ussd)), 1);
        }
        else if(siminfo.contains("banglalink")){
            String encodedHash = Uri.encode("#");
            String ussd = "*124" + encodedHash;
            startActivityForResult(new Intent("android.intent.action.CALL",
                    Uri.parse("tel:" + ussd)), 1);
        }
        else if(siminfo.contains("teletalk")){
            String encodedHash = Uri.encode("#");
            String ussd = "*152" + encodedHash;
            startActivityForResult(new Intent("android.intent.action.CALL",
                    Uri.parse("tel:" + ussd)), 1);
        }
        else if(siminfo.contains("robi")){
            String encodedHash = Uri.encode("#");
            String ussd = "*222" + encodedHash;
            startActivityForResult(new Intent("android.intent.action.CALL",
                    Uri.parse("tel:" + ussd)), 1);
        }
    }

    private static String getSimInfo(Context context, String methodName, int slotId) {

        if (android.os.Build.VERSION.SDK_INT < 22){
            // Do something for below lollipop
            TelephonyManager telephony = (TelephonyManager) context.
                    getSystemService(Context.TELEPHONY_SERVICE);
            Class<?> telephonyClass;
            String reflectionMethod = null;
            String output = null;
            try {
                telephonyClass = Class.forName(telephony.getClass().getName());
                for (Method method : telephonyClass.getMethods()) {
                    String name = method.getName();
                    if (name.contains(methodName)) {
                        Class<?>[] params = method.getParameterTypes();
                        if (params.length == 1 && params[0].getName().equals("int")) {
                            reflectionMethod = name;
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (reflectionMethod != null) {
                try {
                    output = getOpByReflection(telephony, reflectionMethod, slotId, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return output;

        }
        else{

            subscriptionInfos = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
            SubscriptionInfo lsuSubscriptionInfo = null;
            if(slotId<subscriptionInfos.size()) {
                lsuSubscriptionInfo = subscriptionInfos.get(slotId);
                Log.d("Sajid ", "getNumber " + lsuSubscriptionInfo.getNumber());
                Log.d("Sajid", "network name : " + lsuSubscriptionInfo.
                        getCarrierName());
                Log.d("Sajid ", "getCountryIso " + lsuSubscriptionInfo.getCountryIso());
                return lsuSubscriptionInfo.getCarrierName().toString();
            }
            return null;
        }
    }

    private static String getOpByReflection(TelephonyManager telephony,
                                            String predictedMethodName,
                                            int slotID, boolean isPrivate) {
        Log.i("Reflection", "Method: " + predictedMethodName+" "+slotID);
        String result = null;

        try {

            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());

            Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimID;
            if (slotID != -1) {
                if (isPrivate) {
                    getSimID = telephonyClass.getDeclaredMethod(predictedMethodName, parameter);
                } else {
                    getSimID = telephonyClass.getMethod(predictedMethodName, parameter);
                }
            } else {
                if (isPrivate) {
                    getSimID = telephonyClass.getDeclaredMethod(predictedMethodName);
                } else {
                    getSimID = telephonyClass.getMethod(predictedMethodName);
                }
            }

            Object ob_phone;
            Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            if (getSimID != null) {
                if (slotID != -1) {
                    ob_phone = getSimID.invoke(telephony, obParameter);
                } else {
                    ob_phone = getSimID.invoke(telephony);
                }

                if (ob_phone != null) {
                    result = ob_phone.toString();
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            return null;
        }
        Log.i("Reflection", "Result: " + result);
        return result;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Sajid", "Registering receiver...");
        try
        {
            if (!isRegistered) {
                IntentFilter mFilter = new IntentFilter("REFRESH");
                context.registerReceiver(mUSSDReceiver, mFilter);
                isRegistered = true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        changeFlag("true");
    }

    @Override
    protected void onStop() {

        Log.d("Sajid", "Unregistering receiver...");
        try
        {
            if (isRegistered) {
                context.unregisterReceiver(mUSSDReceiver);
                isRegistered = false;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        changeFlag("false");

        super.onStop();

//        stopService(new Intent(MainActivity.this, USSDService.class));
    }



    private void showText(String message) {
        tsim.append(message);
    }
}
