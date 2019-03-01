package com.a85.wechatplugin;

import android.app.Activity;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

/**
 * @author hejiangwei
 * Created at 2019/2/25.
 * @Describe
 */
public class WXMain implements IXposedHookLoadPackage {


    public static final String PACKAGE_NAME = "com.tencent.mm";



    private XC_LoadPackage.LoadPackageParam mlpparam = null;
    private Context mwxContext;
    /**
     * 标志是从加好友页面进入的，防止查看已加好友信息的时候也会跳转验证页面
     */
    private boolean isFromAddFriend=false;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.contains("com.tencent.mm")) {

            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mlpparam = loadPackageParam;
                    final Context context = ((Context) param.args[0]);
                    Log.e("Application", "afterHookedMethod========" + context.getPackageName() + ":" + context.getPackageManager().getPackageInfo(PACKAGE_NAME, 0).versionName);

                    mwxContext = context;
                    hookMultiClound();
                }
            });
        }
    }

    private void hookMultiClound() {


        XposedHelpers.findAndHookMethod("com.tencent.mm.ui.LauncherUI", mlpparam.classLoader,
                "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        isFromAddFriend=false;

                    }
                });
        final Class FTSAddFriendUIClass;
        final Class SayHiWithSnsPermissionUIClass;
        final Class ContactInfoUIClass;
        try {
            FTSAddFriendUIClass = mlpparam.classLoader.loadClass("com.tencent.mm.plugin.fts.ui.FTSAddFriendUI");
            SayHiWithSnsPermissionUIClass = mlpparam.classLoader.loadClass("com.tencent.mm.plugin.profile.ui.SayHiWithSnsPermissionUI");
            ContactInfoUIClass = mlpparam.classLoader.loadClass("com.tencent.mm.plugin.profile.ui.ContactInfoUI");
            findAndHookMethod(FTSAddFriendUIClass,
                    "onCreate", Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
//                            oFTSAddFriendUI = param.thisObject;
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    isFromAddFriend=true;
                                    XposedHelpers.setObjectField(param.thisObject, "query", "手机号／微信号／QQ号");
                                    XposedHelpers.callMethod(param.thisObject, "Mf", "手机号／微信号／QQ号");

                                }
                            }, 1000);

                        }
                    });

            findAndHookMethod(ContactInfoUIClass,
                    "onCreate", Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isFromAddFriend) {
                                        return;
                                    }

                                    String stringExtra = ((Activity) param.thisObject).getIntent().getStringExtra("room_name");
                                    Intent intent = new Intent((Activity) param.thisObject, SayHiWithSnsPermissionUIClass);
                                    Object dUU = null;
                                    try {
                                        dUU = XposedHelpers.findField(ContactInfoUIClass, "dUU").get(param.thisObject);

                                        intent.putExtra("Contact_User", String.valueOf(XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.storage.ad", mlpparam.classLoader), "field_username").get(dUU)));
                                        intent.putExtra("Contact_RemarkName", String.valueOf(XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.storage.ad", mlpparam.classLoader), "field_conRemark").get(dUU)));
                                        intent.putExtra("Contact_Nick", String.valueOf(XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.storage.ad", mlpparam.classLoader), "field_nickname").get(dUU)));
                                        int i = XposedHelpers.findField(ContactInfoUIClass, "ghk").getInt(param.thisObject);
                                        if (i == 14 || i == 8) {
                                            intent.putExtra("Contact_RoomNickname", ((Activity) param.thisObject).getIntent().getStringExtra("Contact_RoomNickname"));
                                        }
                                        intent.putExtra("Contact_Scene", i);
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                    intent.putExtra("room_name", stringExtra);
                                    intent.putExtra("source_from_user_name", stringExtra);
                                    intent.putExtra("source_from_nick_name", ((Activity) param.thisObject).getIntent().getStringExtra("source_from_nick_name"));
                                    intent.putExtra("sayhi_with_sns_perm_send_verify", true);
                                    intent.putExtra("sayhi_with_sns_perm_add_remark", true);
                                    intent.putExtra("sayhi_with_sns_perm_set_label", false);
                                    //
                                    intent.putExtra("AntispamTicket", ((Activity) param.thisObject).getIntent().getStringExtra("AntispamTicket"));
                                    ((Activity) param.thisObject).startActivity(intent);
                                }
                            }, 1000);
                        }
                    });

            findAndHookMethod(ContactInfoUIClass, "a",
                    XposedHelpers.findClass("com.tencent.mm.ui.base.preference.f",mlpparam.classLoader),
                    XposedHelpers.findClass("com.tencent.mm.ui.base.preference.Preference",mlpparam.classLoader),

                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);



                        }
                    });
            findAndHookMethod("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader,
                    "onSceneEnd", int.class, int.class, String.class,
                    findClass("com.tencent.mm.ah.m", mlpparam.classLoader),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            log("onClick------" + param.args[0] + "=" +
                                            param.args[1] + "=" +
                                            param.args[2] + "="+
                                            XposedHelpers.callMethod(param.args[3] ,"getType")
                            );
                            throw new NullPointerException();
                        }
                    });

            findAndHookMethod("com.tencent.mm.ah.p", mlpparam.classLoader,
                    "b", int .class, int .class, String .class,
                    findClass("com.tencent.mm.ah.m", mlpparam.classLoader),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            log("m5437b------" + param.args[0] + "=" +
                                            param.args[1] + "=" +
                                            param.args[2] + "="
                            );
                        }
                    });

            findAndHookMethod("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader,
                    "a", String .class, LinkedList.class, boolean .class, String.class,

                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            log(
                                    "m72431a---"+param.args[0]+"--"+
                                            ((LinkedList)param.args[1])+"----"+
                                    param.args[2]+"----"+
                                    param.args[3]+"----"

                            );
                        }
                    });
            findAndHookMethod(SayHiWithSnsPermissionUIClass,
                    "onCreate", Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            log("SayHiWithSnsPermissionUI-bundle=" + logBundle(((Activity) param.thisObject).getIntent().getExtras()));

                        }
                    });


            findAndHookMethod("com.tencent.mm.plugin.profile.a", mlpparam.classLoader,
                    "EF", String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            log("onClick------1" + param.args[0]);
                            throw new NullPointerException();
                        }
                    });



            findAndHookMethod("com.tencent.mm.ui.base.preference.MMPreference$2", mlpparam.classLoader,
                    "onItemClick", AdapterView.class, View.class, int.class, long.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            Object preference=  ((AdapterView)param.args[0]).getAdapter().getItem((int)param.args[2]);

                            log("onItemClick------" + preference.getClass().getSimpleName() + "=="+
                                    XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.ui.base.preference.Preference",mlpparam.classLoader),"mKey").get(preference)
                            );
                        }
                    });
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void log(Object str) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        XposedBridge.log("com.a85.wechatplugin:[" + df.format(new Date()) + "]:  "
                + str.toString());
    }


    public static String logBundle(Bundle bundle) {
        StringBuffer stringBuffer = new StringBuffer();
        for (String key : bundle.keySet()) {
            stringBuffer.append("Key=" + key + ", content=" + bundle.get(key) + "\n");
        }
        return stringBuffer.toString();
    }

}
