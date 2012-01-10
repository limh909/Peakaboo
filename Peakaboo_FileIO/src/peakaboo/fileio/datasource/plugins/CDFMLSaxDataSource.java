package peakaboo.fileio.datasource.plugins;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bolt.plugin.Plugin;

import com.esotericsoftware.kryo.serialize.ArraySerializer;

import commonenvironment.AbstractFile;
import commonenvironment.IOOperations;

import fava.functionable.FList;
import fava.functionable.Range;
import fava.signatures.FnEach;
import fava.signatures.FnGet;

import peakaboo.common.Version;
import peakaboo.fileio.DataSource;
import peakaboo.fileio.DSRealDimensions;
import peakaboo.fileio.DSMetadata;
import peakaboo.fileio.KryoScratchList;
import peakaboo.fileio.datasource.AbstractCachedDataSourcePlugin;
import peakaboo.fileio.datasource.AbstractDataSourcePlugin;
import peakaboo.fileio.datasource.plugins.cdfml.CDFMLReader;
import peakaboo.fileio.datasource.plugins.cdfml.CDFMLStrings;
import scitypes.Bounds;
import scitypes.Coord;
import scitypes.Spectrum;
import scitypes.SpectrumCalculations;


@Plugin
public class CDFMLSaxDataSource extends AbstractCachedDataSourcePlugin
{

	int											scanReadCount;

	//File-backed List, if it could be created. Some other kind if not
	//List<Spectrum>								correctedData;
	Spectrum									iNaughtNormalized;
	
	Set<String>									hasCategory;
	
	CDFMLReader									reader;

	public CDFMLSaxDataSource()
	{
		
		reader = new CDFMLReader() {
			
			@Override
			protected void processedSpectrum(String varname, int entryNo, Spectrum spectrum)
			{
				handleProcessedSpectrum(varname, entryNo, spectrum);
			}
		};
		
	}

	
	
	private boolean isNewVersion()
	{
		
		if (reader.hasVar(CDFMLStrings.VAR_MCA_SPECTRUM + "0")) return true;
		if (reader.hasVar(CDFMLStrings.VAR_MCA_SUMSPECTRUM)) return true;
		return false;
	}
	
	private boolean hasSumSpectrum()
	{
		if (!isNewVersion()) return false;
		if (reader.hasVar(CDFMLStrings.VAR_MCA_SUMSPECTRUM)) return true;
		return false;
	}
	
	
	private int numElements()
	{
		if (isNewVersion()) {
			
			if (hasSumSpectrum()) return 1;
			
			return reader.getAttrInt(CDFMLStrings.ATTR_MCA_NUM_ELEMENTS, 0);
			
		} else {
			return 1;
		}
	}
	
	private Spectrum getScan(int element, int index)
	{	
		
		if (isNewVersion()) {
			
			if (hasSumSpectrum() && element == 0) {
				return reader.getVarSpectra(CDFMLStrings.VAR_MCA_SUMSPECTRUM).get(index);
			} else {
				return reader.getVarSpectra(CDFMLStrings.VAR_MCA_SPECTRUM + element).get(index);
			}
			
		} else if (reader.hasVar(CDFMLStrings.VAR_XRF_SPECTRUMS)){
			return reader.getVarSpectra(CDFMLStrings.VAR_XRF_SPECTRUMS).get(index);
		}
		
		return null;
		
		
	}
	
	private Spectrum getScan(int index)
	{
		return getScan(0, index);
	}
	
	private Float getDeadtime(int index)
	{
		return getDeadtime(0, index);
	}
	private Float getDeadtime(int element, int index)
	{
		if (reader.hasVar(CDFMLStrings.VAR_MCA_DEADTIME + "0")) {
			if (numElements() == 1 && element == 1){
				return Math.max(0f, reader.getVarFloats(CDFMLStrings.VAR_MCA_DEADTIME).get(index) / 100f);
			}
			return Math.max(0f, reader.getVarFloats(CDFMLStrings.VAR_MCA_DEADTIME + element).get(index) / 100f);	
		} else {
			return 0f;
		}
		
	}
	
