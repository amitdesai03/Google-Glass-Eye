package com.ebay.eye;

import java.io.ByteArrayOutputStream;

import pl.itraff.TestApi.ItraffApi.ItraffApi;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

/**
 * Camera preview
 */
public class CameraActivity extends Activity {
	private CameraPreview _cameraPreview;
	private Camera _camera;
	private RemoteViews _remoteViews;

	private Context _context = this;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		android.os.Debug.waitForDebugger();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		_remoteViews = new RemoteViews(_context.getPackageName(), R.layout.my_layout);
		_cameraPreview = new CameraPreview(this);
		setContentView(_cameraPreview);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		// Handle tap events.
		case KeyEvent.KEYCODE_CAMERA:
			android.util.Log.d("CameraActivity", "Camera button pressed.");
			_cameraPreview.getCamera().takePicture(null, null,
					new SavePicture());
			android.util.Log.d("CameraActivity", "Picture taken.");

			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
			android.util.Log.d("CameraActivity", "Tap.");
			_cameraPreview.getCamera().takePicture(null, null,
					new SavePicture());

			return true;
		default:
			return super.onKeyDown(keyCode, event);
		}
	}

	class SavePicture implements Camera.PictureCallback {

		public final Integer CLIENT_API_ID = 43023;
		public final String CLIENT_API_KEY = "8dcc2ca356";

		private static final String TAG = "TestApi";

		// handler that receives response from api
		@SuppressLint("HandlerLeak")
		private Handler itraffApiHandler = new Handler() {
			// callback from api
			@Override
			public void handleMessage(Message msg) {
				Log.d("MSG", "Yippie");
				// dismissWaitDialog();
				Bundle data = msg.getData();
				if (data != null) {
					Integer status = data.getInt(ItraffApi.STATUS, -1);
					String response = data.getString(ItraffApi.RESPONSE);
					
					Log.d("status", status + "");
					Log.d("respoonse", response);
					LiveCard photoCard = new LiveCard(_context,TAG);
					photoCard.setViews(_remoteViews);
					photoCard.setDirectRenderingEnabled(true);
					if (response.contains("1")) {
						Log.d("Result","Price for rubix cube on ebay is 3$");
					}else {
						Log.d("Result","Item not found on eBay");
					}
					photoCard.publish(PublishMode.SILENT);
				}
			}
		};

		@Override
		public void onPictureTaken(byte[] bytes, Camera camera) {
			android.util.Log.d("CameraActivity", "In onPictureTaken().");
			Bitmap image = BitmapFactory
					.decodeByteArray(bytes, 0, bytes.length);
			android.util.Log.d("Image:", image.toString());

			if (image != null) {
				android.util.Log.d("", "image != null");

				// chceck internet connection
				if (ItraffApi.isOnline(getApplicationContext())) {
					// showWaitDialog();
					SharedPreferences prefs = PreferenceManager
							.getDefaultSharedPreferences(getBaseContext());
					// send photo
					ItraffApi api = new ItraffApi(CLIENT_API_ID,
							CLIENT_API_KEY, TAG, true);
					Log.d("KEY", CLIENT_API_ID.toString());

					api.setMode(ItraffApi.MODE_SINGLE);

					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
					byte[] pictureData = stream.toByteArray();
					Log.d("about to send photo", "Yay");
					api.sendPhoto(pictureData, itraffApiHandler,
							prefs.getBoolean("allResults", true));
				}
			}

		}
	}
}
