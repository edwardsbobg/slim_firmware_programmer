package com.breathometer.production.firmware.programming;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.text.DefaultCaret;

import jssc.SerialPortList;

import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;
import javax.swing.SwingConstants;
import java.awt.Toolkit;

public class FrameMain 
{
	private static final String STATUS_SUCCESS = String.valueOf((char) Integer.parseInt("2714", 16));
	private static final String STATUS_PROGRESS = String.valueOf((char) Integer.parseInt("2022", 16)) + String.valueOf((char) Integer.parseInt("2022", 16)) + String.valueOf((char) Integer.parseInt("2022", 16));
	private static final String STATUS_FAILED = String.valueOf((char) Integer.parseInt("2716", 16));
	private static final Color STATUS_SUCCESS_COLOR = Color.GREEN;
	private static final Color STATUS_PROGRESS_COLOR = Color.BLACK;
	private static final Color STATUS_FAILED_COLOR = Color.RED;
    private static final String HEX_INJECTION_PREFIX_CODE = ":";
    private static final String HEX_INJECTION_PREFIX_DATA = "0B002800";
    private static final String HEX_FILE_NAME = "build\\program.hex";
    private static final int HEX_FILE_OFFSET = 108;
    private JFrame frmBreathometerProduction;
	private JTextField textFieldSerialNumber;
	private JTextField textFieldStatus;
	private JTextField textFieldBluetoothAddress;
	private JComboBox comboBoxScanner;
	private JComboBox comboBoxSlimBCMPort;
	private JComboBox comboBoxSlimMSPPort;
	private JTextArea textAreaConsole;
	private MotorolaDS4208 mMotorolaDS4208;
	private JButton btnLockScanner;
	private JButton btnRefreshScanner;
	private JButton btnLockSlimBCMPort;
	private JButton btnRefreshSlimBCMPort;
	private JButton btnLockSlimMSPPort;
	private JButton btnRefreshSlimMSPPort;

	
	
