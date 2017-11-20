package com.example.shamsad.siminfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private TextView tsim;
    public Context context;
    public TelephonyManager telephonyManager;
    private String imeiSIM1;
    private String imeiSIM2;
    private boolean isSIM1Ready;
    private boolean isSIM2Ready;
    public static List<SubscriptionInfo>subscriptionInfos;
    public Boolean isRegistered;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.i("receiver", "Got message: " + message);
            showText(message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        bsim = findViewById(R.id.bSim);
        tsim = findViewById(R.id.tsim);

        IntentFilter mFilter = new IntentFilter("REFRESH");
        context.registerReceiver(mMessageReceiver, mFilter);
        isRegistered = true;

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

//                String carrierName = telephonyManager.getNetworkOperatorName();
//                Log.d("Sajid","carrier: "+ carrierName);
            }
        });


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
            for(int i=0; i<subscriptionInfos.size();i++)
            {
                lsuSubscriptionInfo = subscriptionInfos.get(i);
                Log.d("Sajid " ,"getNumber "+ lsuSubscriptionInfo.getNumber());
                Log.d("Sajid", "network name : "+ lsuSubscriptionInfo.
                        getCarrierName());
                Log.d("Sajid ", "getCountryIso "+ lsuSubscriptionInfo.getCountryIso());
            }

            return lsuSubscriptionInfo.getCarrierName().toString();
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
    protected void onPause() {
        super.onPause();
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
    }



    private void showText(String message) {
        tsim.setText(message);
    }
}
