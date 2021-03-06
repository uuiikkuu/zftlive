package com.zftlive.android.library.tools;

import java.lang.reflect.Type;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.inputmethodservice.ExtractEditText;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;

import com.google.gson.Gson;
import com.zftlive.android.library.data.DTO;

/**
 * 数据工具类
 * 
 * @author 曾繁添
 * @version 1.0
 * @link http://www.cnblogs.com/fly100/
 * @email mengzhongyouni@gmail.com
 */
public class ToolData {
	
	public static final String TAG = "ToolData";
	/**
	 * 数据分页条数
	 */
	public static Integer pageSize = 10;
	
	static {
		try {
			String value = ToolProperties.readAssetsProp("config.properties", "pageSize");
			if(ToolString.isNoBlankAndNoNull(value)){
				pageSize = Integer.valueOf(value);
			}
		} catch (Exception e) {
			Log.w(TAG, "读取配置文件assets目录config.properties文件pageSize失败，原因："+e.getMessage());
		}
	}
	
	/**
	 * 获取表单控件数据
	 * 
	 * @param root 当前表单容器
	 * @param data 当前表单数据
	 * @return 表单数据（CheckBox多选选项以##拼接）
	 */
	public static DTO<String,Object> gainForm(ViewGroup root,DTO<String,Object> data) {
		if (root.getChildCount() > 0) {
			for (int i = 0; i < root.getChildCount(); i++) {
				View view = root.getChildAt(i);
				// 容器级别控件需要进行递归
				if (view instanceof LinearLayout) {
					gainForm((LinearLayout) view,data);
				} else if (view instanceof RelativeLayout) {
					gainForm((RelativeLayout) view,data);
				} else if (view instanceof FrameLayout) {
					gainForm((FrameLayout) view,data);
				} else if (view instanceof AbsoluteLayout) {
					gainForm((AbsoluteLayout) view,data);
				} else if (view instanceof android.widget.RadioGroup) {
					gainForm((android.widget.RadioGroup) view,data);
				} else if (view instanceof com.zftlive.android.library.widget.RadioGroup) {
					gainForm((com.zftlive.android.library.widget.RadioGroup) view,data);
				} else if (view instanceof TableLayout) {
					gainForm((TableLayout) view,data);
				}

				// 非容器级别控件不用递归
				/**
				 * EditText.class
				 */
				else if (view instanceof EditText) {
					data.put((String) view.getTag(), ((EditText) view).getText().toString());
				} else if (view instanceof AutoCompleteTextView) {
					data.put((String) view.getTag(),((AutoCompleteTextView) view).getText().toString());
				} else if (view instanceof MultiAutoCompleteTextView) {
					data.put((String) view.getTag(),((MultiAutoCompleteTextView) view).getText()
									.toString());
				} else if (view instanceof ExtractEditText) {
					data.put((String) view.getTag(), ((ExtractEditText) view).getText().toString());
				}

				/**
				 * RadioButton.class
				 */
				else if (view.getClass().getName().equals(android.widget.RadioButton.class.getName())) {
					if (((android.widget.RadioButton) view).isChecked()) {
						data.put((String) view.getTag(),((android.widget.RadioButton) view).getText().toString());
					}
				}else if (view.getClass().getName().equals(com.zftlive.android.library.widget.RadioButton.class.getName())) {
					com.zftlive.android.library.widget.RadioButton mView = (com.zftlive.android.library.widget.RadioButton)view;
					if (mView.isChecked()) {
						data.put((String) mView.getKey(),mView.getValue());
					}
				} 

				/**
				 * CheckBox.class(需要拼装选中复选框)
				 */
				else if (view.getClass().getName().equals(android.widget.CheckBox.class.getName())) {
					if (((android.widget.CheckBox) view).isChecked()) {
						if (data.containsKey((String) view.getTag())) {
							Object value = data.get((String) view.getTag());
							value = value+ "##"+ ((android.widget.CheckBox) view).getText().toString();
							data.put((String) view.getTag(), value);
						} else {
							data.put((String) view.getTag(),((android.widget.CheckBox) view).getText().toString());
						}
					}
					
				}else if (view.getClass().getName().equals(com.zftlive.android.library.widget.CheckBox.class.getName())) {
					
					com.zftlive.android.library.widget.CheckBox mView = (com.zftlive.android.library.widget.CheckBox)view;
					if (mView.isChecked()) {
						if (data.containsKey(mView.getKey())) {
							Object value = data.get(mView.getKey());
							value = value+ "##"+ mView.getValue();
							data.put(mView.getKey(), value);
						} else {
							data.put(mView.getKey(),mView.getValue());
						}
					}
				}

				/**
				 * Spinner.class
				 */
				else if (view.getClass().getName().equals(android.widget.Spinner.class.getName())) {
					data.put((String) view.getTag(),((android.widget.Spinner) view).getSelectedItem().toString());
				}else if (view.getClass().getName().equals(com.zftlive.android.library.widget.SingleSpinner.class.getName())) {
					com.zftlive.android.library.widget.SingleSpinner mView = (com.zftlive.android.library.widget.SingleSpinner)view;
					data.put((String) mView.getKey(),mView.getSelectedValue());
				}
			}
		}

		return data;
	}
	
