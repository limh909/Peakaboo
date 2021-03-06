package peakaboo.controller.plotter.data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SavedDataSession {

	public List<Integer> discards = new ArrayList<>();
	public List<String> files = new ArrayList<>();

	
	public SavedDataSession storeFrom(DataController controller) {
		this.discards = controller.getDiscards().list();
		this.files = controller.getDataPaths().stream().map(p -> p.toString()).collect(Collectors.toList());
		return this;
	}
	
	public SavedDataSession loadInto(DataController controller) {
		controller.getDiscards().clear();
		for (int i : discards) {
			controller.getDiscards().discard(i);
		}
		controller.setDataPaths(this.filesAsDataPaths());
		return this;
	}
	
	public List<Path> filesAsDataPaths() {
		return this.files.stream().map(Paths::get).collect(Collectors.toList());
	}
	
}
