package confdb.data;

/**
 * ESInputTagParameter
 * ---------------
 * @author 
 *
 * parameter base class for scalar parameters of type ESInputTag.
 */
public class ESInputTagParameter extends ScalarParameter
{
    //
    // member data
    //
    
    /** parameter type string */
    private static final String type = "ESInputTag";
    
    /** first value: label */
    private String label = null;
    
    /** second value: instance */
    private String instance = null;
    
    /** third value: process */
    private String process = null;
    
    
    //
    // construction
    //

    /** standard constructor */
    public ESInputTagParameter(String name,
			     String label,String instance,String process,
			     boolean isTracked)
    {
	super(name,isTracked);
	isValueSet = (label!=null&&instance!=null&&process!=null);
	if (isValueSet) {
	    this.label    = new String(label);
	    this.instance = new String(instance);
	    this.process  = new String(process);
	}
    }
    
    /** constructor from string */
    public ESInputTagParameter(String name,String valueAsString,
			     boolean isTracked)
    {
	super(name,isTracked);
	setValue(valueAsString);
    }
    
    //
    // member functions
    //
    
    /** make a clone of the parameter */
    public Parameter clone(Object parent)
    {
	ESInputTagParameter result =
	    new ESInputTagParameter(name,label,instance,process,isTracked);
	result.setParent(parent);
	return result;
    }
    
    /** type of the parameter as a string */
    public String type() { return type; }
    
    /** retrieve the value of the parameter */
    public Object value()
    {
	return (isValueSet) ? valueAsString() : null;
    }
    
    /** get label */
    public String label() { return label; }

    /** get instance */
    public String instance() { return instance; }

    /** get process */
    public String process() { return process; }
    
    /** retrieve the value of the parameter as a string */
    public String valueAsString()
    {
	if (isValueSet) {
	    String result = label;
	    if (instance.length()>0||process.length()>0)
		result += ":" + instance;
	    if (process.length()>0)  result += ":" + process;
	    if (result.equals(new String())) result = "\"\"";
	    return result;
	}
	return new String();
    }

    /** set the value  the parameter */
    public boolean setValue(String valueAsString)
    {
	if (valueAsString==null||valueAsString.length()==0) {
	    isValueSet = false;
	    label      = null;
	    instance   = null;
	    process    = null;
	}
	else {
	    if ((valueAsString.startsWith("'") &&valueAsString.endsWith("'"))||
		(valueAsString.startsWith("\"")&&valueAsString.endsWith("\"")))
		valueAsString=valueAsString.substring(1,valueAsString.length()-1);
	    
	    String[] strValues = valueAsString.split(":");
	    if (strValues.length==0||strValues.length>3) return false;
	    label = strValues[0];
	    if (strValues.length>1) instance = strValues[1];
	    else instance = "";
	    if (strValues.length>2) process = strValues[2];
	    else process = "";
	    isValueSet = true;
	}
	return true;
    }

    /** set label */
    public void setLabel(String label) { this.label = label; }
    
    /** set instance */
    public void setInstance(String instance) { this.instance = instance; }

    /** set process */
    public void setProcess(String process) { this.process = process; }

}
