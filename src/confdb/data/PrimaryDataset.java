package confdb.data;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * PrimaryDataset
 * --------------
 * @author Philipp Schieferdecker
 *
 * For HLT configurations; paths can be assigned to primary datasets,
 * in order to organize events for anlyzers.
 */
public class PrimaryDataset extends DatabaseEntry implements Comparable<PrimaryDataset>
{
    //
    // member data
    //
    
    /** label of the stream */
    private String label;

    /** collection of assigned paths */
    private ArrayList<Path> paths = new ArrayList<Path>();
    
    /** parent stream */
    private Stream parentStream = null;
    

    //
    // construction
    //
    
    /** standard constructor */
    public PrimaryDataset(String label)
    {
	this.label = label;
    }
    
    
    //
    // member functions
    //
    
    /** label of this stream */
    public String label() { return label; }
    
    /** get the parent stream */
    public Stream parentStream() { return parentStream; }

    /** set label of this stream */
    public void setLabel(String label) { this.label = label; }
    
    /** overload 'toString()' */
    public String toString() { return label(); }

    /** Comparable: compareTo() */
    public int compareTo(PrimaryDataset s)
    {
	return toString().compareTo(s.toString());
    }

    /** number of paths */
    public int pathCount() { return paths.size(); }

    /** retrieve i-th path */
    public Path path(int i) { return paths.get(i); }
    
    /** retrieve path by label */
    public Path path(String pathName)
    {
	for (Path p : paths) if (p.name().equals(pathName)) return p;
	return null;
    }
    
    /** retrieve iterator over paths */
    public Iterator<Path> pathIterator() { return paths.iterator(); }
    
    /** index of a certain path */
    public int indexOfPath(Path path) { return paths.indexOf(path); }

    /** insert and associate a path with this stream */
    public boolean insertPath(Path path)
    {
	if (paths.indexOf(path)>=0) {
	    System.out.println("PrimaryDataset.insertPath() WARNING: path '"+
			       path.name()+"' already associated with primary "+
			       "dataset '"+label+"'");
	    return false;
	}
	if (!path.addToDataset(this)) return false;
	paths.add(path);
	setHasChanged();
	return true;
    }
    
    /** remove a path from this stream */
    public boolean removePath(Path path)
    {
	int index = paths.indexOf(path);
	if (index<0) {
	    System.out.println("PrimaryDataset.removePath() WARNING: path '"+
			       path.name()+"' not associated with primary "+
			       "dataset '"+label+"'");
	    return false;
	}
	paths.remove(index);
	path.removeFromDataset(this);
	setHasChanged();
	return true;
    }
    
    /** add this dataset to a stream */
    public boolean addToStream(Stream stream)
    {
	if (parentStream==null) {
	    parentStream = stream;
	    setHasChanged();
	    return true;
	}
	System.out.println("PrimaryDataset::addToStream() WARNING: dataset '"+
			   label+"' already assigned to stream '"+
			   parentStream.label()+"'!");
	return false;
    }
    
    /** remove dataset from stream */
    public boolean removeFromStream(Stream stream)
    {
	if (parentStream!=stream) {
	    System.out.println("PrimaryDataset::removeFromStream() WARNING: "+
			       "dataset '"+label+"' is not assinged to stream '"+
			       stream.label()+"'!");
	    return false;
	}
	parentStream = null;
	return true;
    }
    
}
