package assigment.application;

import java.net.MalformedURLException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.UrlResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.oxm.xstream.XStreamMarshaller;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import assigment.application.model.Enclosure;
import assigment.application.model.News;


@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
	
	@Autowired
	public JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	public StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	public DataSource dataSource;
	
	@Autowired
	JobLauncher jobLauncher;
	
	public JdbcTemplate jdbcTemplate;

	public Timestamp lastStoragedPublicationTime = null;
	
	@Bean
	public DataSource dataSource() {
		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://localhost/newsfeeddb?useSSL=false");
		dataSource.setUsername("root");
		dataSource.setPassword("root");
		return dataSource;
	}
	
	
	public StaxEventItemReader<News> reader()  {
		StaxEventItemReader<News> reader = new StaxEventItemReader<News>();
		try {
			reader.setResource(new UrlResource("http://feeds.nos.nl/nosjournaal?format=xml"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//element item in the xml is a element news
		reader.setFragmentRootElementName("item");
		Map<String, Class> aliases = new HashMap<String, Class>();
		aliases.put("item", News.class);
		aliases.put("enclouse", Enclosure.class);
		
		Map<String, Class> attribute = new HashMap<String, Class>();
		attribute.put("url", String.class);
		
		XStreamMarshaller xStreamMarshaller = new XStreamMarshaller();
		xStreamMarshaller.setAliasesByType(aliases);
		xStreamMarshaller.setUseAttributeFor(attribute);
		reader.setUnmarshaller(xStreamMarshaller);
		
		lastStoragedPublicationTime = getLastDatePublish();

		return reader;
	}
	
	// last storaged publication date in the database
	public Timestamp getLastDatePublish() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "select max(pubDate) from news";
		Timestamp t = jdbcTemplate.queryForObject(query, Timestamp.class);		
		return t;
	}
	
	
	public JdbcBatchItemWriter<News> writer(){
		JdbcBatchItemWriter<News> writer = new JdbcBatchItemWriter<News>();
		writer.setDataSource(dataSource);
		writer.setSql("insert into news (title, description, image, pubDate) values (?,?,?,?)");// luego
		writer.setItemPreparedStatementSetter(new NewsPreparedStmSetter());
		return writer;
	}
	
	public ItemProcessor<News, News> processor() {
	    return new ItemProcessor<News, News>() {
	        @Override
	        public News process(News item) throws Exception {
	        //take items that were published after the storaged last publication in the database
	        java.sql.Timestamp timestamp = convertStringToTimestamp(item.getPubDate());
	  		  if(lastStoragedPublicationTime == null || timestamp.after(lastStoragedPublicationTime)) {
				  return item;
			  }
			  else {
				  return null;
			  }
	        }
	    };
	}

	
	
	private class NewsPreparedStmSetter implements ItemPreparedStatementSetter<News>{
			
			@Override
			public void setValues(News news, PreparedStatement ps) throws SQLException {
				java.sql.Timestamp timestamp = convertStringToTimestamp(news.getPubDate());
					ps.setString(1, news.getTitle());
					ps.setString(2, news.getDescription());
					ps.setString(3, news.getUrlEnclosure());
					SimpleDateFormat format =  new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
	                java.util.Date date = null;
					try {
						date = format.parse((news.getPubDate()));
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Calendar cal = Calendar.getInstance();
					ps.setTimestamp(4, timestamp, cal);
				}
			
			
		}
	
	public Timestamp convertStringToTimestamp (String string) {
		SimpleDateFormat format =  new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
		Date date = null;
		try {
			date = new java.sql.Date(format.parse(string).getTime());

		} catch (ParseException e) {
			e.printStackTrace();
		}
		java.sql.Timestamp timestamp = new java.sql.Timestamp(date.getTime());
		return timestamp;
	}


	public Step step1()  { 
	return stepBuilderFactory.get("step1")
			.<News,News> chunk(10)
			.reader(reader())
			.processor(processor())
			.writer(writer())
			.build();
	}

	
	public Job importNewsJob() throws Exception {
	return jobBuilderFactory.get("importNewsJob")
				.incrementer(new RunIdIncrementer())
				.flow(step1())
				.end()
				.build();
	}
	
	
	@Scheduled(cron = "0 0/5 * * * ?")
	public void run() throws Exception {
    	Map<String, JobParameter> confMap = new HashMap<>();
        confMap.put("time", new JobParameter(System.currentTimeMillis()));
        JobParameters jobParameters = new JobParameters(confMap);
        JobExecution jobExecution = jobLauncher.run(importNewsJob(), jobParameters);

	}
	
	
	
	
	
}
