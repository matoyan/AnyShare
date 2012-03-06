package mobisocial.bento.anyshare.io;

public class Postdata {
	public String title;
	public String text;
	public String datatype;
	public String mimetype;
	public String uri;
	public String localUri; // for file
	public String timestamp;
	public int filesize;
	public byte[] thumb;  // image thumbnail
	public byte[] attach; // other raw data (file itself)
	public String comment;
	public String objtype;
	
	// for corral initial try
	public String lanip;
//	public String lanport;
	public String wanip;
//	public String wanport;


	
	public static final String TYPE_TEXT_WITH_LINK = "textwlink";
	public static final String TYPE_TEXT = "text";
	public static final String TYPE_STREAM = "stream";
}