	private Float getINaught(int index)
	{
		if (!reader.hasVar(CDFMLStrings.VAR_NORMALISE)) return 1f;
		
		if (iNaughtNormalized == null) {
			iNaughtNormalized = SpectrumCalculations.normalize(new Spectrum(reader.getVarFloats(CDFMLStrings.VAR_NORMALISE)));
		}
		
		return iNaughtNormalized.get(index);
		
	}
	
	private int numScans()
	{
		
		if (isNewVersion()) {
			
			if (hasSumSpectrum()) {
				return reader.getVarAttrInt(CDFMLStrings.VAR_MCA_SUMSPECTRUM, CDFMLStrings.XML_ATTR_NUMRECORDS);
			} else {
				return reader.getVarAttrInt(CDFMLStrings.VAR_MCA_SPECTRUM + "0", CDFMLStrings.XML_ATTR_NUMRECORDS);	
			}
						
		} else if (reader.hasVar(CDFMLStrings.VAR_XRF_SPECTRUMS)) {
			
			return reader.getVarAttrInt(CDFMLStrings.VAR_XRF_SPECTRUMS, CDFMLStrings.XML_ATTR_NUMRECORDS);
			
		} else {
			
			return 0;
		}
	}
	

	
	

	////////////////////////////////////////////////////////////
	// VARIABLES DATA
	////////////////////////////////////////////////////////////


	@Override
	public Spectrum loadScanAtIndex(int index)
	{

		Spectrum s, s2;
				
		//if this is a multi-element data set, we store the averaged data the 'correctedData' list
		//and the individual spectra in indices 0->N-1. The first time this data is accessed, the
		//'correctedData' index value will be empty, because we won't have calcualted it yet.
		//if ( (correctedData.size() <= index || correctedData.get(index) == null) && numElements() > 1)
		
		if (numElements() > 1)
		{
			
			
			s = new Spectrum(getScan(0, 0).size(), 0f);
			for (Integer i : new Range(0, numElements()-1))
			{
				
				s2 = getScan(i, index);
				
				if (s2 != null) {
					
					//divide by deadtime percent if not 0
					if (getDeadtime(i, index) != 0) {
						SpectrumCalculations.multiplyBy_inplace(s2, 1f - getDeadtime(i, index));
					}
					//add the adjusted value to the total
					SpectrumCalculations.addLists_inplace(s, s2);
					
				}
				
				
			}
			
			float iNaught = getINaught(index);

			if (iNaught != 0) SpectrumCalculations.divideBy_inplace(s, iNaught);
			else SpectrumCalculations.multiplyBy(s, 0);
			
			//commit the newly calculated value to the dataset
			//correctedData.set(index, s);
			return s;
			
			
		//dont have the data cached yet, and its just a single-element dataset
		//} else if ( (correctedData.size() <= index || correctedData.get(index) == null) && numElements() == 1) {
		} else {
			
			Spectrum raw = getScan(index);
			
			if (raw == null) {
				s = new Spectrum(getScan(0, 0).size(), 0f);
			} else {
				s = new Spectrum(getScan(index));
			}
			
			//adjust for deadtime
			if (getDeadtime(index) != 0){
				SpectrumCalculations.multiplyBy_inplace(s, 1f - getDeadtime(index));
			}
			
			
			float iNaught = getINaught(index);

			if (iNaught != 0) SpectrumCalculations.divideBy_inplace(s, iNaught);
			else SpectrumCalculations.multiplyBy(s, 0);
			
			//commit the newly calculated value to the dataset
			//correctedData.set(index, s);
			return s;
			
		}
		

		//return correctedData.get(index);
		
	}

	
	public int getScanCount()
	{
		return numScans();
	}

	
	public Coord<Number> getRealCoordinatesAtIndex(int index)
	{
		Coord<Number> dims = new Coord<Number>(0, 0);

		
		
		dims.x =  reader.getVarFloats(CDFMLStrings.VAR_X_POSITONS).get(index);
		dims.y = reader.getVarFloats(CDFMLStrings.VAR_Y_POSITONS).get(index);
		return dims;

	}


	
	
	
	
	
	
	
	

	////////////////////////////////////////////////////////////
	// ATTRIBUTE DATA
	////////////////////////////////////////////////////////////



