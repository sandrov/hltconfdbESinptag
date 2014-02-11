package confdb.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.python.core.*;
import org.python.util.PythonInterpreter;

import java.io.*;
import confdb.data.*;



/**
 * JPythonParser
 * ------------
 * @author Philipp Schieferdecker
 *
 * Parse a flat *.py configuration file using Jython
 *
 */

@SuppressWarnings("unchecked")
public class JPythonParser
{
    //
    // member data
    //
    
    /** the software release w.r.t. which the configuration is created */
    private SoftwareRelease release = null;
    private String cmsswTag = null;
    
    /** Create an instance of the PythonInterpreter */
    private PythonInterpreter pythonInterpreter = null;
    
    /** Python Configuration object */
    private PyObject process = null;

    /** ConfDB Configuration object */
    private Configuration configuration = null;
    
    /** set with all problem components */
    private HashSet<String> problemModules = new HashSet<String>();
    
    /** set with all problem parameter names */
    private HashSet<String> problemParameters = new HashSet<String>();
    
    /** output stream for problems.txt */
    private PrintWriter  problemStream = null;
    private StringBuffer problemBuffer = new StringBuffer();
    
    private String command = "";
    
    private boolean ignorePrescaleService = false;
    
    private ArrayList<String> error;
    private ArrayList<String> info;
    private ArrayList<String> warning;

    
    //
    // construction
    //

    /** standard constructor */
    public JPythonParser(SoftwareRelease release)
    {
	this.release = release;
	cmsswTag = release.releaseTag();
	int index=cmsswTag.indexOf("_HLT");
	if (index>=0) cmsswTag = cmsswTag.substring(0,index);
	
    	error = new ArrayList<String>();
    	info = new ArrayList<String>();
    	warning = new ArrayList<String>();
    }
    
    public enum msg {
    	  err,	// error messages
    	  war,	// warning messages
    	  inf;	// info messages
    }
    
    public void alert(msg typeMsg, String message) {
    	switch(typeMsg) {
    	case err:
    		error.add(message);
    		System.err.println("[ERROR]" + message);
    		break;
    	case war:
    		warning.add(message);
    		System.out.println("[WARNING]" + message);
    		break;
    	case inf:
    		info.add(message);
    		System.out.println("[INFO]" + message);
    		break;
    	}
    }
    
    static private <T> T convert(PyObject object, Class<T> c) {
        T value = (T) object.__tojava__(c);
        return value;
    }

    //
    // member functions
    //
    
    /** get problems as string */
    //public String problemsAsString() { return problemBuffer.toString(); }
    public String problemsAsString() { 
    	String message ="";
    	
    	for(int i = 0; i < error.size(); 	i ++)	message+="[ERROR]"	+error.get(i)	+"\n";
    	for(int i = 0; i < warning.size(); 	i ++) 	message+="[WARNING]"+warning.get(i)	+"\n";
    	for(int i = 0; i < info.size(); 	i ++)	message+="[INFO]"	+info.get(i)	+"\n";
    	
    	return message;
    }

    /** exec py cmd */
    public void pyCmd(String pycmd) {
	//System.out.println("pycmd="+pycmd);
	pythonInterpreter.exec(pycmd);
    }

    /** parse a file */
    public void parseCompileFile(String fileName) throws JParserException
    {

	// path to file without trailing /
	String path = new String(fileName);
	path=path.substring(0,path.lastIndexOf("/")+1);
	if (path.lastIndexOf("/")>=0) path=path.substring(0,path.lastIndexOf("/"));

	// name of file including extension
	String name = new String(fileName);
	name=name.substring(name.lastIndexOf("/")+1);

	// name of file excluding extension
	String leaf = new String(name);
	if (leaf.indexOf(".py")>=0) leaf=leaf.substring(0,leaf.indexOf(".py"));

	System.out.println("JPythonParser: ");
	System.out.println("  ReleaseTag: "+release.releaseTag());
	System.out.println("  CMSSW  Tag: "+cmsswTag);
	System.out.println("  Full File : "+fileName);
	System.out.println("  File Path : "+path);
	System.out.println("  File Name : "+name);
	System.out.println("  Leaf Name : "+leaf);

	String pyCmd = null;

		try {
			
			
	
		    pythonInterpreter = new PythonInterpreter();
	
		    // need to set up search path
		    pyCmd("import sys");
		    pyCmd("sys.path.append('"+path+"')");
		    pyCmd("print sys.path");

		    pyCmd("import sys");
		    pyCmd("sys.path.append('python')");   // add the CMSSW Python path
		    pyCmd("import pycimport");            // load precompiled .pyc files
		    /////////////////////////////////////////////////////////
	
		    // now import process object from (uncompiled) py file
		    pyCmd("from "+leaf+" import process");
		    
		    pyCmd("print process");
	
		    // get process object
		    process = pythonInterpreter.get("process");
		    System.out.println("Process object found: "+(process!=null));
		    
		    // get its process name
		    String processName = convert(process.invoke("name_"),String.class);
		    System.out.println("Process  name  found: "+processName);
	
		    
		    // configinfo of new configuration
		    ConfigInfo configInfo = new ConfigInfo(name,null,-1,0,"","",
				       release.releaseTag(),processName,
				       "parsed from "+fileName);
	
		    // new configuration
		    configuration = new Configuration();
		    configuration.initialize(configInfo,release);
		    
		    
		    
		}
		catch (Exception e) {
		    e.printStackTrace();
		}

    }
    
    /** parse a file */
    public void parseFileBatchMode(String fileName, boolean ignorePrescales) throws JParserException
    {

	// path to file without trailing /
	String path = new String(fileName);
	path=path.substring(0,path.lastIndexOf("/")+1);
	if (path.lastIndexOf("/")>=0) path=path.substring(0,path.lastIndexOf("/"));

	// name of file including extension
	String name = new String(fileName);
	name=name.substring(name.lastIndexOf("/")+1);

	// name of file excluding extension
	String leaf = new String(name);
	if (leaf.indexOf(".py")>=0) leaf=leaf.substring(0,leaf.indexOf(".py"));
	
	ignorePrescaleService = ignorePrescales;

	System.out.println("JPythonParser: ");
	System.out.println("  ReleaseTag: "+release.releaseTag());
	System.out.println("  CMSSW  Tag: "+cmsswTag);
	System.out.println("  Full File : "+fileName);
	System.out.println("  File Path : "+path);
	System.out.println("  File Name : "+name);
	System.out.println("  Leaf Name : "+leaf);

	String pyCmd = null;

		try {
			
			
	
		    pythonInterpreter = new PythonInterpreter();
	
		    // need to set up search path
		    pyCmd("import sys");
		    pyCmd("sys.path.append('"+path+"')");
		    pyCmd("import sys");
		    pyCmd("sys.path.append('python')");   // add the CMSSW Python path
		    pyCmd("import pycimport");            // load precompiled .pyc files
		    /////////////////////////////////////////////////////////

		    executePyLineByLine(fileName);
	
		    // get process object
		    process = pythonInterpreter.get("process");
		    alert(msg.inf, " Process object found: "+(process!=null));
		    
			    // get its process name
			    String processName = convert(process.invoke("name_"),String.class);
			    alert(msg.inf, " Process  name  found: "+processName);
		
			    
			    // configinfo of new configuration
			    ConfigInfo configInfo = new ConfigInfo(name,null,-1,0,"","",
					       release.releaseTag(),processName,
					       "parsed from "+fileName);
		
			    // new configuration
			    configuration = new Configuration();
			    configuration.initialize(configInfo,release);
		}
		catch (Exception e) {
		    e.printStackTrace();
		}

    }
    
