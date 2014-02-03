package app.akexorcist.bluetoothspp;

import java.util.ArrayList;
import java.util.Set;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("NewApi")
public class BluetoothSPP {
	// Listener for Bluetooth Status & Connection
	private BluetoothStatusListener mBluetoothStatusListener = null;
	private BluetoothConnectionListener mBluetoothConnectionListener = null;
	private AutoConnectionListener mAutoConnectionListener = null;
	
	// This for Debugging
    private static final String TAG = "BluetoothSPP";
    
    // Context from activity which call this class
	private Context mContext;
	
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Member object for the chat services
    private BluetoothService mChatService = null;
    
    // Name and Address of the connected device
    private String mDeviceName = null;
    private String mDeviceAddress = null;

    private boolean isAutoConnecting = false;
    private boolean isAutoConnectionEnabled = false;
    private boolean isConnected = false;
	private boolean isConnecting = false;
	private boolean isServiceRunning = false;
    
	private String keyword = "";
    private boolean isAndroid = BluetoothState.DEVICE_ANDROID;
	
    BluetoothConnectionListener bcl;
    int c = 0;
	
	public BluetoothSPP(Context context) {
		mContext = context;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}
	
    public interface BluetoothStatusListener {
	    public void onDataReceived(byte[] data, String message);
	    public void onServiceStateChanged(int state);
	}
    
    public interface BluetoothConnectionListener {
	    public void onDeviceConnected(String name, String address);
	    public void onDeviceDisconnected();
	    public void onDeviceConnectionFailed();
	}
    
    public interface AutoConnectionListener {
	    public void onAutoConnectionStarted();
	    public void onNewConnection(String name, String address);
	}
	
	public boolean isBluetoothAvailable() {
        try {
        	if (mBluetoothAdapter == null || mBluetoothAdapter.getAddress().equals(null))
	            return false;
        } catch (NullPointerException e) {
        	 return false;
        }
        return true;
	}
	
	public boolean isBluetoothEnable() {
		return mBluetoothAdapter.isEnabled();
	}
	
	public boolean isServiceAvailable() {
		return (mChatService == null);
	}
	
	public boolean isAutoConnecting() {
		return isAutoConnecting;
	}
	
	public void setupService() {
        mChatService = new BluetoothService(mContext, mHandler);
	}
	
	public int getServiceState() {
		return mChatService.getState();
	}
	
	public void startService(boolean isAndroid) {
		if (mChatService != null) {
            if (mChatService.getState() == BluetoothState.STATE_NONE) {
            	isServiceRunning = true;
            	mChatService.start(isAndroid);
            	BluetoothSPP.this.isAndroid = isAndroid;
            }
        }
	}
	
	public void stopService() {
        if (mChatService != null) {
        	isServiceRunning = false;
        	mChatService.stop();
        }
	}
    
    public void restartService() {
    	stopService();
    	startService(BluetoothSPP.this.isAndroid);
    }
    
    public void setDeviceTarget(boolean isAndroid) {
    	stopService();
    	startService(BluetoothSPP.this.isAndroid);
    }
	
	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothState.MESSAGE_WRITE:
                break;
            case BluetoothState.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                String readMessage = new String(readBuf);
                if(readBuf != null && readBuf.length > 0) {
                	if(mBluetoothStatusListener != null)
                		mBluetoothStatusListener.onDataReceived(readBuf, readMessage);
                }
                break;
            case BluetoothState.MESSAGE_DEVICE_NAME:
                mDeviceName = msg.getData().getString(BluetoothState.DEVICE_NAME);
                mDeviceAddress = msg.getData().getString(BluetoothState.DEVICE_ADDRESS);
            	if(mBluetoothConnectionListener != null)
            		mBluetoothConnectionListener.onDeviceConnected(mDeviceName, mDeviceAddress);
                isConnected = true;
                break;
            case BluetoothState.MESSAGE_TOAST:
                Toast.makeText(mContext, msg.getData().getString(BluetoothState.TOAST)
                		, Toast.LENGTH_SHORT).show();
                break;
            case BluetoothState.MESSAGE_STATE_CHANGE:
            	if(mBluetoothStatusListener != null)
            		mBluetoothStatusListener.onServiceStateChanged(msg.arg1);
                if(isConnected && msg.arg1 != BluetoothState.STATE_CONNECTED) {
                	if(mBluetoothConnectionListener != null)
                		mBluetoothConnectionListener.onDeviceDisconnected();
                	if(isAutoConnectionEnabled) {
                		isAutoConnectionEnabled = false;
                		autoConnect(keyword);
                	}
                    isConnected = false;
                    mDeviceName = null;
                    mDeviceAddress = null;
                }
                
