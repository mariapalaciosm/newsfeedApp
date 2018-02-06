package assigment.application.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("enclosure")
public class Enclosure {
	@XStreamAlias("url")
   	@XStreamAsAttribute
    public String url;
    public String type;
    
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
}
