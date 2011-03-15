package confdb.data;


import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.StringTokenizer;

/**
 * SmartPrescaleTable
 * -------------
 * @author Vasundhara Chetluru
 *
 */
public class SmartPrescaleTable
{
    //
    // member data
    //
    

    /** prescale table rows */
    private ArrayList<SmartPrescaleTableRow> rows=new ArrayList<SmartPrescaleTableRow>();
    private ArrayList<Stream> streams=new ArrayList<Stream>();
    public ModuleInstance module;
    private IConfiguration config;
    
    //
    // construction
    //
    
    /** standard constructor */
    public SmartPrescaleTable(IConfiguration config, ModuleInstance module)
    {
	this.config = config;
	this.module = module;
	initialize();
    }

    


    //
    // member functions
    //
    
     
    /** number of prescaled paths */
    public int prescaleConditionCount() { return rows.size(); }

    /** get i-th trigger condition */
    public String  prescaleCondition(int i)
    {
	return rows.get(i).triggerCondition;
    }

    
    /** check if the i-th path is prescaled at all */
    public boolean isSmartPrescaled(int i)
    {
	//need to implement the code
	return true;
    }

    /** check if the i-th path is prescaled at all */
    public boolean addRow(int i,String strCondition)
    {
	//need to implement the code
	if(prescaleConditionCount()==1){
	    if(prescaleCondition(i).equals("")){
		rows.remove(i); 
		module.setHasChanged();
	    }
	}
	rows.add(i,new SmartPrescaleTableRow(strCondition)); 
	module.setHasChanged();
	return true;
    }

    public boolean modRow(int i,String strCondition)
    {
	rows.set(i,new SmartPrescaleTableRow(strCondition));
	module.setHasChanged();
	return true;
    }

    /**check path in the streams **/
    public Path checkPathExists(String strPath){
	if(streams.size()>0){
	    for(int j=0;j<streams.size();j++){
		Path path =streams.get(j).path(strPath);
		if(path!=null)
		    return path;
	    }
	    return null;
	}else{
	    Path path = config.path(strPath);
	    if ((path==null)||(path.isSetAsEndPath())) return null;
	    return config.path(strPath);
	}
    }
    
    /**provide list of associated streams */
    public ArrayList<Stream> associatedStreams() {
	return streams;
    }

    /** check if the i-th path is prescaled at all */
    public boolean removeRow(int i)
    {
	rows.remove(i); 
	if(prescaleConditionCount()==0){
	    rows.add(new SmartPrescaleTableRow("")); 
	}
	module.setHasChanged();
	return true;
    }


    //
    // private member functions
    //
    
    /** initialize the prescale table from a given configuration */
    private void initialize()
    {
	
	Path[] paths = module.parentPaths();
	
	for (Path p : paths){
	    if(p.isEndPath()&&p.hasOutputModule()){		
		Iterator<OutputModule> outputIterator = p.outputIterator();
		while(outputIterator.hasNext()){
		    OutputModule outputModule = outputIterator.next(); 
		    Stream stream = outputModule.parentStream();
		    streams.add(stream);
		}
	    }
	}
	update();
    }