    /** turn process object into configuration */
    public Configuration createConfiguration() throws JParserException
    {


    	if (release==null || process==null ) return null;

	//////////////////////////////////////////////////////////
    //Parser parser = new Parser();
    //Process Pyprocess = parser.parseProcess(process);
    //Pyprocess.dump();
	//////////////////////////////////////////////////////////
	
	
	// add primary data source (edsource)
	parseEDSources(process);
	// add essources
	parseESSources(process);
	// add esmodules
	parseESModules(process);
	// set preferred essources / esmodules
	//?
	// add services
	parseServices(process);
	
	// MODULES:
	// add producers:
	parseProducerModules(process);
	// add filters:
	parseFilterModules(process);
	// add analyzers:
	parseAnalyzerModules(process);

	
	
	// add sequences
	parseSequencesFromPython(process);
	// add paths
    parsePathsFromPython(process);
    
	// add outputmodules + Streams + EventContents
	parseOutputModules(process);
	
	// add global psets + Streams + Datasets
	parsePSets(process); // this need to go after Paths to properly link the Datasets.
	
	// add endpaths
    parseEndPathsFromPython(process);
    
	return configuration;
    }
    
    
    
    // parse Services:
    private void parseServices(PyObject process) {
        PyDictionary producers = (PyDictionary) process.__getattr__("_Process__services");
        if(validPyObject(producers)) 
        	parseServiceMap(producers);
    }
    
    // parse EDSources:
    // There is only one EDSource.
    private void parseEDSources(PyObject process) {
    	PyObject source = (PyObject) process.__getattr__("_Process__source");
        if(validPyObject(source)) 
        	parseEDSource(source);
    }
    
    // parse Psets:
    private void parsePSets(PyObject process) {
        PyDictionary producers = (PyDictionary) process.__getattr__("_Process__psets");
        if(validPyObject(producers)) 
        	parsePSetMap(producers);
    }
    
    
    // parse Modules of type ESModules
    private void parseESModules(PyObject process) {
        PyDictionary producers = (PyDictionary) process.__getattr__("_Process__esproducers");
        if(validPyObject(producers)) 
        	parseESModuleMap(producers);
    }
    
    // parse ESSources:
    private void parseESSources(PyObject process) {
        PyDictionary producers = (PyDictionary) process.__getattr__("_Process__essources");
        if(validPyObject(producers)) 
        	parseESSourceMap(producers);
    }
    
    // parse Modules of type Producer:
    private void parseProducerModules(PyObject process) {
        PyDictionary producers = (PyDictionary) process.__getattr__("_Process__producers");
        if(validPyObject(producers)) 
        	parseModuleMap(producers);
    }
    
    // parse Modules of type filters:
    private void parseFilterModules(PyObject process) {
        PyDictionary producers = (PyDictionary) process.__getattr__("_Process__filters");
        if(validPyObject(producers)) 
        	parseModuleMap(producers);
    }
    
    // parse Modules of type analyzers:
    private void parseAnalyzerModules(PyObject process) {
        PyDictionary producers = (PyDictionary) process.__getattr__("_Process__analyzers");
        if(validPyObject(producers)) 
        	parseModuleMap(producers);
    }
    
    // parse Modules of type outputmodules:
    private void parseOutputModules(PyObject process) {
        PyDictionary producers = (PyDictionary) process.__getattr__("_Process__outputmodules");
        if(validPyObject(producers)) 
        	parseModuleMap(producers);
    }
    
        
    // parse Services dictionary
    private void parseServiceMap(PyDictionary pydict) {
    	alert(msg.inf, " Services (" + pydict.size() + ")");
        for (Object o : pydict.entrySet()) {
            PyDictionary.Entry<String, PyObject> moduleObject = (PyDictionary.Entry<String, PyObject>) o;
            parseService(moduleObject.getValue());
        }
    }
    
    // parse EDSource dictionary
    private void parseEDSource(PyObject pydict) {
    	
        String type  = getType(pydict);
        String label = getLabel(pydict);
        
        String moduleClass = convert(pydict.invoke("type_"), String.class);
        
        //ModuleInstance module = configuration.insertModule(moduleClass,label);
        EDSourceInstance module = configuration.insertEDSource(moduleClass);
        //TODO: Update module with file values?
        PyDictionary parameterContainerObject = (PyDictionary) pydict.invoke("parameters_");
        updateModuleParameters(parameterContainerObject, module);
        
    }
    
    // parse modules dictionary
    private void parseESModuleMap(PyDictionary pydict) {
    	alert(msg.inf, " ESModules (" + pydict.size() + ")");
        for (Object o : pydict.entrySet()) {
            PyDictionary.Entry<String, PyObject> moduleObject = (PyDictionary.Entry<String, PyObject>) o;
            parseESModule(moduleObject.getValue());
        }
    }
    
    
    // parse modules dictionary
    private void parseESSourceMap(PyDictionary pydict) {
    	alert(msg.inf, " ESSources (" + pydict.size() + ")");
        for (Object o : pydict.entrySet()) {
            PyDictionary.Entry<String, PyObject> moduleObject = (PyDictionary.Entry<String, PyObject>) o;
            parseESSource(moduleObject.getValue());
        }
    }
    
    // parse modules dictionary
    private void parseModuleMap(PyDictionary pydict) {
    	alert(msg.inf, " Modules (" + pydict.size() + ")");
        for (Object o : pydict.entrySet()) {
            PyDictionary.Entry<String, PyObject> moduleObject = (PyDictionary.Entry<String, PyObject>) o;
            parseModule(moduleObject.getValue());
        }
    }
    
    // parse PSets dictionary
    private void parsePSetMap(PyDictionary pydict) {
    	alert(msg.inf, " PSets (" + pydict.size() + ")");
        for (Object o : pydict.entrySet()) {
            PyDictionary.Entry<String, PyObject> moduleObject = (PyDictionary.Entry<String, PyObject>) o;
            
            parsePSet(moduleObject.getValue());
        }
    }
    
