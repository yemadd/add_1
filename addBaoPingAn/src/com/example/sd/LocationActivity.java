package com.example.sd;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.mapapi.location.LocationManagerProxy;
//第5次修改
public class LocationActivity extends Activity implements LocationListener,AnimationLayout.Listener {
	private static final int ITME = Menu.FIRST; 
	private static final int ITME2 = Menu.FIRST + 1;  
	double mLat=21.158548;
	double mLon=113.355678;
	private Button send;
	private ProgressDialog dialog = null;
	private String location="";
	private Button btn,contacts;
	private LocationManagerProxy locationManager = null;
	private TextView myLocation;
	private EditText messageText;
	private String str="";
	public final static String TAG = "Demo";
	private static List<String> list ;
	private String[] newStr;
	private List<String> numberList ;
	private List<Integer> numberPosition;
	protected ListView mList;
	protected AnimationLayout mLayout;
	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			myLocation.setText((String) msg.obj);
			myLocation.setText(location);
			messageText.setText(location);
				myLocation.setText("您在:"+location);
				if (msg.what == 1001) {
					Toast.makeText(getApplicationContext(),
							location, Toast.LENGTH_LONG).show();
				}else if(msg.what == 3000){
					Toast.makeText(getApplicationContext(),"请检查网络是否连接！", Toast.LENGTH_SHORT).show();
				}			
			dialog.dismiss();
		}
	};
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//网络检测
		ConnectivityManager manager = (ConnectivityManager) 
				getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networkinfo = manager.getActiveNetworkInfo();
		if (networkinfo == null || !networkinfo.isAvailable()) {
			showAlertDialog(LocationActivity.this, "没有网络连接！",
					"您的手机没有连接上网络，请点击进入网络设置！(You don't have internet connection.)");
			

		}

		
		newStr = SearchContact();
		mList   = (ListView) findViewById(R.id.sidebar_list);
		send = (Button) findViewById(R.id.sidebar_button);
		send.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				// TODO Auto-generated method stub
				trueClickDialog(LocationActivity.this,"温馨提示","您确定将信息发送出去吗？");
			}
			
		});
		list = new ArrayList<String>();
		numberList = new ArrayList<String>();
		numberPosition = new ArrayList<Integer>();
		myLocation = (TextView) findViewById(R.id.myLocation);
		locationManager = LocationManagerProxy.getInstance(this);
		btn = (Button) this.findViewById(R.id.geobtn);	
		contacts = (Button) this.findViewById(R.id.contacts);	
		messageText = (EditText) findViewById(R.id.message);
		dialog=new ProgressDialog(this);
		mLayout = (AnimationLayout) findViewById(R.id.animation_layout);
		mLayout.setListener(this);
		mList   = (ListView) findViewById(R.id.sidebar_list);
		mList.setAdapter(
				new ArrayAdapter<String>(
						this, android.R.layout.simple_list_item_multiple_choice
						,SearchContact()));
		mList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mList.setOnItemClickListener(new OnItemClickListenerImp());

		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getLocation(mLat,mLon);
			}
		});
		contacts.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Uri uri = Uri.parse("smsto:SEND TO");         
				Intent it = new Intent(Intent.ACTION_SENDTO, uri);            
				it.putExtra("sms_body","我现在在"+ location);            
				startActivity(it);  
			}

		});
		//getLocation(mLat,mLon);
//		Location mLocation = getLocationNew(this);
//		getLocation(mLocation.getLongitude(),mLocation.getLatitude());

	}

	