    public void update(){

	rows.clear();
      
	VStringParameter parameterTriggerConditions =  (VStringParameter)module.parameter("triggerConditions");
	
	if (parameterTriggerConditions==null) {
	    rows.add(new SmartPrescaleTableRow("")); 
	    return;
	}

	HashMap<String,SmartPrescaleTableRow> pathToRow =
	    new HashMap<String,SmartPrescaleTableRow>();

	for(int i=0;i<parameterTriggerConditions.vectorSize();i++){
	    String trgCondition = (String)parameterTriggerConditions.value(i);
	    String strCondition = SmartPrescaleTable.regularise(trgCondition);

	    // replace unknown paths by FALSE
	    StringTokenizer pathTokens = new StringTokenizer(strCondition,"/ ");
	    while ( pathTokens.hasMoreTokens()) {
		String strPath = pathTokens.nextToken().trim();
		if (strPath.length()<5) continue;
		int g = -10000;
		try { 
		    g = Integer.parseInt(strPath); 
		}catch (NumberFormatException e) { 
		    g = -10000;
		}
		if ( (g<0)
		     && (!strPath.equals("FALSE"))
		     && (!strPath.substring(0,2).equals("L1"))
		     && (checkPathExists(strPath)==null) ) {
		    strCondition = strCondition.replaceAll(strPath,"FALSE");
		}
	    }

	    // replace conditions containing only FALSE by empty conditions
	    strCondition = SmartPrescaleTable.simplify(strCondition);

	    if (!strCondition.equals(trgCondition)) {
		module.setHasChanged();
		parameterTriggerConditions.setValue(i,strCondition);
	    }
	    if (!strCondition.equals("")) {
		SmartPrescaleTableRow row 
		    = new SmartPrescaleTableRow(strCondition);
		if (row.simple() && (checkPathExists(row.pathName) != null)) {
		    pathToRow.put(row.pathName,row);
		} else {
		    rows.add(new SmartPrescaleTableRow(strCondition));   
		}
	    }
	}
	// remove empty conditions
	if (module.squeeze()) module.setHasChanged();	

	Iterator<Path> itP = config.pathIterator();
	while (itP.hasNext()) {
	    Path path = itP.next();
	    SmartPrescaleTableRow row = pathToRow.remove(path.name());
	    if (row==null) {
		if (checkPathExists(path.name()) != null) {
		    rows.add(new SmartPrescaleTableRow(path.name(), new Long(0)));
		}
	    } else {
		rows.add(row);
	    }
	}

    }

    public static String regularise(String input) {
	String work = new String (input);
	work = work.replaceAll("/"," / ");
	work = work.replaceAll("\\("," \\( "); // regex escape
        work = work.replaceAll("\\)"," \\) "); // regex escape
	while (work.indexOf("  ")>=0) { work=work.replaceAll("  "," "); }
	work = " "+work+" ";
	work = work.replaceAll(" / 1 "," ");   // no /1!
	work = work.trim();
	return work;
    }

    public static String simplify(String input) {
	String work = new String(input);
	work=regularise(work);

	/* replace conditions containing only FALSE by empty conditions */
	StringTokenizer moreTokens = new StringTokenizer(work,"/ ");
	int n=0;
	while ( moreTokens.hasMoreTokens()) {
	    String strPath = moreTokens.nextToken().trim();
	    if (strPath.length()<5) continue;
	    int g = -10000;
	    try { 
		g = Integer.parseInt(strPath); 
	    }catch (NumberFormatException e) { 
		g = -10000;
	    }
	    if ( (g<0) && (!strPath.equals("FALSE"))) n++;
	}
	if (n==0) work="";
	return work;
    }

    public boolean simple(int row) {
	return rows.get(row).simple();
    }

    public Long prescale(int row) {
	return rows.get(row).prescale;
    }
}


//
// class to hold the data for one smart prescale table row
//
class SmartPrescaleTableRow
{
    public String triggerCondition;
    public String pathName;
    public Long   prescale;

    public SmartPrescaleTableRow(String triggerCondition)
    {
	this.pathName = null;
	this.prescale = null;
	this.triggerCondition = SmartPrescaleTable.regularise(triggerCondition);
	//
	if (this.triggerCondition.indexOf(" ")==-1) {
	    this.pathName = this.triggerCondition;
	    this.prescale = new Long(1);
	} else if (this.triggerCondition.indexOf(" / ")==-1) {
	    this.pathName = null;
	    this.prescale = null;
	} else {
	    int index   = this.triggerCondition.indexOf(" / ");
	    String path = SmartPrescaleTable.regularise(this.triggerCondition.substring(0,index));
	    String pres = SmartPrescaleTable.regularise(this.triggerCondition.substring(index+3));
	    if ( (path.indexOf(" ")>=0) || (pres.indexOf(" ")>=0) ) {
		this.pathName = null;
		this.prescale = null;
	    } else {
		try {
		    this.pathName = path;
		    this.prescale = Long.parseLong(pres);
		}catch (NumberFormatException e) {
		    this.pathName = null;
		    this.prescale = null;
		}
	    }
	}
    }

    public SmartPrescaleTableRow(String pathName, Long prescale)
    {
	this.pathName = SmartPrescaleTable.regularise(pathName);
	this.prescale = prescale;
	if (prescale==1) {
	    this.triggerCondition = pathName;
	} else {
	    this.triggerCondition = pathName+" / "+prescale;
	}	    
    }

    public boolean simple()
    {
	return ((pathName!=null) && (prescale!=null));
    }

}
