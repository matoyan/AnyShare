package mobisocial.bento.anyshare.io;

public class Postdata {
	public String title;
	public String text;
	public String datatype; // text, link or stream
	public String mimetype;
	public String uri;
	public String localUri; // for file
	public String timestamp;
	public String comment;
	public int filesize;
	public String key;      // preshared key to decrypt data

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