	public static void main(String[] args) 
	{
		try 
		{
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} 
		catch (Throwable e) 
		{
			e.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() 
			{
				try 
				{
					FrameMain window = new FrameMain();
					window.frmBreathometerProduction.setVisible(true);
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
		});
	}

	public FrameMain() 
	{
		initialize();
		updateTitle();
		updateScanners();
		updateSlimBCMPort();
		updateSlimMSPPort();
		
		mMotorolaDS4208 = new MotorolaDS4208(new MotorolaDS4208.MotorolaDS4208Interface()
		{
			@Override
			public void scannedString(String string)
			{
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						comboBoxScanner.setEnabled(false);
						comboBoxSlimBCMPort.setEnabled(false);
						comboBoxSlimMSPPort.setEnabled(false);
						btnRefreshScanner.setEnabled(false);
						btnLockSlimBCMPort.setEnabled(false);
						btnLockSlimMSPPort.setEnabled(false);
						btnLockScanner.setText("Unlock");
						btnLockSlimBCMPort.setText("Unlock");
						btnLockSlimMSPPort.setText("Unlock");
						
						textFieldStatus.setText(STATUS_PROGRESS);
						textFieldStatus.setForeground(STATUS_PROGRESS_COLOR);

                        DeviceInfo info;
                        try {
                            info = new DeviceInfo(string,getPrefix());
                        } catch (Exception e) {
                            showInvalid(e.getMessage());
                            return;
                        }

						textFieldSerialNumber.setText(info.getSerialNumber());
						textFieldBluetoothAddress.setText(info.getBluetoothDeviceAddress());
                        if (makeProgramHEX(info) && modifyProgramHex(info) && runChipload()) {

//                        if (runChipload()) {
                            textFieldStatus.setText(STATUS_SUCCESS);
                            textFieldStatus.setForeground(STATUS_SUCCESS_COLOR);
                        };
                    }
				}).start();
			}
		});
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(mMotorolaDS4208);
		
	}

    private boolean runBatch(String batchFileName, String[] args) {
        String[] cmd = new String[] {"cmd", "/c", batchFileName};
        TextAreaProcess thread = new TextAreaProcess(textAreaConsole, Utils.concat(cmd,args));
        
        new Thread(thread).start();

        while (!thread.isDone())
        {
            try { Thread.sleep(1); }
            catch (InterruptedException e) { }
        }

        int result = thread.getResult();
        if (result == 0)
        {
            return true;
        }
        else
        {
            showInvalid(batchFileName + " failed");
            return false;
        }
    }

    private boolean makeProgramHEX(DeviceInfo info) {
        return runBatch("make_hex.bat",new String[]{info.getBluetoothDeviceAddress()});
    }

    private boolean modifyProgramHex(DeviceInfo info) {
        String snHex = Utils.toHex(info.getSerialNumber());
        snHex = new String(Arrays.copyOfRange(snHex.getBytes(),snHex.length()-info.getSerialNumber().length()*2,snHex.length()));
        String injection = HEX_INJECTION_PREFIX_DATA + snHex;
        try {
            injection = "\n" + HEX_INJECTION_PREFIX_CODE + injection + Utils.computeChecksum(injection);
            Utils.insert(HEX_FILE_NAME,HEX_FILE_OFFSET,injection.getBytes());
            return true;
        } catch (Exception e) {
            showInvalid(e.getMessage());
        }
        return false;
    }

    private boolean runChipload() {
        return (runBatch("msp_flash_jlink.bat", null) &&   runBatch("run_chipload.bat",new String[]{comboBoxSlimBCMPort.getSelectedItem().toString()}));
    }

    private void showInvalid(String msg) {
        textFieldSerialNumber.setText(msg);
        textFieldBluetoothAddress.setText(msg);
        textFieldStatus.setText(STATUS_FAILED);
        textFieldStatus.setForeground(STATUS_FAILED_COLOR);
    }

    private void initialize()
	{
		frmBreathometerProduction = new JFrame();
		frmBreathometerProduction.setIconImage(Toolkit.getDefaultToolkit().getImage(FrameMain.class.getResource("/com/breathometer/production/firmware/programming/icon.png")));
		frmBreathometerProduction.setResizable(false);
		frmBreathometerProduction.setTitle("Breathometer Slim FW Programmer");
		//frmBreathometerProduction.setBounds(100, 100, 526, 440);
		frmBreathometerProduction.setBounds(100, 100, 526, 660);
		frmBreathometerProduction.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmBreathometerProduction.getContentPane().setLayout(null);
		
		JLabel lblScanner = new JLabel("Scanner");
		lblScanner.setBounds(10, 11, 150, 14);
		frmBreathometerProduction.getContentPane().add(lblScanner);
		
		comboBoxScanner = new JComboBox();
		comboBoxScanner.setBounds(10, 30, 150, 20);
		frmBreathometerProduction.getContentPane().add(comboBoxScanner);
		
		btnRefreshScanner = new JButton("Refresh");
		btnRefreshScanner.setBounds(170, 29, 70, 23);
		btnRefreshScanner.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				updateScanners();
			}
		});
		frmBreathometerProduction.getContentPane().add(btnRefreshScanner);
		
		btnLockScanner = new JButton("Lock");
		btnLockScanner.setBounds(170, 54, 70, 23);
		btnLockScanner.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (btnLockScanner.getText().equals("Lock"))
				{
					btnLockScanner.setText("Unlock");
					comboBoxScanner.setEnabled(false);
					btnRefreshScanner.setEnabled(false);
				}
				else
				{
					btnLockScanner.setText("Lock");
					comboBoxScanner.setEnabled(true);
					btnRefreshScanner.setEnabled(true);
				}
			}
		});
		frmBreathometerProduction.getContentPane().add(btnLockScanner);
		
		JLabel lblSlimBCMPort = new JLabel("Slim BCM Port");
		lblSlimBCMPort.setBounds(10, 61, 150, 14);
		frmBreathometerProduction.getContentPane().add(lblSlimBCMPort);
		
		//Need to add in label for MSP, as we are programming 2 MCUs now
		JLabel lblSlimMSPPort = new JLabel("Slim MSP Port");
		lblSlimMSPPort.setBounds(10, 111, 150, 14);
		frmBreathometerProduction.getContentPane().add(lblSlimMSPPort);
		
		comboBoxSlimBCMPort = new JComboBox();
		comboBoxSlimBCMPort.setBounds(10, 80, 150, 20);
		frmBreathometerProduction.getContentPane().add(comboBoxSlimBCMPort);
		
		comboBoxSlimMSPPort = new JComboBox();
		comboBoxSlimMSPPort.setBounds(10, 129, 150, 20);
		frmBreathometerProduction.getContentPane().add(comboBoxSlimMSPPort);
		
		btnRefreshSlimBCMPort = new JButton("Refresh");
		btnRefreshSlimBCMPort.setBounds(170, 79, 70, 23);
		btnRefreshSlimBCMPort.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				updateSlimBCMPort();
				if (!areValidPorts()) {
					textAreaConsole.append("COM Ports cannot be the same. Please choose a different configuration.\r\n");
				} else {
					runBatchNoParams("res/batch/msp_flash_jlink.bat");
				}
			}
		});
		frmBreathometerProduction.getContentPane().add(btnRefreshSlimBCMPort);
		
		btnRefreshSlimMSPPort = new JButton("Refresh");
		btnRefreshSlimMSPPort.setBounds(170, 129, 70, 23);
		btnRefreshSlimMSPPort.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				updateSlimMSPPort();
			}
		});
		frmBreathometerProduction.getContentPane().add(btnRefreshSlimMSPPort);
		
		btnLockSlimBCMPort = new JButton("Lock");
		btnLockSlimBCMPort.setBounds(170, 103, 70, 23);
		btnLockSlimBCMPort.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (btnLockSlimBCMPort.getText().equals("Lock"))
				{
					btnLockSlimBCMPort.setText("Unlock");
					comboBoxSlimBCMPort.setEnabled(false);
					btnRefreshSlimBCMPort.setEnabled(false);
				}
				else
				{
					btnLockSlimBCMPort.setText("Lock");
					comboBoxSlimBCMPort.setEnabled(true);
					btnRefreshSlimBCMPort.setEnabled(true);
				}
			}
		});
		frmBreathometerProduction.getContentPane().add(btnLockSlimBCMPort);
		
		btnLockSlimMSPPort = new JButton("Lock");
		btnLockSlimMSPPort.setBounds(170, 152, 70, 23);
		btnLockSlimMSPPort.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (btnLockSlimMSPPort.getText().equals("Lock"))
				{
					btnLockSlimMSPPort.setText("Unlock");
					comboBoxSlimMSPPort.setEnabled(false);
					btnRefreshSlimMSPPort.setEnabled(false);
				}
				else
				{
					btnLockSlimMSPPort.setText("Lock");
					comboBoxSlimMSPPort.setEnabled(true);
					btnRefreshSlimMSPPort.setEnabled(true);
				}
			}
		});
		frmBreathometerProduction.getContentPane().add(btnLockSlimMSPPort);
		
		JLabel lblSerialNumber = new JLabel("Serial Number");
		lblSerialNumber.setBounds(250, 11, 91, 14);
		frmBreathometerProduction.getContentPane().add(lblSerialNumber);
		
		textFieldSerialNumber = new JTextField();
		textFieldSerialNumber.setBounds(250, 30, 150, 20);
		textFieldSerialNumber.setEditable(false);
		frmBreathometerProduction.getContentPane().add(textFieldSerialNumber);
		textFieldSerialNumber.setColumns(10);
		
		JLabel lblBluetoothAddress = new JLabel("Bluetooth Address");
		lblBluetoothAddress.setBounds(250, 61, 150, 14);
		frmBreathometerProduction.getContentPane().add(lblBluetoothAddress);
		
		textFieldBluetoothAddress = new JTextField();
		textFieldBluetoothAddress.setEditable(false);
		textFieldBluetoothAddress.setBounds(250, 80, 150, 20);
		frmBreathometerProduction.getContentPane().add(textFieldBluetoothAddress);
		textFieldBluetoothAddress.setColumns(10);
		
		JLabel lblStatus = new JLabel("Status");
		lblStatus.setBounds(410, 11, 90, 14);
		frmBreathometerProduction.getContentPane().add(lblStatus);
		
		textFieldStatus = new JTextField();
		textFieldStatus.setFont(new Font("Arial Unicode MS", Font.PLAIN, 70));
		textFieldStatus.setHorizontalAlignment(SwingConstants.CENTER);
		textFieldStatus.setBounds(410, 30, 90, 90);
		textFieldStatus.setEditable(false);
		frmBreathometerProduction.getContentPane().add(textFieldStatus);
		textFieldStatus.setColumns(10);
		
		JLabel lblConsole = new JLabel("Console");
		lblConsole.setBounds(10, 180, 150, 14);
		frmBreathometerProduction.getContentPane().add(lblConsole);
		
		JScrollPane scrollPaneConsole = new JScrollPane();
		scrollPaneConsole.setAutoscrolls(true);
		scrollPaneConsole.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneConsole.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPaneConsole.setBounds(10, 195, 500, 250);
		frmBreathometerProduction.getContentPane().add(scrollPaneConsole);
		
		textAreaConsole = new JTextArea();
		textAreaConsole.setFont(new Font("Monospaced", Font.PLAIN, 10));
		scrollPaneConsole.setViewportView(textAreaConsole);
		textAreaConsole.setLineWrap(true);
		textAreaConsole.setForeground(Color.WHITE);
		textAreaConsole.setBackground(Color.BLACK);
		textAreaConsole.setEditable(false);
		DefaultCaret caret = (DefaultCaret) textAreaConsole.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		JMenuBar menuBar = new JMenuBar();
		frmBreathometerProduction.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		});
		mnFile.add(mntmExit);
		
	}
	
	private boolean runBatchNoParams(String batchFileName) {
        String[] cmd = new String[] {"cmd", "/c", batchFileName};
        TextAreaProcess thread = new TextAreaProcess(textAreaConsole, cmd);
        
        new Thread(thread).start();

        while (!thread.isDone())
        {
            try { Thread.sleep(1); }
            catch (InterruptedException e) { }
        }

        int result = thread.getResult();
        if (result == 0)
        {
            return true;
        }
        else
        {
            showInvalid(batchFileName + " failed");
            return false;
        }
		
	}

	private void updateScanners()
	{
		comboBoxScanner.removeAllItems();
		com.codeminders.hidapi.ClassPathLibraryLoader.loadNativeHIDLibrary();
		try
		{
			HIDManager hidManager = HIDManager.getInstance();
			HIDDeviceInfo[] devices = hidManager.listDevices();
			if (devices != null)
			{
				for (HIDDeviceInfo device : devices)
				{
					String deviceName = device.getProduct_string();
					comboBoxScanner.addItem(deviceName);
				}
			}
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void updateSlimBCMPort()
	{
		comboBoxSlimBCMPort.removeAllItems();
		String[] serialPortList = SerialPortList.getPortNames();
		if (serialPortList != null)
		{
			for (String serialPort : serialPortList)
				comboBoxSlimBCMPort.addItem(serialPort);
		}
		
	}
	
	private void updateSlimMSPPort()
	{
		comboBoxSlimMSPPort.removeAllItems();
		String[] serialPortList = SerialPortList.getPortNames();
		if (serialPortList != null)
		{
			for (String serialPort : serialPortList)
				comboBoxSlimMSPPort.addItem(serialPort);
		}
	}
	
	private void updateTitle()
	{
		String title;
		
		try
		{
			title = Utils.readFile("res/config/mint/title.txt", StandardCharsets.UTF_8);
			
			frmBreathometerProduction.setTitle(title);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}

	private String getPrefix() throws IOException {
		return Utils.readFile("res/config/prefix.txt", StandardCharsets.UTF_8);
	}
	
	private boolean areValidPorts()
	{
		return !comboBoxSlimBCMPort.getSelectedItem().toString().equals(comboBoxSlimMSPPort.getSelectedItem().toString());
	}
	
}
