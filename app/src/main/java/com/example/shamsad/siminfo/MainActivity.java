package com.example.shamsad.siminfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button bsim;
    private Button bCall1;
    private Button bCall2;
    private TextView tsim;
    public Context context;
    public TelephonyManager telephonyManager;
    private String imeiSIM1;
    private String imeiSIM2;
    private boolean isSIM1Ready;
    private boolean isSIM2Ready;
    public static List<SubscriptionInfo>subscriptionInfos;
    public Boolean isRegistered;

    public static Boolean flag = null;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d("Sajid", "USSD msg: "+message);
            showText(message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        SharedPreferences sharedPref= getSharedPreferences("mypref", 0);
        SharedPreferences.Editor editor= sharedPref.edit();
        editor.putString("flag", "true");
        editor.commit();

        bsim = findViewById(R.id.bSim);
        bCall1 = findViewById(R.id.bCall1);
        bCall2 = findViewById(R.id.bCall2);
        tsim = findViewById(R.id.tsim);

        IntentFilter mFilter = new IntentFilter("REFRESH");
        context.registerReceiver(mMessageReceiver, mFilter);
        isRegistered = true;
        Log.d("Sajid", "Registering receiver...");

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
                context.registerReceiver(mMessageReceiver, mFilter);
                isRegistered = true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        SharedPreferences sharedPref= getSharedPreferences("mypref", 0);
        SharedPreferences.Editor editor= sharedPref.edit();
        editor.putString("flag", "true");
        editor.commit();
    }

    @Override
    protected void onStop() {

        Log.d("Sajid", "Unregistering receiver...");
        try
        {
            if (isRegistered) {
                context.unregisterReceiver(mMessageReceiver);
                isRegistered = false;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        SharedPreferences sharedPref= getSharedPreferences("mypref", 0);
        SharedPreferences.Editor editor= sharedPref.edit();
        editor.putString("flag", "false");
        editor.commit();

        super.onStop();

//        stopService(new Intent(MainActivity.this, USSDService.class));
    }



    private void showText(String message) {
        tsim.append(message);
    }
}