	/**
	 * 读取Assets目录的json文件,并转成指定的Bean
	 * @param mContext 上下文
	 * @param jsonFileName 不带扩展名的文件名
	 * @param clazz 需要转成对应的Bean
	 * @return
	 */
	public static <T> void gainAssetsData(final Context mContext,final String jsonFileName,final T clazz,final IDataCallBackHandler handler ){
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				if(null == handler) return;
				
				String strJsonData = ToolFile.readAssetsValue(mContext, jsonFileName);
				// JSON转成Bean
				T result = null;
				try {
					result = (T) new Gson().fromJson(strJsonData, (Type) clazz);
					handler.onSuccess(result);
				} catch (Exception e) {
					Log.e(TAG, "JSONObject转Bean失败,原因：" + e.getMessage());
					handler.onFailure(e.getMessage());
				}
			}
		}).start();
	}
	
	/**
	 * 读取Assets目录的json文件
	 * @param mContext 上下文
	 * @param jsonFileName 不带扩展名的文件名称
	 * @return
	 */
	public static JSONObject gainAssetsData(Context mContext,String jsonFileName){
		String strJsonData = ToolFile.readAssetsValue(mContext, jsonFileName);
		JSONObject result = null;
		try {
			result = new JSONObject(strJsonData);
		} catch (JSONException e) {
			Log.e(TAG, "构建JSONObject失败，原因："+e.getMessage());
			result = new JSONObject();
		}
		return result;
	}
	
	/**
	 * 读取AndroidManifest.xml配置的meta-data数据
	 * @param mContext 上下文
	 * @param target Activity/BroadcastReceiver/Service/Application
	 * @param key 配置的name
	 * @return
	 */
	public static String gainMetaData(Context mContext, Class target, String key) {
		String result = "";
		try {
			Log.d(TAG, target.getSuperclass().getName());
			
			int flags = PackageManager.GET_META_DATA;
			Object obj = target.newInstance();
			if (obj instanceof Activity) {
				ActivityInfo info2 = mContext.getPackageManager().getActivityInfo(((Activity) mContext).getComponentName(), flags);
				result = info2.metaData.getString(key);
			} else if (obj instanceof Application) {
				ApplicationInfo info1 = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), flags);
				result = info1.metaData.getString(key);
			} else if (obj instanceof Service) {
				ComponentName cn1 = new ComponentName(mContext, target);
				ServiceInfo info3 = mContext.getPackageManager().getServiceInfo(cn1, flags);
				result = info3.metaData.getString(key);
			} else if (obj instanceof BroadcastReceiver) {
				ComponentName cn2 = new ComponentName(mContext, target);
				ActivityInfo info4 = mContext.getPackageManager().getReceiverInfo(cn2, flags);
				result = info4.metaData.getString(key);
			}
		} catch (Exception e) {
			Log.e(TAG, "读取meta元数据失败，原因：" + e.getMessage());
		}
		return result;
	}
	
	public interface IDataCallBackHandler<T>{
		
		public void onSuccess(T result);
		
		public void onFailure(String errorMsg);
	}
}