    // Parse one single module
    // NOTE: This also parse the OUTPUT MODULES.
    // For output modules there are two ways to get them.
    // 1) When parsing Streams we must create EventContents and OutputModules.
    // 2) The streams coming from the python file could not be updated, and there
    //    is not such eventContents structure in the file.
    // 3) What really contains the information are the Output Modules.
    //    An OutputModule is the implementation of the stream.
    //    They have two main parameters:
    //		- outputCommand: definition of the EventContent.
    //		- SelectEvent: definition of the Stream.
    // TODO: What to do if we generate extra things when parsing streams?
    private boolean parseModule(PyObject moduleObject) {
    	
        String type  = getType(moduleObject);
        String label = getLabel(moduleObject);
        
        if(type == "OutputModule") {
        	OutputModule module = configuration.output(label);
        	
        	if(module != null) {
            	PyDictionary parameterContainerObject = (PyDictionary) moduleObject.invoke("parameters_");
            	updateOutputModuleParameters(parameterContainerObject, module);            		
        	} else {
        		// System.err.println("ERROR: Output module not found. Cannot be updated.");
        		// NOTE: In principal this is not an error. This must generate the real Streams/EventContents/OutputModules.
        		
        		// we will generate the stream, EV and OM, from the label.
        		
    	        String streamName = label;
    	        // fixing the OM name:
    	        String prefix = "hltOutput"; // Used prefix for OutputModules.
    	        if(streamName.startsWith(prefix)) streamName = streamName.substring(prefix.length());
    	        
    	        String contentName = "hltEventContent" + streamName; // convention.
    	        EventContent content = configuration.insertContent(contentName);
    	        Stream stream = new Stream(streamName, content);
    	        stream = content.insertStream(streamName);
    	        
    	        // Recursively calling this function MUST Update the recently created OutputModule.
    	        parseModule(moduleObject);
        	}
        	
        } else {
            String moduleClass = convert(moduleObject.invoke("type_"), String.class);
            ModuleInstance module = configuration.insertModule(moduleClass,label);
            //TODO: Update module with file values?
            PyDictionary parameterContainerObject = (PyDictionary) moduleObject.invoke("parameters_");
            updateModuleParameters(parameterContainerObject, module);        	
        }
        
        return true;
    }
    
    // Parse one single Dataset
    private boolean parsePSet(PyObject psetObject) {
        String type  = getType(psetObject);
        String label = getLabel(psetObject);
        
	    // IGNORE PSETs: HLTConfigVersion, maxEvents, options
	    if(	(label.compareTo("HLTConfigVersion") == 0)	||
	    	(label.compareTo("maxEvents") == 0) 		||
	    	(label.compareTo("options") == 0) 			) {
	    	alert(msg.inf, " parsePset: IGNORING! type="+type+", label='"+label+"'");	
	    	return false;
	    }
	    
	    // Inserting streams
	    if(label.compareTo("streams") == 0) {
	    	// The python representation of a stream is a PSET.
	    	// The parameters of the PSET named 'streams' are the name of the streams.
		    return parseStreams(psetObject);

	    }
	    
	    // UPDATE datasets.
	    if(label.compareTo("datasets") == 0) {
	    	return parseDatasets(psetObject);
	    }	 
	    
	    
	    // Otherwise, Insert PSet to configuration.
        Boolean     tracked = convert(psetObject.invoke("isTracked"), Boolean.class);
	    PSetParameter pset = (PSetParameter)ParameterFactory.create("PSet",label,"",tracked);
	    //
        // Add parameters to PSET.
	    ArrayList<confdb.data.Parameter> params = parsePSetParameters(psetObject);
	    for(int It = 0; It < params.size(); It++)
	    	pset.addParameter(params.get(It));
	    configuration.insertPSet(pset);	    	

        return true;
    }
    
    // this will be used to parse PSets inside modules
    private PSetParameter parsePSetParameter(PyObject psetObject, String name) {
        String type  = getType(psetObject);
        String label = getLabel(psetObject);
        Boolean     tracked = convert(psetObject.invoke("isTracked"), Boolean.class);
        
        label = name;
        if(label == null) label = "";
        
	    PSetParameter pset = (PSetParameter)ParameterFactory.create("PSet",label,"",tracked);
	    
	    //
        // Add parameters to PSET.
	    ArrayList<confdb.data.Parameter> params = parsePSetParameters(psetObject);
	    for(int It = 0; It < params.size(); It++)
	    	pset.addParameter(params.get(It));
	    
	    return pset;
    }
    
    
    // Parse one single ESModule
    private boolean parseESModule(PyObject moduleObject) {
    	
        String type  = getType(moduleObject);
        String label = getLabel(moduleObject);
        
        String moduleClass = convert(moduleObject.invoke("type_"), String.class);
        
        //ModuleInstance module = configuration.insertModule(moduleClass,label);
        ESModuleInstance module = configuration.insertESModule(configuration.esmoduleCount(), moduleClass, label);
        //TODO: Update module with file values?
        PyDictionary parameterContainerObject = (PyDictionary) moduleObject.invoke("parameters_");
        updateModuleParameters(parameterContainerObject, module);
        
        return true;
    }
    
    // Parse one single Service
    private boolean parseService(PyObject moduleObject) {
    	
        String type  = getType(moduleObject);
        String label = getLabel(moduleObject);
        
        String moduleClass = convert(moduleObject.invoke("type_"), String.class);
        
        //ModuleInstance module = configuration.insertModule(moduleClass,label);
        ServiceInstance module = configuration.insertService(configuration.serviceCount(), moduleClass);
        //TODO: Update module with file values?
        PyDictionary parameterContainerObject = (PyDictionary) moduleObject.invoke("parameters_");
        updateModuleParameters(parameterContainerObject, module);
        
        return true;
    }
    
    // Parse one single ESSource
    private boolean parseESSource(PyObject moduleObject) {
    	
        String type  = getType(moduleObject);
        String label = getLabel(moduleObject);
        
        String moduleClass = convert(moduleObject.invoke("type_"), String.class);
        
        //ModuleInstance module = configuration.insertModule(moduleClass,label);
        
        ESSourceInstance module = configuration.insertESSource(configuration.essourceCount(), moduleClass, label);
        //TODO: Update module with file values?
        PyDictionary parameterContainerObject = (PyDictionary) moduleObject.invoke("parameters_");
        updateModuleParameters(parameterContainerObject, module);
        
        return true;
    }
    
    // Parse Sequences:
    private void parseSequencesFromPython(PyObject pyprocess){
        PyDictionary sequences = (PyDictionary) pyprocess.__getattr__("_Process__sequences");
        if(validPyObject(sequences)) 
        	parseSequenceMap(sequences);
    }
    
    // Parse Paths:
    private void parsePathsFromPython(PyObject pyprocess){
        PyDictionary paths = (PyDictionary) pyprocess.__getattr__("_Process__paths");
        
        if(validPyObject(paths))
        	parsePathMap(paths);
        
    }
    
