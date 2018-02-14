package peakaboo.datasource.model.internal;

import java.io.File;
import java.util.Collections;
import java.util.List;

import peakaboo.datasource.model.DataSource;
import peakaboo.datasource.model.components.datasize.DataSize;
import peakaboo.datasource.model.components.fileformat.FileFormat;
import peakaboo.datasource.model.components.fileformat.FileFormatCompatibility;
import peakaboo.datasource.model.components.interaction.Interaction;
import peakaboo.datasource.model.components.metadata.Metadata;
import peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import peakaboo.datasource.model.components.scandata.ScanData;
import peakaboo.datasource.model.components.scandata.SimpleScanData;

/**
 * @author maxweld
 * 
 */
public class EmptyDataSource implements DataSource, FileFormat {

	// Data Source //
	
	@Override
	public Metadata getMetadata() {
		return null;
	}

	@Override
	public FileFormatCompatibility compatibility(File filename) {
		return FileFormatCompatibility.NO;
	}

	@Override
	public FileFormatCompatibility compatibility(List<File> filenames) {
		return FileFormatCompatibility.NO;
	}

	@Override
	public List<String> getFileExtensions() {
		return Collections.emptyList();
	}

	@Override
	public void read(File file) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void read(List<File> files) throws Exception {
		throw new UnsupportedOperationException();
	}


	
	// DSScanData //
	


	@Override
	public String getFormatName() {
		return "Empty Format";
	}

	@Override
	public String getFormatDescription() {
		return "Empty Format Description";
	}

	
	@Override
	public DataSize getDataSize() {
		return null;
	}

	@Override
	public FileFormat getFileFormat() {
		return this;
	}

	@Override
	public void setInteraction(Interaction interaction) {
		
	}
	
	@Override
	public Interaction getInteraction() {
		return null;
	}

	@Override
	public ScanData getScanData() {
		return new SimpleScanData("");
	}

	@Override
	public PhysicalSize getPhysicalSize() {
		return null;
	}



	
}