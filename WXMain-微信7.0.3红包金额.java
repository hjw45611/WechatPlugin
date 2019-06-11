package com.a85.datavisualization;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.a85.datavisualization.tools.Tools;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * @author hejiangwei
 * Created at 2019/1/25.
 * @Describe 微信7.0.3 红包金额Hook
 */
public class WXMain implements IXposedHookLoadPackage {


    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) {

        if (loadPackageParam.packageName.contains("com.tencent.mm")) {

            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    final Context context = ((Context) param.args[0]);
                    Log.e("Application", "afterHookedMethod========" + context.getPackageName());

                    findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI", loadPackageParam.classLoader,
                            "onCreate", Bundle.class,
                            new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                                    super.beforeHookedMethod(param);
                                    Bundle bundle = ((Activity) param.thisObject).getIntent().getExtras();
                                    StringBuffer buffer = new StringBuffer("onCreate--");
                                    for (String s : bundle.keySet()) {
                                        buffer.append("-key=" + s + "=value=");
                                        buffer.append(bundle.get(s) + "\n");
                                    }
                                    log(buffer.toString());

                                }
                            });
                    final Class<?> modelClass = XposedHelpers.findClass("com.tencent.mm.plugin.luckymoney.model.j",
                            loadPackageParam.classLoader);

                    findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI", loadPackageParam.classLoader,
                            "a", modelClass,
                            new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                                    super.beforeHookedMethod(param);

                                    log("LuckyMoneyDetailUI-" + Tools.beanToString(param.args[0]));
                                    XposedHelpers.setLongField(param.args[0], "cGT", 100000000L);
                                }
                            });


                }
            });
        }
    }

    public void log(Object str) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        XposedBridge.log("[" + df.format(new Date()) + "]:  "
                + str.toString());
    }

}