	public String getDatasetName()
	{
		String Project = reader.getAttr(CDFMLStrings.ATTR_PROJECT_NAME, 0);
		String DatasetName = reader.getAttr(CDFMLStrings.ATTR_DATASET_NAME, 0);
		String SampleName = reader.getAttr(CDFMLStrings.ATTR_SAMPLE_NAME, 0);

		String name = "";

		if (Project == null) return name;
		name += Project;

		if (DatasetName == null) return name;
		name += ": " + DatasetName;

		if (SampleName == null) return name;
		name += " on " + SampleName;

		return name;

	}


	
	public float getMaxEnergy()
	{
		
		String maxEnergyValue;
		
		if (reader.hasAttr(CDFMLStrings.ATTR_MCA_MAX_ENERGY)) {
			maxEnergyValue = reader.getAttr(CDFMLStrings.ATTR_MCA_MAX_ENERGY, 0);
		} else if (reader.hasAttr(CDFMLStrings.ATTR_XRF_MAX_ENERGY)) {
			maxEnergyValue = reader.getAttr(CDFMLStrings.ATTR_XRF_MAX_ENERGY, 0);
		} else {
			return 0f;
		}
		if (maxEnergyValue == null) return 0f;
		
		return Float.parseFloat(maxEnergyValue) / 1000.0f;
	}


	
	public List<String> getScanNames()
	{
		List<String> scannames = new ArrayList<String>();

		for (int i = 0; i < getScanCount(); i++)
		{
			scannames.add("Scan #" + (i + 1));
		}

		return scannames;
	}


	private int getDataWidth()
	{
		int width = Integer.parseInt(reader.getAttr(CDFMLStrings.ATTR_DATA_X, 0));
		return width;
	}


	private int getDataHeight()
	{
		int height = Integer.parseInt(reader.getAttr(CDFMLStrings.ATTR_DATA_Y, 0));
		return height;
	}


	
	public Coord<Integer> getDataDimensions()
	{
		int width = getDataWidth();
		int height = getDataHeight();
		return new Coord<Integer>(width, height);
	}


	
	public Coord<Bounds<Number>> getRealDimensions()
	{
		float x1, x2, y1, y2;

		x1 = Float.parseFloat(reader.getAttr(CDFMLStrings.ATTR_DIM_X_START, 0));
		x2 = Float.parseFloat(reader.getAttr(CDFMLStrings.ATTR_DIM_X_END, 0));
		y1 = Float.parseFloat(reader.getAttr(CDFMLStrings.ATTR_DIM_Y_START, 0));
		y2 = Float.parseFloat(reader.getAttr(CDFMLStrings.ATTR_DIM_Y_END, 0));


		Bounds<Number> xDim = new Bounds<Number>(x1, x2);
		Bounds<Number> yDim = new Bounds<Number>(y1, y2);
		return new Coord<Bounds<Number>>(xDim, yDim);
	}


	
	public String getRealDimensionsUnit()
	{
		return reader.getAttr(CDFMLStrings.ATTR_DIM_X_START, 1);
	}



	
	public String getCreationTime()
	{
		return reader.getAttr(CDFMLStrings.ATTR_CREATION_TIME, 0);
	}


	public String getCreator()
	{
		return reader.getAttr(CDFMLStrings.ATTR_CREATOR, 0);
	}


	public String getEndTime()
	{
		return reader.getAttr(CDFMLStrings.ATTR_END_TIME, 0);
	}



	public String getExperimentName()
	{
		return reader.getAttr(CDFMLStrings.ATTR_EXPERIMENT_NAME, 0);
	}


	public String getFacilityName()
	{
		return reader.getAttr(CDFMLStrings.ATTR_FACILITY, 0);
	}


	public String getInstrumentName()
	{
		return reader.getAttr(CDFMLStrings.ATTR_INSTRUMENT, 0);
	}


	public String getLaboratoryName()
	{
		return reader.getAttr(CDFMLStrings.ATTR_LABORATORY, 0);
	}


	public String getProjectName()
	{
		return reader.getAttr(CDFMLStrings.ATTR_PROJECT_NAME, 0);
	}


	public String getSampleName()
	{
		return reader.getAttr(CDFMLStrings.ATTR_SAMPLE_NAME, 0);
	}


	public String getScanName()
	{
		return reader.getAttr(CDFMLStrings.ATTR_DATASET_NAME, 0);
	}


