package peakaboo.fileio.implementations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import peakaboo.fileio.DataSource;

import commonenvironment.AbstractFile;
import commonenvironment.IOOperations;
import fava.Fn;
import fava.functionable.Range;
import fava.signatures.FnEach;
import fava.signatures.FnGet;
import fava.signatures.FnMap;

import scitypes.Spectrum;
import scitypes.filebacked.FileBackedList;


public class PlainTextDataSource implements DataSource
{

	FnGet<Boolean>						isAborted;
	FnEach<Integer>						readScanCallback;
	
	//FileBackedList if it can be created, another implementation otherwise
	List<Spectrum>						scandata;
	

	String								datasetName;
	
	public PlainTextDataSource(AbstractFile file, FnEach<Integer> getScanCountCallback, FnEach<Integer> readScanCallback, FnGet<Boolean> isAborted) throws Exception
	{
		
		this.readScanCallback = readScanCallback;
		this.isAborted = isAborted;
		
		scandata = FileBackedList.<Spectrum>create("Peakaboo");
		datasetName = IOOperations.getFileTitle(  file.getFileName()  );
		
		InputStreamReader r = new InputStreamReader(file.getInputStream(), "UTF-8");
		BufferedReader reader = new BufferedReader(r);
		
		//we count the number of linebreaks in the file. This will slow down
		//reading marginally, but not by a lot, since the slowest part is
		//human readable to machine readable conversion.
		getScanCountCallback.f(IOOperations.characterCount(file, '\n'));
		
		String line;
		while (true)
		{
			
			line = reader.readLine();
			if (line == null || isAborted.f()) break;
			
			if (line.trim().equals("") || line.trim().startsWith("#")) continue;
						
			//split on all non-digit characters
			Spectrum scan = new Spectrum(Fn.map(line.trim().split("[, \\t]+"), new FnMap<String, Float>(){
				
				public Float f(String s)
				{
					try { return Float.parseFloat(s); } 
					catch (Exception e) { return 0f; }
					
				}}));
			
			
			if (scandata.size() > 0 && scan.size() != scandata.get(0).size()) throw new Exception("Spectra sizes are not equal");
			
			scandata.add(scan);
			
			readScanCallback.f(1);
			
		}
		
		reader.close();
		
	}
	
	public int estimateDataSourceSize()
	{
		return 1;
	}

	public String getDatasetName()
	{
		return datasetName;
	}

	public int getExpectedScanCount()
	{
		return 1;
	}

	public float getMaxEnergy()
	{
		return 0;
	}

	public Spectrum getScanAtIndex(int index)
	{
		return scandata.get(index);
	}

	public int getScanCount()
	{
		return scandata.size();
	}

	public List<String> getScanNames()
	{
		return Fn.map(new Range(0, scandata.size()-1), new FnMap<Integer, String>(){

			public String f(Integer element)
			{
				return "Scan #" + (element+1);
			}});
	}

	
	public static boolean filesMatchCriteria(List<AbstractFile> files)
	{
		if (files.size() != 1) return false;
		if (! files.get(0).getFileName().toLowerCase().endsWith(".txt")) return false;
		
		return true;
		
	}
	
	

}