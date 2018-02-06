package assigment.application.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;


@XStreamAlias("item")

public class News {
	private Integer id;   
	private String title;
	private String description;
	private Enclosure enclosure;
	private String pubDate;
	
	@XStreamOmitField
	private String link;
	
	@XStreamOmitField
	private String guid;
	
	@XStreamOmitField
	private String origLink;
	
//	@XStreamOmitField
//	private String feedburnerorigLink;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getPubDate() {
		return pubDate;
	}
	public void setPubDate(String pubDate) {
		this.pubDate = pubDate;
	}
	
	public Enclosure getEnclosure() {
		return enclosure;
	}
	public void setEnclosure(Enclosure enclosure) {
		this.enclosure = enclosure;
	}
	
	public String getUrlEnclosure() {
		
		return enclosure.getUrl();
	}

}