	public String getSessionName()
	{
		return reader.getAttr(CDFMLStrings.ATTR_SESSION_NAME, 0);
	}


	public String getStartTime()
	{
		return reader.getAttr(CDFMLStrings.ATTR_START_TIME, 0);
	}


	public String getTechniqueName()
	{
		return reader.getAttr(CDFMLStrings.ATTR_TECHNIQUE, 0);
	}


	
	public boolean hasMetadata()
	{
		return true;
	}


	public boolean hasRealDimensions()
	{
		if (hasCategory.contains(CDFMLStrings.CAT_MapXY_1)) return true;
		return false;
	}

	
	public static boolean filesMatchCriteria(List<AbstractFile> files)
	{
		if (files.size() != 1) return false;
		String ext = files.get(0).getFileName().toLowerCase();
		if (!   (ext.endsWith(".xml") || ext.endsWith(".cdfml"))  ) return false;
		return true;
	}


	protected void handleProcessedSpectrum(String varname, int entryNo, Spectrum spectrum)
	{
		
		//if this is the first scan we're looking at
		if (scanReadCount == 0) {
		
			int totalScanCount = 0;
			
			
			//new version and old version have different criteria for determining how many scans
			if (reader.hasVarAttr(varname, CDFMLStrings.XML_ATTR_NUMRECORDS) && reader.hasAttr(CDFMLStrings.ATTR_MCA_NUM_ELEMENTS)) {
						
				totalScanCount = reader.getVarAttrInt(varname, CDFMLStrings.XML_ATTR_NUMRECORDS) * reader.getAttrInt(CDFMLStrings.ATTR_MCA_NUM_ELEMENTS, 0);

			}
			//we assume that in the older version, there will be only one spectrum recordset
			else if (reader.hasVarAttr(varname, CDFMLStrings.XML_ATTR_NUMRECORDS)) {
				
				totalScanCount = reader.getVarAttrInt(varname, CDFMLStrings.XML_ATTR_NUMRECORDS);
				
			}
			
			getScanCountCallback.f(totalScanCount);
			
			
		}
		
		cache(entryNo, spectrum);
		
		scanReadCount++;
		readScanCallback.f(1);
		
	}
	
	
	
	
	
	
	
	
	//==============================================
	// PLUGIN METHODS
	//==============================================


	@Override
	public boolean canRead(String filename)
	{	
		String ext = filename.toLowerCase();
		if (!   (ext.endsWith(".xml") || ext.endsWith(".cdfml"))  ) return false;
		
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			
			int lineCount = 0;
			String start = "", line;
			while (lineCount < 10)
			{
				line = reader.readLine().trim();
				if (line.length() == 0) continue;
				start += line + "\n";
				lineCount++;
			}
			
			return start.toLowerCase().contains("http://cdf.gsfc.nasa.gov");
		}
		catch (Exception e){}
		
		return false;
	}

	@Override
	public boolean canRead(List<String> filenames)
	{
		if (filenames.size() == 1) return canRead(filenames.get(0));
		return false;
	}

	@Override
	public void read(String filename) throws Exception
	{
		
		
		reader.read(filename, isAborted);
		
		/*
		KryoScratchList<Spectrum> newlist;
		try {
			newlist = new KryoScratchList<Spectrum>(Version.program_name + " - Corrected Spectrum", Spectrum.class);
			newlist.register(float[].class, new ArraySerializer(newlist.getKryo()));
			correctedData = newlist;
		} catch (IOException e) {
			correctedData = new FList<Spectrum>();
		}
		*/
		
		//get a listing of all of the categories that this supports
		hasCategory = new HashSet<String>();
		for (String s : reader.getAttrEntries("ScienceStudio"))
		{
			if (s != null) hasCategory.add(s);
		}
	}

	@Override
	public void read(List<String> filenames) throws Exception
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPluginName()
	{
		return "Peakaboo CDFML Reader";
	}

	@Override
	public String getPluginDescription()
	{
		return "This plugin provides support for the CDFML data format in Peakaboo";
	}


	@Override
	public void initialize()
	{
		
	}
	
	@Override
	public List<String> getFileExtensions()
	{
		return new FList<String>("xml", "cdfml");
	}




	
}