    // Parse Paths:
    private void parseEndPathsFromPython(PyObject pyprocess){
        PyDictionary paths = (PyDictionary) pyprocess.__getattr__("_Process__endpaths");
        
        if(validPyObject(paths))
        	parseEndPathMap(paths);
        
    }
    
    private boolean parsePathMap(PyDictionary pydict) {
    	alert(msg.inf, " Paths (" + pydict.size() + ")");
    	PyList keys = (PyList) pydict.invoke("keys");
        for (Object key : keys) {
        	String pathName = (String) key;
        	//System.out.println("Path: " + key);
        	Path path = configuration.insertPath(configuration.pathCount(), pathName);
        	PyObject value = pydict.__getitem__(new PyString((String) key));
        	parsePath(value);
        }
        
        return true;
    }
    
    private boolean parseEndPathMap(PyDictionary pydict) {
    	alert(msg.inf, " EndPaths (" + pydict.size() + ")");
    	PyList keys = (PyList) pydict.invoke("keys");
        for (Object key : keys) {
        	String pathName = (String) key;
        	//System.out.println("Path: " + key);
        	Path path = configuration.insertPath(configuration.pathCount(), pathName);
        	path.setAsEndPath(true);
        	PyObject value = pydict.__getitem__(new PyString((String) key));
        	parsePath(value);
        }
        
        return true;
    }
    
    // parse one single path, creating references to its sequences.
    private void parsePath(PyObject object) {
    	String type  = getType(object);
        String label = getLabel(object);
        //System.out.println("[parseSequence] " + type + " " + label);
        confdb.data.Path insertedPath = null;
        
        if(confdbTypes.path.is(type)) {
        	insertedPath = configuration.path(label);
        	
        	if(insertedPath == null)
        		alert(msg.err, "[parsePath] path does not exist!" + label);

        	
    		// Content:
        	PyObject pathContent = object.__getattr__(new PyString("_seq"));
        	parseReferenceContainerContent(pathContent, insertedPath);
        	
        } else if(confdbTypes.endPath.is(type)){
        	insertedPath = configuration.path(label);
        	
        	if(insertedPath == null)
        		alert(msg.err, "[parsePath] path does not exist!" + label);

    		// Content:
        	PyObject pathContent = object.__getattr__(new PyString("_seq"));
        	parseReferenceContainerContent(pathContent, insertedPath);
        	
        } else alert(msg.err, "[parsePath] type Unknow " + type);
    }
    
    

    private void updateModuleParameters(PyDictionary parameterContainer, 
    																confdb.data.Instance module) {
    	
        for (Object parameterObject : parameterContainer.entrySet()) {
            PyDictionary.Entry<String, PyObject> entry = (PyDictionary.Entry<String, PyObject>) parameterObject;
            parseParameter(entry.getKey(), entry.getValue(), module);
        }
        
    }
    
    // Basically the same as updateModuleParameters but an OutputModule parameter.
    private void updateOutputModuleParameters(PyDictionary parameterContainer, 
			confdb.data.OutputModule module) {

		for (Object parameterObject : parameterContainer.entrySet()) {
			PyDictionary.Entry<String, PyObject> entry = (PyDictionary.Entry<String, PyObject>) parameterObject;
			parseOutputModuleParameter(entry.getKey(), entry.getValue(), module);
		}
	
	}    
    
    
    
    // parse recursively a PSet object with parameters.
    private ArrayList<confdb.data.Parameter> parsePSetParameters(PyObject parameterContainerObject) {
    	ArrayList<confdb.data.Parameter> params = new ArrayList<confdb.data.Parameter>();
        //String type  = getType(parameterContainerObject);
        //String label = getLabel(parameterContainerObject);
    	PyDictionary parameterContainer = (PyDictionary) parameterContainerObject.invoke("parameters_");
			for (Object parameterObject : parameterContainer.entrySet()) {
				PyDictionary.Entry<String, PyObject> entry = (PyDictionary.Entry<String, PyObject>) parameterObject;
				confdb.data.Parameter param = __parseParameter(entry.getValue(), entry.getKey());
				
				params.add(param);
			}
			return params;
    }
    
    // NOTE: Streams come as PSets from python.
    // Streams are inserted previously using the OutputModule definition.
    // In principal, the streams definition of a Python file could be ignored.
    // Instead, we try to recreate the PrimaryDataset when possible.
    // BUT DONT ADD ANY PATH TO THE STREAM.
    private boolean parseStreams(PyObject parameterContainerObject) {
        //String type  = getType(parameterContainerObject);
        //String label = getLabel(parameterContainerObject);
    	PyDictionary parameterContainer = (PyDictionary) parameterContainerObject.invoke("parameters_");
			for (Object parameterObject : parameterContainer.entrySet()) {
				PyDictionary.Entry<String, PyObject> entry = (PyDictionary.Entry<String, PyObject>) parameterObject;

				confdb.data.Parameter param = __parseParameter(entry.getValue(), entry.getKey());
		        String streamName = param.name();
		        Stream stream = configuration.stream(streamName);
		        
		        if(stream != null) {
		        	// This is ok. now we try to split it in Datasets.
					// Each stream has a list of Dataset associated.
					ArrayList<String> ListOfDatasets = __getArrayListFromPyObject(entry.getValue());
						for(int ds = 0; ds < ListOfDatasets.size(); ds++) {
				    		PrimaryDataset newDataSet = stream.insertDataset(ListOfDatasets.get(ds));
						}
		        	
		        } else {
		        	// Create Stream coming from Stream definition, not from OutputModule definition.
		        	alert(msg.war, " Creating Stream from Stream definition "+ streamName);
			        
			        String contentName = "hltEventContent" + streamName; // convention.
			        EventContent content = configuration.insertContent(contentName);
			        Stream ExtraStream = new Stream(streamName, content);
			        ExtraStream = content.insertStream(streamName);
	
					// Each stream has a list of Dataset associated.
					ArrayList<String> ListOfDatasets = __getArrayListFromPyObject(entry.getValue());
					for(int ds = 0; ds < ListOfDatasets.size(); ds++) {
			    		PrimaryDataset newDataSet = ExtraStream.insertDataset(ListOfDatasets.get(ds));
					}
				}
	        }
			
			return true;
    }
    
    
    // This function organise Streams from path into datasets when possible.
    // No new paths are added to the stream. Only organized.
    private boolean parseDatasets(PyObject parameterContainerObject) {
    	ArrayList<confdb.data.Parameter> params = new ArrayList<confdb.data.Parameter>();
        String type  = getType(parameterContainerObject);
        String label = getLabel(parameterContainerObject);
        //System.out.println("PSET parameter type="+type+", label="+label);
    	PyDictionary parameterContainer = (PyDictionary) parameterContainerObject.invoke("parameters_");
			for (Object parameterObject : parameterContainer.entrySet()) {
			PyDictionary.Entry<String, PyObject> entry = (PyDictionary.Entry<String, PyObject>) parameterObject;
			
			
			confdb.data.Parameter param = __parseParameter(entry.getValue(), entry.getKey());
			
	        String datasetName = param.name();
	        
	        PrimaryDataset dset = configuration.dataset(datasetName);
	        
	        if(dset == null) {
	        	alert(msg.err, "[parseDatasets]: Dataset not found. Cannot be updated!");
	        	return false;
	        }
	        
			// Each stream has a list of Dataset associated.
			ArrayList<String> ListOfPaths = __getArrayListFromPyObject(entry.getValue());
			
				for(int path = 0; path < ListOfPaths.size(); path++) {
					// Path must be previously inserted into the stream from the OUTPUT MODULE definition.
					Stream stream = dset.parentStream();
					Path path_ = stream.path(ListOfPaths.get(path));
					
					if(path_ == null) {
						alert(msg.war, "[parseDatasets]: Path " + ListOfPaths.get(path) + "not found in the stream. Cannot be associated to Dataset --> " + ListOfPaths.get(path));
					} else {
						// ONLY existing paths in the stream.
						dset.insertPath(path_);
					}
				}
			}
			
			return true;
    }
    
    
    // Parse parameter types
    private confdb.data.Parameter __parseParameter(PyObject parameterObject, String name) {
        String      type    = parameterObject.getType().getName();
        Boolean     tracked = convert(parameterObject.invoke("isTracked"), Boolean.class);
        PyObject    value   = parameterObject.invoke("value");
        
        String string_value = value.toString();
        if(string_value == "") string_value = "\"\""; // equivalent to cms.string("");
       
    	string_value = cleanBrackets(string_value); //Needed!
        
    	if(type == "PSet") {
    		return parsePSetParameter(parameterObject, name); // Start recursion.
    	}  else if ("uint32" == type || "int32" == type || "uint64" == type || "int64" == type) {
    		string_value = fixLongNumber(string_value);
    	}
    	
        return ParameterFactory.create(type, name, string_value, tracked);
    }
    