//	public Location getLocationNew(Context context) {
//        LocationManager locMan = (LocationManager) context
//                .getSystemService(Context.LOCATION_SERVICE);
//        Location location = locMan
//                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
//        if (location == null) {
//            location = locMan
//                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//        }
//        return location;
//    }
//	
//	
//	
	
	//添加菜单栏
	public boolean onCreateOptionsMenu(Menu menu) { 
		menu.add(0, ITME, 0, "打电话").setIcon(R.drawable.ic_launcher); 
		menu.add(0, ITME2, 0,"发信息").setIcon(R.drawable.ic_launcher); 
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) { 

		switch (item.getItemId()) { 
		case ITME: 
			Uri uri_1 = Uri.parse("tel: CALL TO ");
			Intent it_1 = new Intent(Intent.ACTION_DIAL,uri_1);
			startActivity(it_1);
			break; 
		case ITME2: 
			Uri uri_2 = Uri.parse("smsto:SEND TO");         
			Intent it_2 = new Intent(Intent.ACTION_SENDTO, uri_2);            
			it_2.putExtra("sms_body", location);            
			startActivity(it_2);  
			break; 		
		} 
		return true; 
	} 
	
	//注册监听
	public boolean enableMyLocation() {
		boolean result = true;
		Criteria cri = new Criteria();
		cri.setAccuracy(Criteria.ACCURACY_COARSE);//表示所需的经度和纬度的精度。
		cri.setAltitudeRequired(false);//指示是否提供者必须提供高度的信息。
		cri.setBearingRequired(false);//指示是否提供者必须提供承载的信息。
		cri.setCostAllowed(false);//是否允许付费
		String bestProvider = locationManager.getBestProvider(cri, false);
		System.out.println(bestProvider);		
		/**
		 * provider - 注册监听的provider名称
		 * minTime - 位置变化的通知时间，单位为毫秒，实际时间有可能长于或短于设定值
		 * minDistance - 位置变化通知距离，单位为米
		 * listener - 监听listener
		 */
		locationManager.requestLocationUpdates(bestProvider, 2000, 10, this);
		return result;
	}

	//重新获得焦点
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		enableMyLocation();
	}
	//暂停
	@Override
	protected void onPause() {
		//移除给定的listener位置更新
		locationManager.removeUpdates(this);
		super.onPause();
	}

	//活动销毁时
	@Override
	protected void onDestroy() {
		if (locationManager != null) {
			//移除给定的listener位置更新，并且销毁locationManager
			locationManager.removeUpdates(this);
			locationManager.destory();
		}
		locationManager = null;
		super.onDestroy();
	}
	//位置变化时
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		try{
			if (location != null) {
				Double geoLat = location.getLatitude();
				Double geoLng = location.getLongitude();
				String str = ("定位成功！:(" + geoLng + "," + geoLat + ")");
				mLat= geoLat;
				mLon = geoLng;				
				Message msg = new Message();
				msg.obj = str;				
				if (handler != null) {
					handler.sendMessage(msg);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}
	//当供应商由用户启用调用
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}
	//当provider的状态发生改变时调用
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}
    //改用谷歌解析，不用高德的
	private void getLocation(final double mlat,final double mLon){
		Thread t = new Thread(new Runnable() {
			public void run(){
				String url = "http://maps.googleapis.com/maps/api/geocode/json?latlng="+mlat+","+mLon+"&sensor=true&language=zh-cn";
				//创建一个HttpClint请求；
				HttpClient httpClient = new DefaultHttpClient();
				try{
					//向指定的URL发送http请求
					HttpResponse response = httpClient.execute(new HttpGet(url));
					//取得服务器的响应
					HttpEntity entity = response.getEntity();
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent()));
					String line = "";
					while((line = bufferedReader.readLine()) != null){
						location = location + line; 
					}

					if(location.indexOf("formatted_address")!=-1){
						location = location.substring(location.indexOf("formatted_address")+22,
								location.indexOf("geometry"));
						location = location.replaceAll(" ", "");
						location = location.replaceAll("\"", "");
						location = location.replaceAll("\"", "");
						location = location.replaceAll(",", "");
						handler.sendMessage(Message.obtain(handler, 1));
					} else{
						location="";
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					handler.sendMessage(Message.obtain(handler,0));
				}
			}
		});

		//ProgressDialog 
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setIndeterminate(false);
		dialog.setCancelable(true);
		dialog.setMessage("追寻你的足迹!定位中...");
		dialog.show();
		t.start();
	}
    //手机返回键监听，按返回键弹出是否退出dialog
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:{
			AlertDialog.Builder builder = new AlertDialog.Builder(LocationActivity.this);
			builder.setMessage("您确定退出程序吗?");
			builder.setPositiveButton("确定",new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					LocationActivity.this.finish();
				}
			});
			builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.show();
		}
		return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	//AnomationLayout.java的布局
	public void onClickContentButton(View v) {
		mLayout.toggleSidebar();
	}

	public void onBackPressed() {
		if (mLayout.isOpening()){
			mLayout.closeSidebar();
		} else {
			finish();
		}
	}

	public void onSidebarOpened() {
		Log.d(TAG, "opened");
	}


	public void onSidebarClosed() {
		Log.d(TAG, "opened");
	}


	public boolean onContentTouchedWhenOpening() {
		Log.d(TAG, "going to close sidebar");
		mLayout.closeSidebar();
		return true;
	}
	//查找联系人
	public String[] SearchContact(){  
		Cursor cursor = null;
		String[] nameList = null;
		try{
			ContentResolver content = getContentResolver();
			cursor = content.query(Contacts.People.CONTENT_URI, null, null, null, null);
			if(cursor != null){
				cursor.moveToFirst();
				List<String> nameListS = new ArrayList<String>();
				for( int i = 0 , z = 0; i < cursor.getCount()&&z<cursor.getCount();i++)	{
					cursor.moveToPosition(i);
					String name = cursor.getString(cursor.getColumnIndexOrThrow(Contacts.People.NAME));
					String number = cursor.getString(cursor.getColumnIndexOrThrow(Contacts.People.NUMBER));					
					nameListS.add(name+":"+number);				
					z++;
					if(name.matches("爸.*")||name.matches("妈.*")||name.matches("妹.*")
							||name.matches("哥.*")||name.matches("ү.*")
						    ||name.matches("弟.*")||name.matches("姐.*")
						    ||name.matches("叔.*")||name.matches("阿.*")
						    ||name.matches("老.*")||name.matches("姨.*")
							||name.matches("舅.*")||name.matches("爷.*")
							||name.matches("奶.*")||name.matches("婶.*")
							||name.matches("大.*")||name.matches("小.*"))
					{

						nameListS.add(name+":"+number);				
						z++;			
						}

				}
				nameList = new String[nameListS.size()];

				for(int k = 0 ; k < nameListS.size() ; k++){
					nameList[k] = nameListS.get(k);

				}

			}

		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(cursor != null){
				cursor.close();
			}
		}
		return nameList ;
	}

	//发送信息
	private class OnClickListenerImp implements OnClickListener{

		public void onClick(View v) {
			try{
				if(numberList.equals("null")){
					Toast.makeText(LocationActivity.this, "没有选中发送对象", Toast.LENGTH_SHORT).show();
				}else{
					str = messageText.getText().toString();
					str = str.replaceAll("null","");
					System.out.println("表明发送成功");
					SendMessage("我现在在"+str);
				}

			}catch(Exception e){
				e.printStackTrace();
			};

		}
	}
	//发送按钮弹出对话框方法
	private void trueClickDialog(Context context, String title, String message){
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();		 		        
		alertDialog.setTitle(title);		 		       
		alertDialog.setMessage(message);		 		       
		alertDialog.setIcon(R.drawable.agt_action_success);	 		      
		alertDialog.setButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//new OnClickListenerImp();
				try{
					if(numberList.equals("null")){
						Toast.makeText(LocationActivity.this, "没有选中发送对象", Toast.LENGTH_SHORT).show();
					}else{
						str = messageText.getText().toString();
						str = str.replaceAll("null","");
						System.out.println("表明发送成功");
						SendMessage("我现在在"+str);
					}

				}catch(Exception e){
					e.printStackTrace();
				};
			}
		});	
		alertDialog.setButton2("取消", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});		 		       
		alertDialog.show();
	}
	//选择发送对象
	private class OnItemClickListenerImp implements OnItemClickListener{

		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if(!list.contains(newStr[position])){

				list.add(newStr[position]);
				numberPosition.add(position);
			}
			else{
				list.remove(newStr[position]);
				numberPosition.remove(numberPosition.contains(position));
			}
			Toast.makeText(getApplicationContext(), list.toString(), Toast.LENGTH_SHORT).show();
		}

	}
	//发送信息
	public void SendMessage(String content){
		if(list.size() == 0){
			Toast.makeText(getApplicationContext(), "添加联系人" , Toast.LENGTH_SHORT).show();

		}
		else{
			SmsManager smsManager=SmsManager.getDefault();
			PendingIntent intent=PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(), 0);
			for(int i = 0 ; i<list.size() ; i++){
				String[] numberS = list.get(i).split(":");
				smsManager.sendTextMessage(numberS[1], null, content, 
						intent, null);
				Toast.makeText(getApplicationContext(), numberS[1], Toast.LENGTH_SHORT).show();
			}
			Toast.makeText(getApplicationContext(), "发送成功", Toast.LENGTH_LONG).show(); 
		}

	}					
	//网络连接情况对话框
	public void showAlertDialog(Context context, String title, String message) {
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();		 		        
		alertDialog.setTitle(title);		 		       
		alertDialog.setMessage(message);		 		       
		alertDialog.setIcon(R.drawable.agt_action_fail1);	 		      
		alertDialog.setButton("设置网络", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));//进入无线网络配置界面
			}
		});		 		       
		alertDialog.show();
	}



}
