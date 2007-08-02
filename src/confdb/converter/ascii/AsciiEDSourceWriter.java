package confdb.converter.ascii;

import confdb.converter.Converter;
import confdb.converter.IEDSourceWriter;
import confdb.data.EDSourceInstance;


public class AsciiEDSourceWriter extends AsciiInstanceWriter implements IEDSourceWriter 
{
	
	public String toString( EDSourceInstance edsource, Converter converter ) 
	{
		if ( edsource == null )
			return "";
		return toString( "source", edsource, converter );
	}

}