    // This is mainly used to get the list of paths when parsing datasets.
    private ArrayList<String> __getArrayListFromPyObject(PyObject VStringParam) {
    	ArrayList<String> VString = new ArrayList<String>();
    	PyList parameterList= (PyList) VStringParam;
    	for(int i=0; i < parameterList.size(); i++) {
    		VString.add(parameterList.get(i).toString());
    	}
    	return VString;
    }
   
    
    
    // Parse parameter types    
    /* Note:
     * all integer types are extracted using the generic Number class;
     * this will automatically use BigInteger for uint64_t numbers, etc.
     */

    /* Note:
     * Care should be taken to keep hex numbers in hex notation
     */

    /* Note:
     * an InputTag could either be split in
     *   'label', 'instance', 'pyprocess'
     * or left as a single string
     *   'label:instance:pyprocess'
     */

    /* Note:
     * these CMS types are not yet supported:
     *   FileInPath
     *   EventID
     *   VEventID
     *   LuminosityBlockID
     *   VLuminosityBlockID
     *   EventRange
     *   VEventRange
     *   LuminosityBlockRange
     *   VLuminosityBlockRange
     */
    private void parseParameter(String parameterName, PyObject parameterObject, Instance module) {
        String      type    = parameterObject.getType().getName();
        Boolean     tracked = convert(parameterObject.invoke("isTracked"), Boolean.class);
        PyObject    value   = parameterObject.invoke("value");

        
        // NOTE: This only can be a toplevel parameter:
   	    Parameter[] p = module.findParameters(parameterName, type);
   	    Parameter ParameterToBeUpdated = null;
   	    if(p == null) alert(msg.err, "[parseParameter] "+type+" parameter '" + parameterName + "' not found! at Module '" + module.name() +"'");
   	    else {
    		for(int i = 0; i < p.length; i++) {
    			if(p[i].fullName().compareTo(parameterName) == 0) {
    				ParameterToBeUpdated = p[i];// Parameter to update.
    			}
    		}
    		
    		if(ParameterToBeUpdated == null){
    			alert(msg.err, "[parseParameter] "+ type +" parameter '" + parameterName + "' not found! at Module '" + module.name() +"'");    	    	
    		}
   	    }

   	    
        if ("bool" == type) {
            //Boolean v = convert(value, Boolean.class);
            
            module.updateParameter(parameterName,type,value.toString());
            module.findParameter(parameterName).setTracked(tracked);
        } else if ("vbool" == type) {
            module.updateParameter(parameterName,type,value.toString());
            module.findParameter(parameterName).setTracked(tracked);
            
        } else if ("uint32" == type || "int32" == type || "uint64" == type || "int64" == type) {
            if(type == "uint64") {
            	// Get Hexadecimal format by default.
            	String LHexStringValue = value.__hex__().toString();
            	String svalue = value.toString();
        		svalue = cleanBrackets(LHexStringValue);
        		svalue = fixLongNumber(svalue);
            	
            	module.updateParameter(parameterName,type,svalue);
            } else {
            	// Default case:
            	//System.out.println("CASTING NUMBER TO STRING!" + value.__str__().toString());
            	
            	String svalue = value.toString();
        		svalue = cleanBrackets(svalue);
        		svalue = fixLongNumber(svalue);
            	module.updateParameter(parameterName,type,svalue);
            }
            module.findParameter(parameterName).setTracked(tracked);
            
        } else if ("vuint32" == type || "vint32" == type || "vuint64" == type || "vint64" == type) {
        	
            // NOTE: No need to convert or to cast value by value.
        	// It can be done at once, but erasing the vector brackets.
            confdb.data.Parameter param = module.findParameter(parameterName);
            if(param != null) {
            	if(param instanceof VectorParameter) {
            		VectorParameter param_sp = (VectorParameter) param;
            		
                	String svalue = value.toString();
            		svalue = cleanBrackets(svalue);
                    module.updateParameter(parameterName,type,svalue);
                    module.findParameter(parameterName).setTracked(tracked);
            	} else alert(msg.err, "[parseParameter] parameter not VectorParameter! " + parameterName);
            } else alert(msg.err, "[parseParameter] "+type+" parameter '" + parameterName + "' not found! at Module '" + module.name() +"'");
            
        } else if ("double" == type) {
        	String svalue = value.toString();
    		//svalue = cleanBrackets(svalue);
            
            module.updateParameter(parameterName,type,svalue);
            module.findParameter(parameterName).setTracked(tracked);
        } else if ("vdouble" == type) {
            
            confdb.data.Parameter param = module.findParameter(parameterName);
            if(param != null) {
            	if(param instanceof VectorParameter) {
            		VectorParameter param_sp = (VectorParameter) param;
                	String svalue = value.toString();
            		svalue = cleanBrackets(svalue);
            		param_sp.setValue(svalue);
            		
                    module.updateParameter(parameterName,type,svalue);
                    module.findParameter(parameterName).setTracked(tracked);
            	} else alert(msg.err, "[parseParameter] parameter not VectorParameter! " + parameterName);
            } else alert(msg.err, "[parseParameter] vdouble parameter '" + parameterName + "' not found! at Module '" + module.name() +"'");
            
        } else if ("string" == type) {
            String v = convert(value, String.class);
            if(v.isEmpty()) {
            	v = "\"\"";	// Empty string from the point of view of the GUI.
            }
            
            module.updateParameter(parameterName,type,v);
            module.findParameter(parameterName).setTracked(tracked);
        } else if ("vstring" == type) {
            
            confdb.data.Parameter param = module.findParameter(parameterName);
            if(param != null) {
            	if(param instanceof VectorParameter) {
            		VectorParameter param_sp = (VectorParameter) param;
            		String clean_value = cleanBrackets(value.toString());
            		param_sp.setValue(clean_value);
            		
                    module.updateParameter(parameterName,type,clean_value);
                    module.findParameter(parameterName).setTracked(tracked);
            	} else alert(msg.err, "[parseParameter] parameter not VectorParameter! " + parameterName);
            } else alert(msg.err, "[parseParameter] vstring parameter '" + parameterName + "' not found! at Module '" + module.name() +"'");
            
        } else if ("PSet" == type) {
            //TODO: PARSING PSET
        		PSetParameter pset = parsePSetParameter(parameterObject, parameterName);
        		if(ParameterToBeUpdated instanceof PSetParameter) {
       	    		PSetParameter psetToUpdate = (PSetParameter) ParameterToBeUpdated;
       	    		
       	    		// If PSet contains already a structure, check names.
       	    		// If a sub-parameter exist, update it, Otherwise add it:

       	    		for(int i = 0; i < pset.parameterCount(); i++) {
       	    			// Check the content.
       	    			if(pset.parameter(i).name() == "") {
       	    				// Insert: No named parameter.
       	    				// NOTE: If the template structure contains no named parameters, they could be duplicated.
       	    				psetToUpdate.addParameter(pset.parameter(i));
       	    			} else {
       	    				Parameter param = psetToUpdate.parameter(pset.parameter(i).name());
           	    			if(param != null){
           	    				// If exist: update
           	    				psetToUpdate.parameter(pset.parameter(i).name()).setValue(pset.parameter(i).valueAsString());
           	    			} else {
           	    				// If it doesnt exist: add.
           	    				psetToUpdate.addParameter(pset.parameter(i));

           	    			}
       	    			}
       	    			

       	    		}     	    			
        		} else alert(msg.err, "[parseParameter] PSetParamter expected for name " + parameterName + " for the module " + module.name());
        		
        		module.findParameter(parameterName).setTracked(tracked);
            
        } else if ("VPSet" == type) {
            //TODO: PARSING VPSET
            confdb.data.Parameter param = module.findParameter(parameterName);
            if(param != null) {
            	if(param instanceof VPSetParameter) {
            	
            		VPSetParameter VPSet = (VPSetParameter) param;

            		module.updateParameter(parameterName,type,""); // TODO REMOVE THE DEFAULT VALUES ? 
                    module.findParameter(parameterName).setTracked(tracked);

                	PyList parameterList= (PyList) value;
                	for(int i=0; i < parameterList.size(); i++) {
                	    PSetParameter Pset = parsePSetParameter((PyObject)parameterList.get(i), ""); //TODO name "" ?
                	    VPSet.addParameterSet(Pset);                	    
                	}
                	
            	} else  alert(msg.err, "[parseParameter] "+parameterName +" of VPSETPARAMETER expected! found " + param.getClass().toString());
            } else alert(msg.err, "[parseParameter] VPSet parameter '" + parameterName + "' not found! at Module '" + module.name() +"'");
        } else if ("ESInputTag" == type) {
            module.updateParameter(parameterName,type,value.toString());
            module.findParameter(parameterName).setTracked(tracked);
            
        } else if ("InputTag" == type) {
            module.updateParameter(parameterName,type,value.toString());
            module.findParameter(parameterName).setTracked(tracked);
            
        } else if ("VInputTag" == type) {
            confdb.data.Parameter param = module.findParameter(parameterName);
            if(param != null) {
            	if(param instanceof VectorParameter) {
            		VectorParameter param_sp = (VectorParameter) param;
            		String clean_value = cleanBrackets(value.toString());
            		param_sp.setValue(clean_value);
            		
                    module.updateParameter(parameterName,type,clean_value);
                    module.findParameter(parameterName).setTracked(tracked);
            	} else alert(msg.err, "[parseParameter] parameter not VectorParameter! " + parameterName);
            } else alert(msg.err, "[parseParameter] VInputTag parameter '" + parameterName + "' not found! at Module '" + module.name() +"'");
        } else {
        	alert(msg.war, "[parseParameter] TYPE: [unsupported] " + type);
        }
        
    }
    