                if(!isConnecting && msg.arg1 == BluetoothState.STATE_CONNECTING) {
                	isConnecting = true;
                } else if(isConnecting) {
                	if(msg.arg1 != BluetoothState.STATE_CONNECTED) {
                    	if(mBluetoothConnectionListener != null)
                    		mBluetoothConnectionListener.onDeviceConnectionFailed();
                	}
                	isConnecting = false;
                }
                break;
            }
        }
    };
    
    public boolean isAutoConnectionEnabled() {
    	return isAutoConnectionEnabled;
    }
    
    public void stopAutoConnect() {
    	isAutoConnectionEnabled = false;
    }
    
    public void connect(Intent data) {
        String address = data.getExtras().getString(BluetoothState.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mChatService.connect(device);
    }
    
    public void connect(String address) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mChatService.connect(device);
    }
    
    public void setBluetoothStatusListener (BluetoothStatusListener listener) {
    	mBluetoothStatusListener = listener;
    }
    
    public void setBluetoothConnectionListener (BluetoothConnectionListener listener) {
    	mBluetoothConnectionListener = listener;
    }
    
    public void setAutoConnectionListener(AutoConnectionListener listener) {
    	mAutoConnectionListener = listener;
    }
    
    public void enable() {
    	mBluetoothAdapter.enable();
    }
    
    public void send(byte[] data) {
    	if(mChatService.getState() == BluetoothState.STATE_CONNECTED)
    		mChatService.write(data);
    }
    
    public void send(String data) {
    	if(mChatService.getState() == BluetoothState.STATE_CONNECTED)
    		mChatService.write(data.getBytes());
    }
    
    public String getConnectedDeviceName() {
    	return mDeviceName;
    }
    
    public String getConnectedDeviceAddress() {
    	return mDeviceAddress;
    }
    
    public String[] getPairedDeviceName() {
    	int c = 0;
    	Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();  
    	String[] name_list = new String[devices.size()];
        for (BluetoothDevice device : devices) {  
        	name_list[c] = device.getName();
        	c++;
        }  
        return name_list;
    }
    
    public String[] getPairedDeviceAddress() {
    	int c = 0;
    	Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();  
    	String[] address_list = new String[devices.size()];
        for (BluetoothDevice device : devices) {  
        	address_list[c] = device.getAddress();
        	c++;
        }  
        return address_list;
    }
    
    
    public void autoConnect(String keywordName) {
    	if(!isAutoConnectionEnabled) {
    		keyword = keywordName;
    		isAutoConnectionEnabled = true;
    		isAutoConnecting = true;
        	if(mAutoConnectionListener != null)
        		mAutoConnectionListener.onAutoConnectionStarted();
	    	final ArrayList<String> arr_filter_address = new ArrayList<String>();
	    	final ArrayList<String> arr_filter_name = new ArrayList<String>();
	    	String[] arr_name = getPairedDeviceName();
	    	String[] arr_address = getPairedDeviceAddress();
	    	for(int i = 0 ; i < arr_name.length ; i++) {
	    		if(arr_name[i].contains(keywordName)) {
	    			arr_filter_address.add(arr_address[i]);
	    			arr_filter_name.add(arr_name[i]);
	    		}
	    	}
	
	    	bcl = new BluetoothConnectionListener() {
				public void onDeviceConnected(String name, String address) {
					bcl = null;
		    		isAutoConnecting = false;
				}
	
				public void onDeviceDisconnected() { }
				public void onDeviceConnectionFailed() {
					if(isServiceRunning) {
						if(isAutoConnectionEnabled) {
							c++;
							if(c >= arr_filter_address.size())
						    	c = 0;
							connect(arr_filter_address.get(c));
				        	if(mAutoConnectionListener != null)
				        		mAutoConnectionListener.onNewConnection(arr_filter_name.get(c)
					    			, arr_filter_address.get(c));
						} else {
							bcl = null;
				    		isAutoConnecting = false;
						}
					}
				}
	    	};

	    	setBluetoothConnectionListener(bcl);
	    	c = 0;
        	if(mAutoConnectionListener != null)
        		mAutoConnectionListener.onNewConnection(arr_name[c], arr_address[c]);
        	if(arr_filter_address.size() > 0) 
        		connect(arr_filter_address.get(c));
        	else 
        		Toast.makeText(mContext, "Device name mismatch", Toast.LENGTH_SHORT).show();
    	}
    }
}
