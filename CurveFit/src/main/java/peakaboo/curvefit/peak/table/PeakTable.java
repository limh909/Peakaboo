package peakaboo.curvefit.peak.table;

import java.util.List;
import java.util.stream.Collectors;

import peakaboo.curvefit.peak.transition.TransitionSeries;
import peakaboo.curvefit.peak.transition.TransitionSeriesType;

public interface PeakTable {

	static final DelegatingPeakTable SYSTEM = new DelegatingPeakTable(
			new CombinedPeakTable(
					new XrayLibPeakTable(), 
					new KrausPeakTable()
			)
	);
	
	default TransitionSeries get(Element e, TransitionSeriesType tst) {
		List<TransitionSeries> tss = getAll()
				.stream()
				.filter(ts -> (ts.element == e) && (ts.type == tst))
				.collect(Collectors.toList());
		if (tss.size() == 0) return null;
		if (tss.size() > 1) {
			throw new RuntimeException("Found more than one TransitionSeries for the given Element and TransitionSeriesType");
		}
		return tss.get(0);
	}
	
	List<TransitionSeries> getAll();
	
	default List<TransitionSeries> getForElement(Element e) {
		return getAll()
				.stream()
				.filter(ts -> (ts.element == e))
				.collect(Collectors.toList());
	}
	
	
}