    // 
    private String fixLongNumber(String string) {
    	String LHexStringValue = string;
    	if(LHexStringValue.contains("L")) LHexStringValue = LHexStringValue.substring(0, LHexStringValue.indexOf("L"));
    	
    	return LHexStringValue;
    }
    
    // Basically the same as parseParameter, but only for OutputModules.
    // An OutputModule only has two parameters:
    //      * Vstring outputCommands: definition of an EventContent.
    // 		* PSet SelectEvents: list of Paths associated to a stream.
    private void parseOutputModuleParameter(String parameterName, PyObject parameterObject, OutputModule module) {
        String      type    = parameterObject.getType().getName();
        Boolean     tracked = convert(parameterObject.invoke("isTracked"), Boolean.class);
        PyObject    value   = parameterObject.invoke("value");

        if ("vstring" == type) {
            confdb.data.Parameter param = module.findParameter(parameterName);
            if(param != null) {
            	
            	Stream ps = module.parentStream();
            	EventContent ec = ps.parentContent();
            	
            	if(parameterName.compareTo("outputCommands") == 0) {
                	PyList parameterList= (PyList) value;
                	for(int i=0; i < parameterList.size(); i++) {
                		// update eventContent:
                		OutputCommand outputCommand = new OutputCommand();
                		outputCommand.initializeFromString(parameterList.get(i).toString());
                		ec.insertCommand(outputCommand);
                	}

            	} else alert(msg.err, "[parseOutputModuleParameter] outputCommands not found! " + parameterName);
            } else alert(msg.err, "[parseOutputModuleParameter] parameter not found! " + parameterName);
            
        } else if ("PSet" == type) {
            PyDictionary parameterContainer = (PyDictionary) parameterObject.invoke("parameters_");
			for (Object pObject : parameterContainer.entrySet()) {
				PyDictionary.Entry<String, PyObject> entry = (PyDictionary.Entry<String, PyObject>) pObject;
				
				Stream stream = module.parentStream();
				
				if(entry.getKey().compareTo("SelectEvents") == 0) {
					// get Streams definition from here.
					//NOTE: The object in python can be split in several parts but we can iterate all over them. 
					// SelectEvent = (cms.vstring(..) + cms.vstring(..))
					PyObject val = entry.getValue();
					for(int path = 0; path < val.__len__(); path++) {
						Path pathObj = configuration.path(val.__getitem__(path).toString());
						stream.insertPath(pathObj);
					}
				} else alert(msg.err, "[parseOutputModuleParameter] SelectEvents not found! " + parameterName);
			}
        } else {
        	alert(msg.war, "[parseParameter] TYPE "+ type +" unsupported");
        }
        
    }
    
    
    // Sometimes, a simple value comes with a braket at the end that needs to be removed.
    private String cleanBrackets(String vectorValues) {
    	return  vectorValues.replace("[", "").replace("]", "");
    }
    
    
    
