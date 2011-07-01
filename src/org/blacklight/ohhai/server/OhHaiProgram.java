package org.blacklight.ohhai.server;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import org.blacklight.ohhai.socket.*;

/**
 * Main class for the program
 * @author blacklight
 *
 */
public class OhHaiProgram extends Activity implements Serializable {
	private final static long serialVersionUID = -4209208543883385035L;
	private final static String appDir = "/mnt/sdcard/ohhaisms";
	private final static String logFile = "OhHaiLog.txt";
	private final static String pwdFile = "passwd";
	private static OhHaiServerSocket.ServerSocketType socketType;
	private static int listenPort;
	
	// GUI widgets
	private Button okButton;
	private Button quitButton;
	private TextView tv;
	private TextView tvListenPort;
	private EditText etListenPort;
	private RadioButton btButton;
	private RadioButton wifiButton;
	
	public static void addMessage(String text, PrintWriter out, Throwable e)
	{ addMessage(text, out, e, false); }
	
	/**
	 * Actions to be performed when a new log message is generated by the software
	 * @param text Text of the generated message
	 * @param out Object identifying an output stream to send the message too as well (null if none)
	 * @param e Object identifying the exception raised by the software (null if none)
	 */
	public static void addMessage(String text, PrintWriter out, Throwable e, boolean showAlert)
	{
		String stacktrace = "";
		
		if (getPasswd() == null)
			text = "Warning: no password has been set. We strongly recommend you " +
				"to set one through ohhaiclient -s option\n" + text;
		
		if (e != null)
		{
			StringWriter result = new StringWriter();
			PrintWriter writer = new PrintWriter(result);
			e.printStackTrace(writer);
			stacktrace = result.toString();
			writer.close();
			
			text += "\n" + stacktrace + "\n";
		}
		
		try
		{
			BufferedWriter file = new BufferedWriter(
				new FileWriter(appDir + "/" + logFile, true)
			);
			
			file.write(text + "\n");
			file.flush();
			file.close();
			
			if (out != null)
			{
				out.write(text + "\n");
				out.flush();
			}
		}
		
		catch (Exception ex)  {}
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		try
		{
			if (!(new File(appDir)).exists())
				new File(appDir).mkdirs();
		}
		
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
		
		tvListenPort = (TextView) findViewById(R.id.listenPortText);
		tvListenPort.setVisibility(View.INVISIBLE);
		
		etListenPort = (EditText) findViewById(R.id.listenPortEdit);
		etListenPort.setVisibility(View.INVISIBLE);
		
		tv = (TextView) findViewById(R.id.textView);
		
		btButton = (RadioButton) findViewById(R.id.btButton);
		wifiButton = (RadioButton) findViewById(R.id.wifiButton);
		
		btButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (wifiButton.isChecked())
				{
					wifiButton.setChecked(false);
					btButton.setChecked(true);
				}
				
				tvListenPort.setVisibility(View.INVISIBLE);
				etListenPort.setVisibility(View.INVISIBLE);
			}
		});
		
		wifiButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (btButton.isChecked())
				{
					wifiButton.setChecked(true);
					btButton.setChecked(false);
				}
				
				tvListenPort.setVisibility(View.VISIBLE);
				etListenPort.setVisibility(View.VISIBLE);
			}
		});
		
		okButton = (Button) findViewById(R.id.okButton);
		okButton.setOnClickListener(new View.OnClickListener()  {
			@Override
			public void onClick (View v)
			{
				socketType = (wifiButton.isChecked()) ?
					OhHaiServerSocket.ServerSocketType.InetSocket :
					OhHaiServerSocket.ServerSocketType.BluetoothSocket;
				
				if (socketType == OhHaiServerSocket.ServerSocketType.InetSocket)
				{
					if (etListenPort.getText().toString().length() == 0)
					{
						tv.setText("No port specified for the WiFi connection");
						return;
					}
					
					listenPort = Integer.parseInt(etListenPort.getText().toString());
				}
				
		        try
		        {
		        	Intent service = new Intent(OhHaiProgram.this, OhHaiService.class);
					startService (service);
			        
			        if (socketType == OhHaiServerSocket.ServerSocketType.InetSocket)
			        {
				        tv.setText("WiFi service successfully started on port " + listenPort);
			        } else {
				        tv.setText("Bluetooth service successfully started");
			        }
			        
			        wifiButton.setVisibility(View.INVISIBLE);
			        btButton.setVisibility(View.INVISIBLE);
			        okButton.setVisibility(View.INVISIBLE);
			        tvListenPort.setVisibility(View.INVISIBLE);
			        etListenPort.setVisibility(View.INVISIBLE);
		        }
		        
		        catch (Exception e)  {
		        	addMessage("Error while starting the service: " + e.toString(), null, e, true);
		        }
			}
		});
		
		quitButton = (Button) findViewById(R.id.quitButton);
		quitButton.setOnClickListener(new View.OnClickListener()  {
			@Override
			public void onClick (View v)
			{
				System.exit(0);
			}
		});
    }
    
    public static String getLocalIpAddress()
    {
    	try
    	{
    		for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
    			NetworkInterface intf = en.nextElement();
    			
    			for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
    				InetAddress inetAddress = enumIpAddr.nextElement();
    				
    				if (!inetAddress.isLoopbackAddress()) {
    					return inetAddress.getHostAddress().toString();
    				}
    			}
    		}
    	} catch (SocketException ex) {}
    	
    	return null;
    }
    
    public static OhHaiServerSocket.ServerSocketType getSocketType()  { return socketType; }
    public static int getListenPort()  { return listenPort; }
    
    public void showAlert (String msg)
    {
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    
	    builder.setMessage(msg)
	           .setCancelable(false)
	           .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	                    OhHaiProgram.this.finish();
	               }
	           }).create();
    }
    
    public static String getPasswd()
    {
    	try
    	{
    		BufferedReader in = new BufferedReader(new FileReader(appDir + "/" + pwdFile));
    		String pwd = in.readLine();
    		in.close();
    		return pwd;
    	}
    	
    	catch (Exception e)
    	{
    		return null;
    	}
    }
    
    public static void changePasswd (String newPasswd)
    	throws IOException
    {
    	try
    	{
    		BufferedWriter out = new BufferedWriter (new FileWriter(appDir + "/" + pwdFile));
    		out.write(newPasswd + "\n");
    		out.close();
    	}
    	
    	catch (Exception e)
    	{
    		throw new IOException ("Unable to change the password");
    	}
    }
}
