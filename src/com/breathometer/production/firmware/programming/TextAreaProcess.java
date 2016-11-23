package com.breathometer.production.firmware.programming;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.swing.JTextArea;

public class TextAreaProcess implements Runnable
{
	private static final int TEXT_AREA_MAX_LINES = 1024;
	private JTextArea textAreaConsole;
	private String[] command;
	private boolean done = false;
	private int result = -1;
	
	
	
	public TextAreaProcess(JTextArea textArea, String[] cmd)
	{
		super();
		textAreaConsole = textArea;
		command = cmd;
	}
	
	
	
	@Override
	public void run()
	{
		Process process;
		try
		{
			process = Runtime.getRuntime().exec(command, null, Utils.getJarLocation());
			while (!done)
			{
				try
				{
					result = process.exitValue(); // this will throw an exception while running
					done = true;
				}
				catch (IllegalThreadStateException e)
				{
					BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					
					String line = "";			
					while ((line = reader.readLine()) != null) 
					{
						textAreaPrintln(line);
					}
				}
				
				try { Thread.sleep(1); } 
				catch (InterruptedException e) { }
			}
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean isDone()
	{
		return done;
	}
	
	public int getResult()
	{
		return result;
	}
	
	
	
	private void textAreaPrintln(String str)
	{
		if (textAreaConsole.getLineCount() > TEXT_AREA_MAX_LINES)
		{ // if we are over the max lines, remove the first line
			String current = textAreaConsole.getText();
			String next = current.substring(current.indexOf(System.lineSeparator()) + System.lineSeparator().length());
			textAreaConsole.setText(next);
		}
		
		textAreaConsole.append(str + System.lineSeparator());
	}
	
}