    private void parseSequenceMap(PyDictionary pydict) {
    	alert(msg.inf, " Sequences (" + pydict.size() + ")");
    	PyList keys = (PyList) pydict.invoke("keys");
    	
        for (Object key : keys) {
        	String sequenceName = (String) key;
        	
        	confdb.data.Sequence seq = configuration.sequence(sequenceName);
        	if(seq == null) {
        		
        		PyObject value = pydict.__getitem__(new PyString((String) key)); // Get the Sequence Python Object.
        		parseSequence(value);	// Parse it and insert the sequence. Recursively
        		
        	} //else System.out.println("Seq: " + sequenceName + " already exist! "); 
        }
    	
    }
    
    
    private confdb.data.Sequence parseSequence(PyObject object) {
    	String type  = getType(object);
        String label = getLabel(object);
        //System.out.println("[parseSequence] " + type + " " + label);
        confdb.data.Sequence insertedSeq = null;
        
        
        if(confdbTypes.sequence.is(type)) {
        	insertedSeq = configuration.sequence(label);
        	
        	if(insertedSeq == null) {
	        	// Now we can insert the resulting sequence (with subitems already on it).
	    		int seqIndex = configuration.sequenceCount();
	    		insertedSeq = configuration.insertSequence(seqIndex, label);
        	}
        	
    		// Content:
        	PyObject sequenceContent = object.__getattr__(new PyString("_seq"));
        	parseReferenceContainerContent(sequenceContent, insertedSeq);
        	
        } else alert(msg.err, "[parseSequence] expected Sequence, got type " + type);
        
        return insertedSeq;
    }
    
    // Parse sequence/path content:
    private void parseReferenceContainerContent(PyObject sequenceContent, confdb.data.ReferenceContainer parentContainer) {
    	String type  = getType(sequenceContent);
        String label = getLabel(sequenceContent);	// subseq.
        
        if(pythonObjects.sequence.is(type)) {
        	// Parse sub-items of label sequence on demand:
        	confdb.data.Sequence subSequence = parseSequenceOnDemand(label);
        	
        	Reference seqRef = parentContainer.entry(label);
        	
        	// If sequence reference does not exist in this container, add it!
        	if(seqRef == null) configuration.insertSequenceReference(parentContainer,parentContainer.entryCount(),subSequence); // Missing operator.

        	
        } else if(	pythonObjects.seqOpFollows.is(type)	||
        			pythonObjects.seqOpAids.is(type)) 	{
        	// Contain list of sequences?
        	PyObject leftContent  = sequenceContent.__getattr__(new PyString("_left"));
        	PyObject rightContent = sequenceContent.__getattr__(new PyString("_right"));
        	
        	parseReferenceContainerContent(leftContent, parentContainer);
        	parseReferenceContainerContent(rightContent, parentContainer);
        	
        } else if(	pythonObjects.seqIgnore.is(type)	||
        			pythonObjects.seqNegation.is(type)	){
        	
        	
        	PyObject operandContent = sequenceContent.__getattr__(new PyString("_operand"));

        	String Subtype  = getType(operandContent);
            label = getLabel(operandContent);	// subseq.
        	// Only modules can be negated or ignored:
        	ModuleInstance module = configuration.module(label);
        	
        	if(module == null)
        		alert(msg.err, "[parseSequenceImpl] module not found! label = " + label + ", type  = " + Subtype);
        	
        	Reference moduleRef = parentContainer.entry(module.name());
        	
        	if(moduleRef == null)
        		moduleRef = configuration.insertModuleReference(parentContainer, parentContainer.entryCount(), module);
        	
        	if(pythonObjects.seqNegation.is(type))	moduleRef.setOperator(Operator.NEGATE);
        	if(pythonObjects.seqIgnore.is(type)) 	moduleRef.setOperator(Operator.IGNORE);
        		
        	
        	// TODO: Must negate the generated sequence. How?
        } else if(	pythonObjects.EDProducer.is(type)	||
        			pythonObjects.EDFilter.is(type)		||
        			pythonObjects.EDAnalyzer.is(type)) {
        	
        	
        	ModuleInstance module = configuration.module(label);
        	
        	if(module == null) {
        		alert(msg.err, "[parseSequenceImpl] module not found! label = " + label + ", type  = " + type);
        	}
        	
        	Reference moduleRef = parentContainer.entry(module.name());
        	
        	if(moduleRef == null)
        		moduleRef = configuration.insertModuleReference(parentContainer, parentContainer.entryCount(), module);
        	// TODO: How to set the operator of the module reference?
        	//config.insertModuleReference(sequence,index,entry).setOperator( operator );
        } else if(pythonObjects.OutputModule.is(type)) {
        	OutputModule module = configuration.output(label);
        	
    	    if (module !=null) {
    	    	Reference moduleRef = configuration.insertOutputModuleReference(parentContainer,parentContainer.entryCount(), module);
            	if(moduleRef == null)
            		alert(msg.err, " No reference to OutputModule " + label + " has been added to "+parentContainer.name());
    	    } else alert(msg.err, " No OutputModule " + label + " has been found!");
        	
        }
        //else System.out.println("type = " + type);
        
    }
    
    // Return the recently inserted sequence to be linked to the parent seq/container.
    private confdb.data.Sequence parseSequenceOnDemand(String label) {
    	PyDictionary sequences = (PyDictionary) process.__getattr__("_Process__sequences");
    	PyObject sequence = sequences.__getitem__(new PyString(label));
    	
    	return parseSequence(sequence);
    }

    private boolean validPyObject(PyObject object) {
        String type  = getType(object);
        String label = getLabel(object);
        boolean validObject = true;
    	if((type == "NoneType")||(label== "null")) {
    		validObject = false;
    		
    		alert(msg.err, "[validPyObject] Object of type = " + type + ", label = " + label + " does not exist in this Config.");
    	}
    	
    	return validObject;
    }
    
    private static String getType(PyObject object) {
    	return object.getType().getName();
    }

