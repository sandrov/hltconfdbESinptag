package confdb.data;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Stream
 * ------
 * @author Philipp Schieferdecker
 *
 * streams contain primary datasets, and thereby implicitely a list of
 * paths.
 */
public class Stream extends DatabaseEntry implements Comparable<Stream>
{
    //
    // member data
    //

    /** label of the stream */
    private String label;

    /** reference to parent event content */
    private EventContent parentContent = null;

    /** collection of assigned paths */
    private ArrayList<Path> paths = new ArrayList<Path>();

    /** collection of assigned paths */
    private ArrayList<PrimaryDataset> datasets=new ArrayList<PrimaryDataset>();
    
    
    //
    // construction
    //
    
    /** standard constructor */
    public Stream(String label,EventContent parentContent)
    {
	this.label = label;
	this.parentContent = parentContent;
    }
    
    
    //
    // member functions
    //
    
    /** label of this stream */
    public String label() { return label; }
    
    /** set label of this stream */
    public void setLabel(String label) { this.label = label; }
    
    /** overload 'toString()' */
    public String toString() { return label(); }
   
    /** Comparable: compareTo() */
    public int compareTo(Stream s) {return toString().compareTo(s.toString());}
    
    /** number of paths */
    public int pathCount() { return paths.size(); }
    
    /** retrieve i-th path */
    public Path path(int i) { return paths.get(i); }
    
    /** retrieve path by name */
    public Path path(String pathName)
    {
	for (Path p : paths) if (p.name().equals(pathName)) return p;
	return null;
    }
    
    /** retrieve index of a given path */
    public int indexOfPath(Path path) { return paths.indexOf(path); }
    
    /** retrieve iterator over paths */
    public Iterator<Path> pathIterator() { return paths.iterator(); }
    
    /** associate another path with this event content */
    public boolean insertPath(Path path)
    {
	if (paths.indexOf(path)>=0) return false;
	if (parentContent.indexOfPath(path)<0) return false;
	paths.add(path);
	setHasChanged();
	return true;
    }
    
    /** remove a path from this event content */
    public boolean removePath(Path path)
    {
	int index = paths.indexOf(path);
	if (index<0) return false;
	paths.remove(index);
	Iterator<PrimaryDataset> itPD = datasetIterator();
	while (itPD.hasNext()) {
	    PrimaryDataset dataset = itPD.next();
	    if (dataset.indexOfPath(path)>=0) dataset.removePath(path);
	}
	return true;
    }
    
    /** number of paths assigned to a dataset */
    public int assignedPathCount() { return listOfAssignedPaths().size(); }
    
    /** retrieve collection paths assigned to datasets */
    public ArrayList<Path> listOfAssignedPaths()
    {
	ArrayList<Path> result = new ArrayList<Path>();
	Iterator<PrimaryDataset> itPD = datasetIterator();
	while (itPD.hasNext()) {
	    Iterator<Path> itP = itPD.next().pathIterator();
	    while (itP.hasNext()) result.add(itP.next());
	}
	return result;
    }


    /** number of paths NOT assigned to any dataset */
    public int unassignedPathCount() { return listOfUnassignedPaths().size(); }
    
    /** retrieve collection of paths NOT assigned to any dataset */
    public ArrayList<Path> listOfUnassignedPaths()
    {
	ArrayList<Path> result = new ArrayList<Path>();
	ArrayList<Path> assigned = listOfAssignedPaths();
	Iterator<Path> itP = pathIterator();
	while (itP.hasNext()) {
	    Path path = itP.next();
	    if (assigned.indexOf(path)<0) result.add(path);
	}
	return result;
    }


    /** number of primary datasets */
    public int datasetCount() { return datasets.size(); }
    
    /** retrieve i-th primary dataset */
    public PrimaryDataset dataset(int i) { return datasets.get(i); }

    /** retrieve primary dataset by label */
    public PrimaryDataset dataset(String datasetLabel)
    {
	for (PrimaryDataset pd : datasets)
	    if (pd.label().equals(datasetLabel)) return pd;
	return null;
    }

    /** retrieve primary dataset iterator */
    public Iterator<PrimaryDataset> datasetIterator()
    {
	return datasets.iterator();
    }

    /** index of a given primary dataset */
    public int indexOfDataset(PrimaryDataset ds) {return datasets.indexOf(ds);}
    
    /** insert and associate a primary dataset with this stream */
    public boolean insertDataset(String datasetLabel)
    {
	for (PrimaryDataset pd : datasets)
	    if (pd.label().equals(datasetLabel)) return false;
	datasets.add(new PrimaryDataset(datasetLabel,this));
	setHasChanged();
	return true;
    }
    
    /** remove a dataset from this stream */
    public boolean removeDataset(PrimaryDataset dataset)
    {
	int index = datasets.indexOf(dataset);
	if (index<0) return false;
	datasets.remove(index);
	setHasChanged();
	return true;
    }
    
}