    private static String getLabel(PyObject object) {
    	
    	PyStringMap dict = (PyStringMap) object.getDict();
    	
    	if(dict == null) return null;	// patched to don't brake
    	
    	if (dict.has_key("_Labelable__label"))
    		return convert(dict.__getitem__("_Labelable__label"), String.class);
    	else if (dict.has_key("label") && dict.__getitem__("label").isCallable())
            return convert(object.invoke("label"), String.class);
        else
        	return null;
    }
    
    
    /** close the problem stream, if it is open */
    public boolean closeProblemStream()
    {
		if (problemStream==null) return false;
		problemStream.flush();
		problemStream.close();
		return true;
    }

    //
    // private member functions
    //
    
    /** record a missing/mismatched parameter */
    private void addProblem(String type,String name,String subsys,String pkg,
			    confdb.data.Parameter p)
    {
	if (problemParameters.contains(name + "." + p.name())) return;
	   
	if (problemStream==null) {
	    try {
		problemStream = new PrintWriter(new FileWriter("problems.txt"));
		problemBuffer = new StringBuffer();
	    }
	    catch (IOException e) {
		System.err.println("Can't open problems.txt: "+e.getMessage());
		return;
	    }
	}
	
	if (!problemModules.contains(name)) {
	    problemModules.add(name);
	    String s = (type.startsWith("Module:")) ?
		"Module " + type.substring(7) : type;
	    problemStream.println("\n"+s+" "+name+" "+subsys+" "+pkg);
	    problemBuffer.append("\n"+s+" "+name+" "+subsys+" "+pkg+"\n");
	}
	
	String trkd = (p.isTracked()) ? "tracked" : "untracked";
	String pline = name + ".Parameter " + p.name() + " " + p.type() + " ";
	if (p instanceof PSetParameter|| p instanceof VPSetParameter) {
	    pline += trkd;
	}
	else if (p instanceof VectorParameter) {
	    pline += "{ " + p.valueAsString() + " } " + trkd;
	}
	else {
	    pline += p.valueAsString() + " " + trkd;
	}
	problemStream.println(pline);
	problemBuffer.append(pline+"\n");
	problemParameters.add(name + "." + p.name());
    }

    //
    // main (testing)
    //

    /** main method */

    public static void main(String[] args)
    {


    }

    
    // ENUM types for support.
    
    public enum confdbTypes {
  	  sequence("Sequence"),
  	  path("Path"),
  	  endPath("EndPath"),
  	  module("Module");

  	  private String text;

  	  confdbTypes(String text) {
  	    this.text = text;
  	  }

  	  public String getText() {
  	    return this.text;
  	  }

  	  public static confdbTypes fromString(String text) {
  	    if (text != null) {
  	      for (confdbTypes b : confdbTypes.values()) {
  	        if (text.equalsIgnoreCase(b.text)) {
  	          return b;
  	        }
  	      }
  	    }
  	    return null;
  	  }
  	  
      public boolean is(String OutText) {
      	if(this.text.compareTo(OutText) == 0) return true;
      	return false;
      }
      
  	}
    
    public enum confdbDataTypes {
    	bool("bool"),
    	vbool("vbool"),
    	uint32("uint32"),
    	int32("int32"),
    	uint64("uint64"),
    	int64("int64"),
    	vuint32("vuint32"),
    	vint32("vint32"),
    	vuint64("vuint64"),
    	vint64("vint64"),
    	Double("double"),
    	vdouble("vdouble"),
    	string("string"),
    	vstring("vstring"),
    	PSet("PSet"),
    	ESInputTag("ESInputTag"),
    	InputTag("InputTag"),
    	VInputTag("VInputTag");

    	  private String text;

    	  confdbDataTypes(String text) {
    	    this.text = text;
    	  }

    	  public String getText() {
    	    return this.text;
    	  }

    	  public static confdbDataTypes fromString(String text) {
    	    if (text != null) {
    	      for (confdbDataTypes b : confdbDataTypes.values()) {
    	        if (text.equalsIgnoreCase(b.text)) {
    	          return b;
    	        }
    	      }
    	    }
    	    return null;
    	  }
    	  
        public boolean is(String OutText) {
        	if(this.text.compareTo(OutText) == 0) return true;
        	return false;
        }
        
    	}
    
    public enum pythonObjects {
    	  sequence("Sequence"),
    	  seqOpFollows("_SequenceOpFollows"),
    	  seqNegation("_SequenceNegation"),
    	  seqIgnore("_SequenceIgnore"),
    	  seqOpAids("_SequenceOpAids"), 
    	  EDProducer("EDProducer"),
    	  EDFilter("EDFilter"),
    	  EDAnalyzer("EDAnalyzer"),
    	  OutputModule("OutputModule");

    	  private String text;

    	  pythonObjects(String text) {
    	    this.text = text;
    	  }

    	  public String getText() {
    	    return this.text;
    	  }

    	  public static pythonObjects fromString(String text) {
    	    if (text != null) {
    	      for (pythonObjects b : pythonObjects.values()) {
    	        if (text.equalsIgnoreCase(b.text)) {
    	          return b;
    	        }
    	      }
    	    }
    	    return null;
    	  }
    	  
        public boolean is(String OutText) {
        	if(this.text.compareTo(OutText) == 0) return true;
        	return false;
        }
    }
    

    
    
	   public void executePyLineByLine(String file) {
		      File archivo = null;
		      FileReader fr = null;
		      BufferedReader br = null;

		      try {
		         archivo = new File (file);
		         fr = new FileReader (archivo);
		         br = new BufferedReader(fr);

		         // Lectura del fichero
		         String linea;
		         while((linea=br.readLine())!=null) {
		         //for(int i = 0; i < 20; i++) {
		        //	 linea = br.readLine();
		        	 //System.out.println("\nprocesing line "+i+": "+linea);
		        	 
		        	 
		        	 checkValidInstruction(linea); 
		         }
		         	
		      }
		      catch(Exception e){
		         e.printStackTrace();
		      }finally{
		         // En el finally cerramos el fichero, para asegurarnos
		         // que se cierra tanto si todo va bien como si salta 
		         // una excepcion.
		         try{                    
		            if( null != fr ){   
		               fr.close();     
		            }                  
		         }catch (Exception e2){ 
		            e2.printStackTrace();
		         }
		      }
		   }
	   
	   public void checkValidInstruction(String pyInstrc) {
		   command+= pyInstrc;
		   
		   int leftBCount = countOccurrences(command, "[^(]");
		   int rightBCount = countOccurrences(command, "[^)]");

		   // ChangeFormat?
		   
		   
		   
		   if((leftBCount>0)&&(leftBCount == rightBCount)) {
			   // Command is well formed!
			   if(ignorePrescaleService) {
				   if(command.contains("PrescaleService")) 
					   command = ""; // Ignore prescale service instruction.
			   }				   
			   
			   if(command != "")
				   pyCmd(command);

			   command = "";
		   }
	   }
	   
	   public int countOccurrences(String line, String regex) {
		   int count = line.length() - line.replaceAll(regex, "").length();
		   return count;
	   }

}



